package com.kontakti.ui.screens.companies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.CreateCompanyRequest
import com.kontakti.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCompanyViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun submit(req: CreateCompanyRequest, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _error.value = null
            runCatching { api.createCompany(req) }
                .onSuccess { onCreated(it.id) }
                .onFailure { _error.value = it.message ?: "Could not create company." }
            _isSubmitting.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCompanyScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    vm: AddCompanyViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var linkedinUrl by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val isSubmitting by vm.isSubmitting.collectAsState()
    val error by vm.error.collectAsState()
    val canSubmit = name.isNotBlank() && !isSubmitting

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New company") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            vm.submit(
                                CreateCompanyRequest(
                                    name = name.trim(),
                                    domain = domain.trim().ifBlank { null },
                                    industry = industry.trim().ifBlank { null },
                                    website = website.trim().ifBlank { null },
                                    linkedinUrl = linkedinUrl.trim().ifBlank { null },
                                    notes = notes.trim().ifBlank { null }
                                ),
                                onCreated
                            )
                        },
                        enabled = canSubmit
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = domain,
                onValueChange = { domain = it },
                label = { Text("Domain") },
                singleLine = true,
                placeholder = { Text("example.com") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = industry,
                onValueChange = { industry = it },
                label = { Text("Industry") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                label = { Text("Website") },
                singleLine = true,
                placeholder = { Text("https://example.com") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = linkedinUrl,
                onValueChange = { linkedinUrl = it },
                label = { Text("LinkedIn URL") },
                singleLine = true,
                placeholder = { Text("https://linkedin.com/company/...") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            error?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
