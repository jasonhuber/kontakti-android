package com.kontakti.ui.screens.voice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.VoiceCaptureResult
import com.kontakti.data.repository.VoiceRepository
import com.kontakti.data.voice.VoiceRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val recorder: VoiceRecorder,
    private val repo: VoiceRepository
) : ViewModel() {
    data class State(
        val isRecording: Boolean = false,
        val elapsedSeconds: Int = 0,
        val uploading: Boolean = false,
        val result: VoiceCaptureResult? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var currentFile: File? = null

    fun start() {
        try {
            currentFile = recorder.start()
            _state.value = State(isRecording = true)
            viewModelScope.launch {
                var s = 0
                while (isActive && _state.value.isRecording) {
                    delay(1000)
                    s += 1
                    _state.value = _state.value.copy(elapsedSeconds = s)
                }
            }
        } catch (e: Exception) {
            _state.value = State(error = e.message)
        }
    }

    fun stopAndUpload(personId: String? = null) {
        val file = recorder.stop() ?: currentFile
        _state.value = _state.value.copy(isRecording = false, uploading = true)
        if (file == null || !file.exists()) {
            _state.value = _state.value.copy(uploading = false, error = "No audio captured")
            return
        }
        viewModelScope.launch {
            try {
                val res = repo.capture(file, personId)
                _state.value = _state.value.copy(uploading = false, result = res)
            } catch (e: Exception) {
                _state.value = _state.value.copy(uploading = false, error = e.message)
            }
        }
    }

    fun cancel() {
        recorder.cancel()
        _state.value = State()
    }

    fun reset() { _state.value = State() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecorderScreen(
    onBack: () -> Unit,
    personId: String? = null,
    vm: VoiceViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var permissionGranted by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }
    LaunchedEffect(Unit) { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice memo") },
                navigationIcon = { IconButton(onClick = { vm.cancel(); onBack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        val res = state.result
        if (res != null) {
            VoiceResultReview(res, onDone = { vm.reset(); onBack() })
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!permissionGranted) {
                Text("Microphone permission required.")
                return@Column
            }
            val formatted = "%02d:%02d".format(state.elapsedSeconds / 60, state.elapsedSeconds % 60)
            Text(formatted, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            if (state.uploading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Uploading…")
            } else if (state.isRecording) {
                FilledIconButton(
                    onClick = { vm.stopAndUpload(personId) },
                    modifier = Modifier.size(96.dp).clip(CircleShape)
                ) { Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(48.dp)) }
            } else {
                FilledIconButton(
                    onClick = { vm.start() },
                    modifier = Modifier.size(96.dp).clip(CircleShape)
                ) { Icon(Icons.Default.Mic, contentDescription = "Record", modifier = Modifier.size(48.dp)) }
            }
            state.error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
