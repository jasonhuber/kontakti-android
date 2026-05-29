package com.kontakti.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.contacts.ImportCandidate
import com.kontakti.data.google.GoogleAuthManager
import com.kontakti.data.google.GoogleContactsService
import com.kontakti.data.network.ApiService
import com.kontakti.data.repository.PeopleRepository
import com.kontakti.ui.components.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class GmailImportViewModel @Inject constructor(
    val authManager: GoogleAuthManager,
    private val googleContactsService: GoogleContactsService,
    private val api: ApiService,
    private val peopleRepo: PeopleRepository
) : ViewModel() {

    enum class State { IDLE, SIGNING_IN, LOADING, READY, IMPORTING, DONE, ERROR }

    private val _uiState = MutableStateFlow(State.IDLE)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _candidates = MutableStateFlow<List<ImportCandidate>>(emptyList())
    val candidates: StateFlow<List<ImportCandidate>> = _candidates.asStateFlow()

    private val _selected = MutableStateFlow<Set<Int>>(emptySet())
    val selected: StateFlow<Set<Int>> = _selected.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        // If already signed in, attempt to load immediately
        val existing = authManager.getLastSignedInAccount()
        if (existing?.serverAuthCode != null) {
            loadWithToken(existing.serverAuthCode!!)
        }
    }

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.value = State.LOADING
            val token = authManager.handleSignInResult(data)
            if (token != null) {
                loadWithToken(token)
            } else {
                _uiState.value = State.ERROR
                _message.value = "Google sign-in failed or was cancelled"
            }
        }
    }

    private fun loadWithToken(token: String) {
        viewModelScope.launch {
            _uiState.value = State.LOADING
            try {
                val all = googleContactsService.fetchCandidates(token)
                _candidates.value = all
                _selected.value = all.indices.toSet()
                _uiState.value = State.READY
            } catch (e: Exception) {
                _uiState.value = State.ERROR
                _message.value = "Failed to load contacts: ${e.message}"
            }
        }
    }

    fun toggle(index: Int) {
        _selected.value = if (index in _selected.value) {
            _selected.value - index
        } else {
            _selected.value + index
        }
    }

    fun importSelected() {
        val toImport = _selected.value.mapNotNull { _candidates.value.getOrNull(it) }
        if (toImport.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = State.IMPORTING
            try {
                val result = api.importContacts(com.kontakti.data.contacts.BulkImportRequest(contacts = toImport))
                peopleRepo.refresh()
                _message.value = "Imported ${result.imported}, skipped ${result.skipped}"
                _uiState.value = State.DONE
            } catch (e: Exception) {
                _message.value = "Import failed: ${e.message}"
                _uiState.value = State.READY
            }
        }
    }

    fun clearMessage() { _message.value = null }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmailImportScreen(
    onBack: () -> Unit,
    viewModel: GmailImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val candidates by viewModel.candidates.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val message by viewModel.message.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    message?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3500)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from Gmail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState == GmailImportViewModel.State.READY && candidates.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.importSelected() },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Import ${selected.size} contact${if (selected.size != 1) "s" else ""}")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                GmailImportViewModel.State.IDLE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Connect Gmail",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Import contacts from Google Contacts and discover frequent email senders.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { signInLauncher.launch(viewModel.authManager.getSignInIntent()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign in with Google")
                        }
                    }
                }

                GmailImportViewModel.State.SIGNING_IN,
                GmailImportViewModel.State.LOADING,
                GmailImportViewModel.State.IMPORTING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            when (uiState) {
                                GmailImportViewModel.State.LOADING -> "Loading contacts..."
                                GmailImportViewModel.State.IMPORTING -> "Importing..."
                                else -> "Connecting..."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                GmailImportViewModel.State.ERROR -> {
                    EmptyState(
                        icon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(48.dp)) },
                        title = "Connection failed",
                        subtitle = message ?: "Try signing in again."
                    )
                }

                GmailImportViewModel.State.DONE -> {
                    EmptyState(
                        icon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(48.dp)) },
                        title = "Import complete",
                        subtitle = message ?: "Contacts have been added to Kontakti."
                    )
                }

                GmailImportViewModel.State.READY -> {
                    if (candidates.isEmpty()) {
                        EmptyState(
                            icon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(48.dp)) },
                            title = "All caught up",
                            subtitle = "All Gmail contacts are already in Kontakti."
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = "${candidates.size} contact${if (candidates.size != 1) "s" else ""} found",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            items(candidates.indices.toList()) { index ->
                                val candidate = candidates[index]
                                CandidateRow(
                                    candidate = candidate,
                                    checked = index in selected,
                                    onToggle = { viewModel.toggle(index) }
                                )
                            }
                        }
                    }
                }
            }

            message?.let { msg ->
                if (uiState != GmailImportViewModel.State.DONE && uiState != GmailImportViewModel.State.ERROR) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Text(msg)
                    }
                }
            }
        }
    }
}
