package com.kontakti.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.NaturalSearchHit
import com.kontakti.data.repository.SearchRepository
import com.kontakti.ui.components.AvatarComposable
import com.kontakti.ui.components.DoNotContactBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NaturalSearchViewModel @Inject constructor(private val repo: SearchRepository) : ViewModel() {
    data class State(val query: String = "", val loading: Boolean = false, val results: List<NaturalSearchHit> = emptyList(), val error: String? = null)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun setQuery(q: String) { _state.value = _state.value.copy(query = q) }
    fun submit() {
        val q = _state.value.query.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val r = repo.natural(q)
                _state.value = _state.value.copy(loading = false, results = r.results)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaturalSearchScreen(onOpenPerson: (String) -> Unit, vm: NaturalSearchViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Ask anything") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = s.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("e.g. 'devs in Berlin I haven't talked to in 6 months'") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                singleLine = true
            )
            Button(onClick = vm::submit, enabled = !s.loading && s.query.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Text(if (s.loading) "Thinking…" else "Search")
            }
            Spacer(Modifier.height(8.dp))
            if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
            LazyColumn(Modifier.fillMaxSize()) {
                items(s.results, key = { it.person.id }) { hit ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { onOpenPerson(hit.person.id) }
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            AvatarComposable(hit.person.fullName, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text(hit.person.fullName, fontWeight = FontWeight.SemiBold)
                                    if (hit.person.doNotContact) {
                                        Spacer(Modifier.width(6.dp))
                                        DoNotContactBadge()
                                    }
                                }
                                hit.reasoning?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) }
                            }
                            hit.score?.let { Text("${"%.0f".format(it * 100)}%", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        }
    }
}
