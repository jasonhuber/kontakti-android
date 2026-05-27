package com.kontakti.ui.screens.people

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.*
import com.kontakti.data.repository.*
import com.kontakti.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Merge a Person's structured emails with the legacy single-column `email`,
 *  deduping by lowercased value and sorting primaries first. Mirrors the web's
 *  `mergeEmails` in PersonDetailModal.tsx. */
private fun mergePersonEmails(p: Person): List<PersonEmail> {
    val out = mutableListOf<PersonEmail>()
    val seen = mutableSetOf<String>()
    fun push(value: String, label: String?, primary: Boolean) {
        val v = value.trim()
        if (v.isEmpty()) return
        val key = v.lowercase()
        if (key in seen) return
        seen += key
        out += PersonEmail(value = v, label = label, isPrimary = primary)
    }
    p.emails.forEach { push(it.value, it.label, it.isPrimary) }
    p.email?.let { push(it, null, false) }
    return out.sortedByDescending { it.isPrimary }
}

/** Merge a Person's structured phones with the legacy single-column `phone`,
 *  deduping by digits-only (stripping US country code `1` when 11 digits) and
 *  sorting primaries first. Mirrors the web's `mergePhones`. */
private fun mergePersonPhones(p: Person): List<PersonPhone> {
    val out = mutableListOf<PersonPhone>()
    val seen = mutableSetOf<String>()
    fun norm(s: String): String {
        val digits = s.filter { it.isDigit() }
        return if (digits.length == 11 && digits.startsWith("1")) digits.drop(1) else digits
    }
    fun push(value: String, label: String?, primary: Boolean) {
        val v = value.trim()
        if (v.isEmpty()) return
        val key = norm(v)
        if (key.isEmpty() || key in seen) return
        seen += key
        out += PersonPhone(value = v, label = label, isPrimary = primary)
    }
    p.phones.forEach { push(it.value, it.label, it.isPrimary) }
    p.phone?.let { push(it, null, false) }
    return out.sortedByDescending { it.isPrimary }
}

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val peopleRepo: PeopleRepository,
    private val activityRepo: ActivityRepository,
    private val notesRepo: NotesRepository,
    private val tasksRepo: TasksRepository,
    private val discussionsRepo: DiscussionsRepository,
    private val quizRepo: QuizRepository
) : ViewModel() {
    val personId: String = checkNotNull(saved["personId"])

    data class State(
        val person: Person? = null,
        val activity: List<SocialActivity> = emptyList(),
        val notes: List<Note> = emptyList(),
        val tasks: List<Task> = emptyList(),
        val discussions: List<Discussion> = emptyList(),
        val timeline: List<TimelineEvent> = emptyList(),
        val quizMemories: List<ContactPrompt> = emptyList(),
        val loading: Boolean = false,
        val refreshingActivity: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val p = peopleRepo.getPerson(personId)
                val activity = runCatching { activityRepo.list(personId) }.getOrDefault(emptyList())
                val notes = runCatching { notesRepo.listForPerson(personId) }.getOrDefault(emptyList())
                val tasks = runCatching { tasksRepo.listForPerson(personId) }.getOrDefault(emptyList())
                val discussions = runCatching { discussionsRepo.listForPerson(personId) }.getOrDefault(emptyList())
                val timeline = runCatching { peopleRepo.getTimeline(personId) }.getOrDefault(emptyList())
                val quizMemories = runCatching { quizRepo.historyForPerson(personId) }.getOrDefault(emptyList())
                _state.value = State(p, activity, notes, tasks, discussions, timeline, quizMemories, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun refreshActivity() {
        viewModelScope.launch {
            _state.value = _state.value.copy(refreshingActivity = true)
            runCatching { activityRepo.refresh(personId) }
            val activity = runCatching { activityRepo.list(personId) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(activity = activity, refreshingActivity = false)
        }
    }

    fun acknowledge(activityId: String) {
        viewModelScope.launch {
            runCatching { activityRepo.acknowledge(activityId) }
            val activity = runCatching { activityRepo.list(personId) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(activity = activity)
        }
    }

    fun addNote(title: String?, body: String) {
        viewModelScope.launch {
            runCatching { notesRepo.create(personId, title, body) }
            load()
        }
    }

    fun addTask(title: String, dueAt: String?, priority: String?) {
        viewModelScope.launch {
            runCatching { tasksRepo.create(personId, title, dueAt, priority) }
            load()
        }
    }

    fun completeTask(id: String) {
        viewModelScope.launch {
            runCatching { tasksRepo.complete(id) }
            load()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { peopleRepo.deletePerson(personId) }
            onDone()
        }
    }

    /**
     * Flip the DNC flag immediately. When turning off, also clears the reason
     * so the next toggle-on starts fresh.
     */
    fun setDoNotContact(enabled: Boolean) {
        val current = _state.value.person ?: return
        // Optimistic UI update so the switch / colors flip without waiting on the network.
        _state.value = _state.value.copy(
            person = current.copy(
                doNotContact = enabled,
                doNotContactReason = if (enabled) current.doNotContactReason else null
            )
        )
        viewModelScope.launch {
            val patch = PersonPatch(
                doNotContact = enabled,
                doNotContactReason = if (enabled) current.doNotContactReason else ""
            )
            runCatching { peopleRepo.updatePerson(personId, patch) }
                .onSuccess { updated -> _state.value = _state.value.copy(person = updated) }
        }
    }

    private var reasonJob: Job? = null

    /** Debounced reason update — fires 500ms after the user stops typing. */
    fun setDoNotContactReason(reason: String) {
        val current = _state.value.person ?: return
        _state.value = _state.value.copy(person = current.copy(doNotContactReason = reason))
        reasonJob?.cancel()
        reasonJob = viewModelScope.launch {
            delay(500)
            val patch = PersonPatch(doNotContactReason = reason)
            runCatching { peopleRepo.updatePerson(personId, patch) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    vm: PersonDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var showAddNote by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.person?.fullName ?: "Person") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { onEdit(personId) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; showDeleteConfirm = true })
                    }
                }
            )
        }
    ) { padding ->
        val person = state.person
        if (state.loading && person == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (person == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Person not found")
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarComposable(person.fullName, size = 84.dp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(person.fullName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                    if (person.doNotContact) {
                        Spacer(Modifier.width(8.dp))
                        DoNotContactBadge()
                    }
                }
                val sub = listOfNotNull(person.title, person.company?.name).joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                Spacer(Modifier.height(6.dp))
                RelationshipStrengthBadge(person.relationshipStrength)
            }

            // Quick actions
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                QuickAction(Icons.Default.Notes, "Note") { showAddNote = true }
                QuickAction(Icons.Default.Check, "Task") { showAddTask = true }
                QuickAction(Icons.Default.Chat, "Discuss") { /* opens create discussion modal in future */ }
                QuickAction(Icons.Default.Schedule, "Follow") { /* TODO followup picker */ }
                QuickAction(Icons.Default.Mic, "Voice") { /* opens voice recorder via global FAB */ }
            }

            Spacer(Modifier.height(8.dp))

            // Contact info — multi-value emails and phones, tappable to launch
            // the default mail/dialer app. Primary entries are sorted first;
            // duplicates between the legacy and structured columns are deduped.
            val context = LocalContext.current
            val mergedEmails = remember(person) { mergePersonEmails(person) }
            val mergedPhones = remember(person) { mergePersonPhones(person) }
            if (mergedEmails.isNotEmpty() || mergedPhones.isNotEmpty()) {
                SectionHeader("Contact")
                mergedEmails.forEach { e ->
                    EmailRow(e, onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${e.value}"))
                            )
                        }
                    })
                }
                mergedPhones.forEach { ph ->
                    PhoneRow(ph, onClick = {
                        val cleaned = ph.value.filter { it.isDigit() || it == '+' }
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned"))
                            )
                        }
                    })
                }
            }

            // Birthday
            person.birthday?.let {
                SectionHeader("Birthday")
                Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            // Addresses
            if (person.addresses.isNotEmpty()) {
                SectionHeader("Addresses")
                person.addresses.forEach { AddressCard(it) }
            }

            // URLs / social
            val socials = buildList {
                person.linkedinUrl?.let { add("LinkedIn" to it) }
                person.facebookUrl?.let { add("Facebook" to it) }
                person.instagramHandle?.let { add("Instagram" to "@$it") }
                person.twitterXHandle?.let { add("X" to "@$it") }
                person.tiktokHandle?.let { add("TikTok" to "@$it") }
                person.whatsappPhone?.let { add("WhatsApp" to it) }
            }
            if (socials.isNotEmpty() || person.urls.isNotEmpty()) {
                SectionHeader("Online")
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    socials.forEach { (l, v) ->
                        SocialHandleChip(l, v)
                        Spacer(Modifier.width(6.dp))
                    }
                }
                if (person.urls.isNotEmpty()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        person.urls.forEach { UrlChip(it); Spacer(Modifier.width(6.dp)) }
                    }
                }
            }

            // Tags
            if (person.tags.isNotEmpty()) {
                SectionHeader("Tags")
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    person.tags.forEach { t -> LabeledChip(t.name); Spacer(Modifier.width(6.dp)) }
                }
            }

            // Do not contact
            DoNotContactCard(
                enabled = person.doNotContact,
                reason = person.doNotContactReason.orEmpty(),
                onToggle = vm::setDoNotContact,
                onReasonChange = vm::setDoNotContactReason
            )

            // Activity
            SectionHeader("Recent activity")
            Row(Modifier.padding(horizontal = 16.dp)) {
                OutlinedButton(onClick = { vm.refreshActivity() }, enabled = !state.refreshingActivity) {
                    if (state.refreshingActivity) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    else Text("Refresh signals")
                }
            }
            state.activity.forEach { a ->
                ActivityRow(a, onAck = { vm.acknowledge(a.id) })
            }
            if (state.activity.isEmpty()) {
                Text("No recent activity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), modifier = Modifier.padding(16.dp))
            }

            // What you remember (from quiz answers)
            if (state.quizMemories.isNotEmpty()) {
                SectionHeader("What you remember")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        state.quizMemories.forEach { mem ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    mem.questionText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    mem.answer ?: mem.suggestedResponses.firstOrNull() ?: "(answered)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Notes
            SectionHeader("Notes")
            state.notes.forEach { n ->
                Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        n.title?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                        Text(n.body)
                    }
                }
            }
            if (state.notes.isEmpty()) Text("No notes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), modifier = Modifier.padding(16.dp))

            // Tasks
            SectionHeader("Tasks")
            state.tasks.forEach { t ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = t.isComplete, onCheckedChange = { if (!t.isComplete) vm.completeTask(t.id) })
                    Column(Modifier.weight(1f)) {
                        Text(t.title)
                        t.dueAt?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            if (state.tasks.isEmpty()) Text("No tasks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), modifier = Modifier.padding(16.dp))

            // Discussions / Timeline
            if (state.discussions.isNotEmpty()) {
                SectionHeader("Discussions")
                state.discussions.forEach { d ->
                    Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${d.type.emoji} ${d.title}", fontWeight = FontWeight.SemiBold)
                            d.summary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            RelativeDateText(d.date)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAddNote) {
        AddNoteSheet(onDismiss = { showAddNote = false }, onSave = { t, b -> vm.addNote(t, b); showAddNote = false })
    }
    if (showAddTask) {
        AddTaskSheet(onDismiss = { showAddTask = false }, onSave = { title, due, pri -> vm.addTask(title, due, pri); showAddTask = false })
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete person?") },
            text = { Text("This cannot be undone.") },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; vm.delete(onBack) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DoNotContactCard(
    enabled: Boolean,
    reason: String,
    onToggle: (Boolean) -> Unit,
    onReasonChange: (String) -> Unit
) {
    val containerColor = if (enabled) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (enabled) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = if (enabled) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.error else contentColor.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Do not contact", fontWeight = FontWeight.SemiBold, color = contentColor)
                    Text(
                        if (enabled) "Drafts and outreach prompts are blocked." else "Mark this person to suppress reminders and outreach.",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.75f)
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            if (enabled) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason") },
                    placeholder = { Text("e.g. asked to be removed, deceased, ex-spouse, harassment, GDPR request") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    supportingText = { Text("Saved automatically.", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        FilledTonalIconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ActivityRow(a: SocialActivity, onAck: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        tonalElevation = if (a.acknowledgedAt == null) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(Modifier.padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("${a.source.name.uppercase()} · ${a.kind.orEmpty()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                a.content?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                a.occurredAt?.let { RelativeDateText(it) }
            }
            if (a.acknowledgedAt == null) {
                TextButton(onClick = onAck) { Text("Ack") }
            } else {
                Icon(Icons.Default.Check, contentDescription = "Acknowledged", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNoteSheet(onDismiss: () -> Unit, onSave: (String?, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("Add note", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Body") }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), minLines = 4)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onSave(title.ifBlank { null }, body) }, enabled = body.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskSheet(onDismiss: () -> Unit, onSave: (String, String?, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("Add task", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = due, onValueChange = { due = it }, label = { Text("Due (ISO 8601, optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row {
                listOf("low", "medium", "high", "urgent").forEach { p ->
                    FilterChip(selected = priority == p, onClick = { priority = if (priority == p) null else p }, label = { Text(p) })
                    Spacer(Modifier.width(6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onSave(title, due.ifBlank { null }, priority) }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
