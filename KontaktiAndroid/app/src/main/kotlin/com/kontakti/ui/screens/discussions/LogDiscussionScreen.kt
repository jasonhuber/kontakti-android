package com.kontakti.ui.screens.discussions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.kontakti.data.model.CreateDiscussionRequest
import com.kontakti.data.model.DiscussionType
import com.kontakti.data.model.Person
import com.kontakti.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LogDiscussionViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Person>>(emptyList())
    val searchResults: StateFlow<List<Person>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun search(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            runCatching { api.listPeople(query = query) }
                .getOrNull()?.let { _searchResults.value = it.data }
            _isSearching.value = false
        }
    }

    fun submit(req: CreateDiscussionRequest, onCreated: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _error.value = null
            runCatching { api.createDiscussion(req) }
                .onSuccess { onCreated() }
                .onFailure { _error.value = it.message ?: "Could not log discussion." }
            _isSubmitting.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDiscussionScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    vm: LogDiscussionViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(DiscussionType.call) }
    var typeExpanded by remember { mutableStateOf(false) }
    var participantQuery by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<String, Person>() }

    val searchResults by vm.searchResults.collectAsState()
    val isSearching by vm.isSearching.collectAsState()
    val isSubmitting by vm.isSubmitting.collectAsState()
    val error by vm.error.collectAsState()

    val canSubmit = title.isNotBlank() && !isSubmitting
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log discussion") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            vm.submit(
                                CreateDiscussionRequest(
                                    title = title.trim(),
                                    date = today,
                                    type = type.name,
                                    summary = summary.trim().ifBlank { null },
                                    participantIds = selected.keys.toList().ifEmpty { null }
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
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = "${type.emoji} ${type.label}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    DiscussionType.values().forEach { t ->
                        DropdownMenuItem(
                            text = { Text("${t.emoji} ${t.label}") },
                            onClick = { type = t; typeExpanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text("Summary") },
                placeholder = { Text("What was discussed?") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Text("Participants", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = participantQuery,
                onValueChange = {
                    participantQuery = it
                    vm.search(it)
                },
                placeholder = { Text("Search people…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (isSearching) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (selected.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selected.values.forEach { person ->
                        AssistChip(
                            onClick = { selected.remove(person.id) },
                            label = { Text(person.firstName) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            searchResults.forEach { person ->
                ParticipantSearchRow(
                    person = person,
                    isSelected = selected.containsKey(person.id),
                    onToggle = {
                        if (selected.containsKey(person.id)) selected.remove(person.id)
                        else selected[person.id] = person
                    }
                )
            }

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

@Composable
private fun ParticipantSearchRow(person: Person, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(person.firstName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(person.fullName)
            person.title?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
