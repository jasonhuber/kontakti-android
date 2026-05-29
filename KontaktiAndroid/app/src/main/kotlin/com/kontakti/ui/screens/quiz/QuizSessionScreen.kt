package com.kontakti.ui.screens.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.ContactPrompt
import com.kontakti.data.repository.QuizRepository
import com.kontakti.data.repository.TodayRepository
import com.kontakti.ui.screens.today.QuizCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizSessionViewModel @Inject constructor(
    private val todayRepo: TodayRepository,
    private val quizRepo: QuizRepository
) : ViewModel() {
    private val _prompts = MutableStateFlow<List<ContactPrompt>>(emptyList())
    val prompts: StateFlow<List<ContactPrompt>> = _prompts.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { todayRepo.load() }
                .onSuccess { _prompts.value = it.quiz; _index.value = 0 }
            _loading.value = false
        }
    }

    fun answer(answer: String, structured: Map<String, Any?>? = null) {
        val current = _prompts.value.getOrNull(_index.value) ?: return
        viewModelScope.launch {
            runCatching { quizRepo.answer(current.id, answer, structured) }
            advance()
        }
    }

    fun skip() {
        val current = _prompts.value.getOrNull(_index.value) ?: return
        viewModelScope.launch {
            runCatching { quizRepo.skip(current.id) }
            advance()
        }
    }

    fun back() { if (_index.value > 0) _index.value -= 1 }
    fun next() { if (_index.value < _prompts.value.lastIndex) _index.value += 1 }
    private fun advance() {
        if (_index.value < _prompts.value.lastIndex) _index.value += 1
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizSessionScreen(
    onBack: () -> Unit,
    vm: QuizSessionViewModel = hiltViewModel()
) {
    val prompts by vm.prompts.collectAsState()
    val loading by vm.loading.collectAsState()
    val index by vm.index.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily quiz") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            val total = prompts.size
            val progress = if (total == 0) 0f else ((index + 1).toFloat() / total)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (total > 0) "Question ${index + 1} of $total" else "No questions",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(16.dp))

            when {
                loading && prompts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                prompts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("All done for today!", style = MaterialTheme.typography.titleMedium)
                }
                else -> {
                    val current = prompts[index.coerceIn(0, prompts.lastIndex)]
                    QuizCard(
                        prompt = current,
                        onAnswer = { answer, structured -> vm.answer(answer, structured) },
                        onSkip = { vm.skip() }
                    )
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { vm.back() }, enabled = index > 0) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = null)
                            Spacer(Modifier.width(4.dp)); Text("Back")
                        }
                        OutlinedButton(onClick = { vm.next() }, enabled = index < prompts.lastIndex) {
                            Text("Next"); Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
