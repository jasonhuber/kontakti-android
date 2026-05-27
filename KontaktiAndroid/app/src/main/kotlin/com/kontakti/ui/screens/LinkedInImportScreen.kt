package com.kontakti.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.CreatePersonRequest
import com.kontakti.data.network.ApiService
import com.kontakti.data.network.EnrichmentService
import com.kontakti.data.repository.PeopleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class LinkedInImportViewModel @Inject constructor(
    private val enrichmentService: EnrichmentService,
    private val api: ApiService,
    private val peopleRepo: PeopleRepository
) : ViewModel() {

    enum class State { IDLE, ENRICHING, FORM, SAVING, DONE, ERROR }

    private val _uiState = MutableStateFlow(State.IDLE)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Form fields — mutable so the user can edit before saving
    val linkedinUrl = MutableStateFlow("")
    val firstName = MutableStateFlow("")
    val lastName = MutableStateFlow("")
    val title = MutableStateFlow("")
    val companyName = MutableStateFlow("")
    val email = MutableStateFlow("")
    val phone = MutableStateFlow("")

    fun enrich(url: String) {
        if (url.isBlank()) {
            _error.value = "Paste a LinkedIn profile URL first."
            return
        }
        _error.value = null
        viewModelScope.launch {
            _uiState.value = State.ENRICHING
            try {
                val result = enrichmentService.enrich(url.trim())
                val person = result.person
                linkedinUrl.value = person.linkedinUrl ?: url.trim()
                firstName.value = person.firstName ?: ""
                lastName.value = person.lastName ?: ""
                title.value = person.title ?: ""
                companyName.value = person.company?.name ?: ""
                email.value = person.email ?: ""
                phone.value = person.phone ?: ""
                if (result.source == "url_only") {
                    _error.value = "LinkedIn blocked server-side fetch — fill in any missing fields manually."
                }
                _uiState.value = State.FORM
            } catch (e: Exception) {
                _error.value = e.message ?: "Enrichment failed"
                _uiState.value = State.ERROR
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.value = State.SAVING
            _error.value = null
            try {
                api.createPerson(
                    CreatePersonRequest(
                        firstName = firstName.value.trim().ifBlank { null },
                        lastName = lastName.value.trim().ifBlank { null },
                        email = email.value.trim().ifBlank { null },
                        phone = phone.value.trim().ifBlank { null },
                        linkedinUrl = linkedinUrl.value.trim().ifBlank { null },
                        title = title.value.trim().ifBlank { null },
                        companyName = companyName.value.trim().ifBlank { null }
                    )
                )
                peopleRepo.refresh()
                _uiState.value = State.DONE
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save person"
                _uiState.value = State.FORM
            }
        }
    }

    fun reset() {
        linkedinUrl.value = ""
        firstName.value = ""
        lastName.value = ""
        title.value = ""
        companyName.value = ""
        email.value = ""
        phone.value = ""
        _error.value = null
        _uiState.value = State.IDLE
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedInImportScreen(
    onBack: () -> Unit,
    viewModel: LinkedInImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    val firstName by viewModel.firstName.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val title by viewModel.title.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val email by viewModel.email.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val linkedinUrl by viewModel.linkedinUrl.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from LinkedIn") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                LinkedInImportViewModel.State.ENRICHING,
                LinkedInImportViewModel.State.SAVING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (uiState == LinkedInImportViewModel.State.ENRICHING) "Enriching profile..."
                            else "Saving to Kontakti...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                LinkedInImportViewModel.State.DONE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Text(
                            "Person added",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val displayName = listOfNotNull(
                            firstName.ifBlank { null },
                            lastName.ifBlank { null }
                        ).joinToString(" ").ifBlank { "Contact" }
                        Text(
                            "$displayName has been added to Kontakti.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.reset(); urlInput = "" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Import another")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done")
                        }
                    }
                }

                else -> {
                    // IDLE, FORM, ERROR all use the same scrollable column
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // URL input + Enrich button
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("LinkedIn profile URL") },
                            placeholder = { Text("https://www.linkedin.com/in/someone") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { viewModel.enrich(urlInput) },
                            enabled = uiState != LinkedInImportViewModel.State.ENRICHING,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enrich")
                        }

                        // Inline error
                        error?.let { msg ->
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Pre-filled editable form — visible once we have enriched data
                        if (uiState == LinkedInImportViewModel.State.FORM ||
                            uiState == LinkedInImportViewModel.State.SAVING
                        ) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                "Review and edit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = firstName,
                                    onValueChange = { viewModel.firstName.value = it },
                                    label = { Text("First name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = lastName,
                                    onValueChange = { viewModel.lastName.value = it },
                                    label = { Text("Last name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = title,
                                onValueChange = { viewModel.title.value = it },
                                label = { Text("Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = companyName,
                                onValueChange = { viewModel.companyName.value = it },
                                label = { Text("Company") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { viewModel.email.value = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { viewModel.phone.value = it },
                                label = { Text("Phone") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = linkedinUrl,
                                onValueChange = { viewModel.linkedinUrl.value = it },
                                label = { Text("LinkedIn URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { viewModel.save() },
                                enabled = uiState == LinkedInImportViewModel.State.FORM,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add to Kontakti")
                            }
                        }

                        // Bottom padding so the last field isn't flush with the bottom
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
