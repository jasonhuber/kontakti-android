package com.kontakti.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.SocialGroup
import com.kontakti.data.repository.SocialGroupRepository
import com.kontakti.ui.components.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SocialGroupsViewModel @Inject constructor(private val repo: SocialGroupRepository) : ViewModel() {
    private val _groups = MutableStateFlow<List<SocialGroup>>(emptyList())
    val groups: StateFlow<List<SocialGroup>> = _groups.asStateFlow()
    init { load() }
    fun load() { viewModelScope.launch { _groups.value = runCatching { repo.list() }.getOrDefault(emptyList()) } }
    fun sync(id: String) { viewModelScope.launch { runCatching { repo.sync(id) }; load() } }
    fun delete(id: String) { viewModelScope.launch { runCatching { repo.delete(id) }; load() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialGroupsScreen(onBack: () -> Unit, onImport: () -> Unit, vm: SocialGroupsViewModel = hiltViewModel()) {
    val groups by vm.groups.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Social groups") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onImport) { Icon(Icons.Default.Add, contentDescription = "Import") }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (groups.isEmpty()) {
                EmptyState(
                    icon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(48.dp)) },
                    title = "No groups linked",
                    subtitle = "Tap + to import a Facebook or WhatsApp group."
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(groups, key = { it.id }) { g ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(g.name ?: g.externalId, fontWeight = FontWeight.Medium)
                                Text("${g.source} · ${g.memberCount ?: "?"} members", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            IconButton(onClick = { vm.sync(g.id) }) { Icon(Icons.Default.Refresh, contentDescription = "Sync") }
                            TextButton(onClick = { vm.delete(g.id) }) { Text("Remove") }
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
