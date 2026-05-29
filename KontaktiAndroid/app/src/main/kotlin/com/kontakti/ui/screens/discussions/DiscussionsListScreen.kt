package com.kontakti.ui.screens.discussions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.Discussion
import com.kontakti.data.model.DiscussionType
import com.kontakti.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscussionsListViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _discussions = MutableStateFlow<List<Discussion>>(emptyList())
    val discussions: StateFlow<List<Discussion>> = _discussions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedType = MutableStateFlow<DiscussionType?>(null)
    val selectedType: StateFlow<DiscussionType?> = _selectedType.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init { load() }

    fun setType(type: DiscussionType?) {
        _selectedType.value = type
        load()
    }

    fun setQuery(text: String) { _query.value = text }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val q = _query.value.ifBlank { null }
            val t = _selectedType.value?.name
            runCatching { api.listDiscussions(query = q, type = t) }
                .getOrNull()?.let { _discussions.value = it.data }
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionsListScreen(
    onOpenDiscussion: (String) -> Unit,
    onLogDiscussion: () -> Unit,
    vm: DiscussionsListViewModel = hiltViewModel()
) {
    val discussions by vm.discussions.collectAsState()
    val loading by vm.isLoading.collectAsState()
    val selected by vm.selectedType.collectAsState()
    val query by vm.query.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussions") },
                actions = {
                    IconButton(onClick = onLogDiscussion) {
                        Icon(Icons.Default.Add, contentDescription = "Log discussion")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    vm.setQuery(it)
                    vm.load()
                },
                label = { Text("Search discussions") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            TypeFilter(selected = selected, onSelect = vm::setType)

            when {
                loading && discussions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                discussions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No discussions yet",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(discussions, key = { it.id }) { d ->
                            DiscussionRow(discussion = d, onClick = { onOpenDiscussion(d.id) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeFilter(selected: DiscussionType?, onSelect: (DiscussionType?) -> Unit) {
    val types: List<DiscussionType?> = listOf(null) + DiscussionType.values().toList()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        types.forEach { t ->
            val label = t?.let { "${it.emoji} ${it.label}" } ?: "All"
            FilterChip(
                selected = selected == t,
                onClick = { onSelect(t) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun DiscussionRow(discussion: Discussion, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(discussion.type.emoji)
            Spacer(Modifier.width(8.dp))
            Text(
                discussion.title,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(
                discussion.date.take(10),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        discussion.summary?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        discussion.participants?.takeIf { it.isNotEmpty() }?.let { list ->
            Text(
                "${list.size} participant${if (list.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
