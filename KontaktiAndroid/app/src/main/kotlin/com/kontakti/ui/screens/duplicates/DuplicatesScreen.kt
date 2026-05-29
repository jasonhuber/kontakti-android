package com.kontakti.ui.screens.duplicates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.DuplicateCandidate
import com.kontakti.data.model.Person
import com.kontakti.data.repository.DuplicatesRepository
import com.kontakti.ui.components.AvatarComposable
import com.kontakti.ui.components.DoNotContactBadge
import com.kontakti.ui.components.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DuplicatesViewModel @Inject constructor(private val repo: DuplicatesRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<DuplicateCandidate>>(emptyList())
    val items: StateFlow<List<DuplicateCandidate>> = _items.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init { load() }

    fun load() { viewModelScope.launch {
        _loading.value = true
        _items.value = runCatching { repo.list() }.getOrDefault(emptyList())
        _loading.value = false
    } }

    fun scan() { viewModelScope.launch {
        _loading.value = true
        runCatching { repo.scan() }
        _items.value = runCatching { repo.list() }.getOrDefault(emptyList())
        _loading.value = false
    } }

    fun merge(id: String, primaryId: String, otherIds: List<String>) {
        viewModelScope.launch {
            runCatching { repo.merge(id, primaryId, otherIds) }
            load()
        }
    }

    fun dismiss(id: String) {
        viewModelScope.launch { runCatching { repo.dismiss(id) }; load() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(onBack: () -> Unit, vm: DuplicatesViewModel = hiltViewModel()) {
    val items by vm.items.collectAsState()
    val loading by vm.loading.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicates") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = { TextButton(onClick = { vm.scan() }, enabled = !loading) { Text("Scan") } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading && items.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                items.isEmpty() -> EmptyState(
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(48.dp)) },
                    title = "No duplicates pending",
                    subtitle = "Tap Scan to look for new ones."
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { c ->
                        DuplicateCard(c, onMerge = { primary -> vm.merge(c.id, primary, c.people.filter { it.id != primary }.map { it.id }) }, onDismiss = { vm.dismiss(c.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateCard(c: DuplicateCandidate, onMerge: (String) -> Unit, onDismiss: () -> Unit) {
    var primary by remember { mutableStateOf(c.people.firstOrNull()?.id) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            c.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) }
            c.score?.let { Text("Confidence: ${"%.0f".format(it * 100)}%", style = MaterialTheme.typography.labelSmall) }
            Spacer(Modifier.height(8.dp))
            c.people.forEach { p ->
                DuplicatePersonRow(p, primary == p.id) { primary = p.id }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Button(enabled = primary != null && c.people.size >= 2, onClick = { primary?.let(onMerge) }) { Text("Merge") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onDismiss) { Text("Not duplicates") }
            }
        }
    }
}

@Composable
private fun DuplicatePersonRow(p: Person, isPrimary: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 6.dp)
    ) {
        RadioButton(selected = isPrimary, onClick = onSelect)
        AvatarComposable(p.fullName, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.fullName, fontWeight = FontWeight.Medium)
                if (p.doNotContact) {
                    Spacer(Modifier.width(6.dp))
                    DoNotContactBadge()
                }
            }
            val sub = listOfNotNull(p.email, p.company?.name).joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
