package com.kontakti.ui.screens.companies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.local.CompanyEntity
import com.kontakti.data.repository.CompanyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompaniesViewModel @Inject constructor(
    private val repo: CompanyRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val companies: StateFlow<List<CompanyEntity>> = _query
        .flatMapLatest { q -> repo.getCompanies(q.ifBlank { null }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repo.refresh() }
    }

    fun setQuery(text: String) { _query.value = text }

    fun refresh() {
        viewModelScope.launch { repo.refresh() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompaniesListScreen(
    onBack: () -> Unit,
    onOpenCompany: (String) -> Unit,
    vm: CompaniesViewModel = hiltViewModel()
) {
    val items by vm.companies.collectAsState()
    val query by vm.query.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Companies") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = { Text("Search companies") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No companies",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { company ->
                        CompanyRow(company = company, onClick = { onOpenCompany(company.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyRow(company: CompanyEntity, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Business,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).clickable(onClick = onClick)) {
            Text(company.name, fontWeight = FontWeight.SemiBold)
            company.domain?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                company.industry?.let {
                    AssistChip(onClick = onClick, label = { Text(it, style = MaterialTheme.typography.labelSmall) })
                    Spacer(Modifier.width(6.dp))
                }
                company.peopleCount?.let { count ->
                    if (count > 0) {
                        Text(
                            "$count ${if (count == 1) "person" else "people"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

