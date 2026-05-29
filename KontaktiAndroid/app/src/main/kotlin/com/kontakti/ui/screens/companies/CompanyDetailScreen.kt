package com.kontakti.ui.screens.companies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.Company
import com.kontakti.data.model.Person
import com.kontakti.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompanyDetailViewModel @Inject constructor(
    private val api: ApiService,
    savedState: SavedStateHandle
) : ViewModel() {
    private val companyId: String = savedState.get<String>("companyId").orEmpty()

    private val _company = MutableStateFlow<Company?>(null)
    val company: StateFlow<Company?> = _company.asStateFlow()

    private val _people = MutableStateFlow<List<Person>>(emptyList())
    val people: StateFlow<List<Person>> = _people.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { api.getCompany(companyId) }.getOrNull()?.let { _company.value = it }
            runCatching { api.getCompanyPeople(companyId) }.getOrNull()?.let { _people.value = it }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDetailScreen(
    onBack: () -> Unit,
    onOpenPerson: (String) -> Unit,
    vm: CompanyDetailViewModel = hiltViewModel()
) {
    val company by vm.company.collectAsState()
    val people by vm.people.collectAsState()
    val loading by vm.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(company?.name ?: "Company") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (loading && company == null) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val c = company ?: return@Scaffold
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Header(c)

            if (people.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SectionTitle("People")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    items(people, key = { it.id }) { person ->
                        PersonChip(person = person, onClick = { onOpenPerson(person.id) })
                    }
                }
            }

            c.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(Modifier.height(16.dp))
                SectionTitle("Notes")
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(notes, modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Header(c: Company) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Business,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(c.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        c.domain?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(8.dp))
        Row {
            c.industry?.let {
                AssistChip(onClick = {}, label = { Text(it) })
                Spacer(Modifier.width(6.dp))
            }
            c.sizeRange?.let {
                AssistChip(onClick = {}, label = { Text(it) })
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun PersonChip(person: Person, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                person.firstName.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(person.firstName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
