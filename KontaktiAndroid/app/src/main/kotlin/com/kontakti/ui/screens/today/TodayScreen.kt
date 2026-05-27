package com.kontakti.ui.screens.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.ContactPrompt
import com.kontakti.data.model.RhythmInsight
import com.kontakti.data.model.TodayItem
import com.kontakti.data.model.TodayItemKind
import com.kontakti.data.repository.QuizRepository
import com.kontakti.data.repository.TodayRepository
import com.kontakti.ui.components.AvatarComposable
import com.kontakti.ui.components.DoNotContactBadge
import com.kontakti.ui.components.EmptyState
import com.google.gson.JsonParser
import retrofit2.HttpException
import com.kontakti.widget.TodayWidgetState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repo: TodayRepository,
    private val quizRepo: QuizRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    data class State(
        val loading: Boolean = false,
        val items: List<TodayItem> = emptyList(),
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _quizPrompts = MutableStateFlow<List<ContactPrompt>>(emptyList())
    val quizPrompts: StateFlow<List<ContactPrompt>> = _quizPrompts.asStateFlow()

    private val _rhythmInsights = MutableStateFlow<List<RhythmInsight>>(emptyList())
    val rhythmInsights: StateFlow<List<RhythmInsight>> = _rhythmInsights.asStateFlow()

    private val _quizAnsweredCount = MutableStateFlow(0)
    val quizAnsweredCount: StateFlow<Int> = _quizAnsweredCount.asStateFlow()

    private val _draftFor = MutableStateFlow<TodayItem?>(null)
    val draftFor: StateFlow<TodayItem?> = _draftFor.asStateFlow()

    private val _draftText = MutableStateFlow("")
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    private val _draftLoading = MutableStateFlow(false)
    val draftLoading: StateFlow<Boolean> = _draftLoading.asStateFlow()

    /** One-shot user-visible message (e.g. DNC block from the backend). */
    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    fun consumeSnackbar() { _snackbar.value = null }

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val resp = repo.load()
                _state.value = State(loading = false, items = resp.items)
                _quizPrompts.value = resp.quiz
                _rhythmInsights.value = resp.rhythmInsights
                // Update widget DataStore + trigger re-render.
                runCatching {
                    TodayWidgetState.update(appContext, resp.items, resp.count)
                }
            } catch (e: Exception) {
                _state.value = State(loading = false, error = e.message)
            }
        }
    }

    fun openDraft(item: TodayItem) {
        // Short-circuit: server filters DNC out of /today, but a stale snapshot
        // could still surface one. Don't even ask for a draft.
        if (item.person.doNotContact) {
            _snackbar.value = "This contact is marked do-not-contact" +
                (item.person.doNotContactReason?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty())
            return
        }
        _draftFor.value = item
        _draftText.value = item.suggestedMessage.orEmpty()
        val key = item.key ?: item.id
        viewModelScope.launch {
            _draftLoading.value = true
            try {
                val d = repo.draft(key)
                _draftText.value = d.draft
            } catch (e: HttpException) {
                if (e.code() == 422) {
                    val msg = runCatching {
                        val body = e.response()?.errorBody()?.string().orEmpty()
                        JsonParser.parseString(body).asJsonObject.get("message")?.asString
                    }.getOrNull() ?: "This contact is marked do-not-contact."
                    _snackbar.value = msg
                    _draftFor.value = null
                }
            } catch (_: Exception) {
            } finally {
                _draftLoading.value = false
            }
        }
    }

    fun updateDraft(value: String) { _draftText.value = value }
    fun closeDraft() { _draftFor.value = null }

    fun logSend(via: String) {
        val item = _draftFor.value ?: return
        val key = item.key ?: item.id
        viewModelScope.launch {
            runCatching { repo.log(key, via) }
            _draftFor.value = null
            load()
        }
    }

    fun snooze(item: TodayItem) {
        val key = item.key ?: item.id
        viewModelScope.launch {
            runCatching { repo.snooze(key) }
            load()
        }
    }

    fun skip(item: TodayItem) {
        val key = item.key ?: item.id
        viewModelScope.launch {
            runCatching { repo.skip(key) }
            load()
        }
    }

    // ── Quiz actions ─────────────────────────────────────────────────────────
    fun answerQuiz(promptId: String, answer: String, structured: Map<String, Any?>? = null) {
        // Optimistic remove.
        val current = _quizPrompts.value
        _quizPrompts.value = current.filterNot { it.id == promptId }
        _quizAnsweredCount.value = _quizAnsweredCount.value + 1
        viewModelScope.launch {
            runCatching { quizRepo.answer(promptId, answer, structured) }
                .onFailure { _quizPrompts.value = current }
        }
    }

    fun skipQuiz(promptId: String) {
        val current = _quizPrompts.value
        _quizPrompts.value = current.filterNot { it.id == promptId }
        viewModelScope.launch {
            runCatching { quizRepo.skip(promptId) }
                .onFailure { _quizPrompts.value = current }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenPerson: (String) -> Unit,
    vm: TodayViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val draftFor by vm.draftFor.collectAsState()
    val draftText by vm.draftText.collectAsState()
    val draftLoading by vm.draftLoading.collectAsState()
    val quizPrompts by vm.quizPrompts.collectAsState()
    val quizAnswered by vm.quizAnsweredCount.collectAsState()
    val snackbarMsg by vm.snackbar.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = { vm.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh signals")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading && state.items.isEmpty() && quizPrompts.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.items.isEmpty() && quizPrompts.isEmpty() -> EmptyState(
                    icon = { Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp)) },
                    title = "Nothing for today",
                    subtitle = "You're all caught up. Pull to refresh later."
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    item(key = "quiz-section") {
                        QuizSection(
                            prompts = quizPrompts,
                            answeredCount = quizAnswered,
                            onAnswer = { id, ans, struct -> vm.answerQuiz(id, ans, struct) },
                            onSkip = { vm.skipQuiz(it) }
                        )
                    }
                    items(state.items, key = { it.id }) { item ->
                        TodayItemCard(
                            item = item,
                            onOpen = { onOpenPerson(item.person.id) },
                            onDraft = { vm.openDraft(item) },
                            onSnooze = { vm.snooze(item) },
                            onSkip = { vm.skip(item) }
                        )
                    }
                }
            }
        }
    }

    draftFor?.let { item ->
        DraftMessageSheet(
            item = item,
            text = draftText,
            loading = draftLoading,
            onText = vm::updateDraft,
            onDismiss = vm::closeDraft,
            onSendVia = vm::logSend
        )
    }
}

