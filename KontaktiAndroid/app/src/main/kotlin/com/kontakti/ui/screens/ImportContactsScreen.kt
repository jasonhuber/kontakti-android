package com.kontakti.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.contacts.DeviceContactsImporter
import com.kontakti.data.contacts.ImportCandidate
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
class ImportContactsViewModel @Inject constructor(
    private val importer: DeviceContactsImporter,
    private val api: ApiService,
    private val peopleRepo: PeopleRepository
) : ViewModel() {

    private val _candidates = MutableStateFlow<List<ImportCandidate>>(emptyList())
    val candidates: StateFlow<List<ImportCandidate>> = _candidates.asStateFlow()

    private val _selected = MutableStateFlow<Set<Int>>(emptySet())
    val selected: StateFlow<Set<Int>> = _selected.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult.asStateFlow()

    fun loadCandidates() {
        viewModelScope.launch {
            _loading.value = true
            _candidates.value = importer.fetchNewCandidates()
            // Pre-select all
            _selected.value = _candidates.value.indices.toSet()
            _loading.value = false
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
            _loading.value = true
            try {
                val result = api.importContacts(com.kontakti.data.contacts.BulkImportRequest(contacts = toImport))
                peopleRepo.refresh()
                _importResult.value = "Imported ${result.imported}, skipped ${result.skipped}"
            } catch (e: Exception) {
                _importResult.value = "Import failed: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearResult() { _importResult.value = null }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactsScreen(
    onBack: () -> Unit,
    viewModel: ImportContactsViewModel = hiltViewModel()
) {
    val candidates by viewModel.candidates.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val importResult by viewModel.importResult.collectAsState()

    var permissionGranted by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionRequested = true
        if (granted) viewModel.loadCandidates()
    }

    // Trigger permission request once on first composition
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    importResult?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from Phone") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (candidates.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.importSelected() },
                        enabled = selected.isNotEmpty() && !loading,
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
            when {
                permissionRequested && !permissionGranted -> {
                    EmptyState(
                        icon = { Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(48.dp)) },
                        title = "Contacts permission required",
                        subtitle = "Grant permission in Settings to import your phone contacts."
                    )
                }
                loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                candidates.isEmpty() && permissionGranted -> {
                    EmptyState(
                        icon = { Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(48.dp)) },
                        title = "All caught up",
                        subtitle = "All phone contacts are already in Kontakti."
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = "${candidates.size} new contact${if (candidates.size != 1) "s" else ""} found",
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

            importResult?.let { msg ->
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

@Composable
internal fun CandidateRow(
    candidate: ImportCandidate,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(candidate.name, fontWeight = FontWeight.Medium)
            if (!candidate.email.isNullOrBlank()) {
                Text(
                    candidate.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            if (!candidate.company.isNullOrBlank()) {
                Text(
                    candidate.company,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp)
}