@Composable
private fun TodayItemCard(
    item: TodayItem,
    onOpen: () -> Unit,
    onDraft: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    var whyExpanded by remember { mutableStateOf(false) }
    val isRhythm = item.kind == TodayItemKind.rhythm_broken
    val tint = if (isRhythm) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surface

    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        color = if (isRhythm) tint.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onOpen() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRhythm) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(tint.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = tint)
                    }
                } else {
                    AvatarComposable(item.person.fullName, size = 44.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.person.fullName, fontWeight = FontWeight.SemiBold)
                        if (item.person.doNotContact) {
                            Spacer(Modifier.width(6.dp))
                            DoNotContactBadge()
                        }
                    }
                    Text(
                        item.reason ?: kindLabel(item.kind),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                KindChip(item.kind)
            }
            if (!item.suggestedMessage.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        item.suggestedMessage,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            // Rhythm "Why?" expandable — defensive: only show if signal has any useful field.
            if (isRhythm && !item.signal.isNullOrEmpty()) {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { whyExpanded = !whyExpanded }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Icon(
                        if (whyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Why?", style = MaterialTheme.typography.labelMedium)
                }
                AnimatedVisibility(visible = whyExpanded) {
                    Column(Modifier.padding(start = 8.dp, bottom = 4.dp)) {
                        item.signal.forEach { (k, v) ->
                            if (v != null) {
                                Text(
                                    "${k.replace('_', ' ')}: $v",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onDraft, enabled = !item.person.doNotContact) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text("Draft")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onSnooze) { Text("Snooze") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onSkip) { Text("Skip") }
            }
            if (item.person.doNotContact) {
                Text(
                    "Do not contact",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun KindChip(kind: TodayItemKind) {
    val (label, color) = when (kind) {
        TodayItemKind.birthday -> "Birthday" to Color(0xFFEC4899)
        TodayItemKind.cadence_overdue -> "Overdue" to Color(0xFFEA580C)
        TodayItemKind.follow_up_due -> "Follow-up" to Color(0xFF2563EB)
        TodayItemKind.job_change -> "Job change" to Color(0xFF7C3AED)
        TodayItemKind.social_signal -> "Signal" to Color(0xFF0EA5E9)
        TodayItemKind.anniversary_met -> "Anniversary" to Color(0xFF16A34A)
        TodayItemKind.rhythm_broken -> "Rhythm" to Color(0xFFF59E0B)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

private fun kindLabel(kind: TodayItemKind): String = when (kind) {
    TodayItemKind.birthday -> "It's their birthday today"
    TodayItemKind.cadence_overdue -> "Overdue for a check-in"
    TodayItemKind.follow_up_due -> "Follow-up is due"
    TodayItemKind.job_change -> "Recently changed jobs"
    TodayItemKind.social_signal -> "New social activity"
    TodayItemKind.anniversary_met -> "Anniversary of meeting"
    TodayItemKind.rhythm_broken -> "Your usual rhythm with them has slipped"
}
