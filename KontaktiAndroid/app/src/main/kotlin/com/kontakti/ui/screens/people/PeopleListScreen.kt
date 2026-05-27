package com.kontakti.ui.screens.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.local.PersonEntity
import com.kontakti.data.repository.PeopleRepository
import com.kontakti.ui.components.AvatarComposable
import com.kontakti.ui.components.DoNotContactBadge
import com.kontakti.ui.components.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PeopleListViewModel @Inject constructor(
    private val repo: PeopleRepository
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val people: Flow<List<PersonEntity>> =
        _query.flatMapLatest { q -> repo.getPeople(if (q.isBlank()) null else q) }

    init { refresh() }

    fun setQuery(q: String) { _query.value = q }

    fun refresh() {
        viewModelScope.launch { repo.refresh() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleListScreen(
    onOpenPerson: (String) -> Unit,
    onAddPerson: () -> Unit,
    vm: PeopleListViewModel = hiltViewModel()
) {
    val q by vm.query.collectAsState()
    val list by vm.people.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("People") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPerson) {
                Icon(Icons.Default.Add, contentDescription = "Add person")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = q,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search name, email, company") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            )
            if (list.isEmpty()) {
                EmptyState(
                    icon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(48.dp)) },
                    title = if (q.isBlank()) "No people yet" else "No matches",
                    subtitle = if (q.isBlank()) "Tap + to add someone, or import contacts from Settings." else null
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(list, key = { it.id }) { p ->
                        PersonRow(p, onClick = { onOpenPerson(p.id) })
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonRow(p: PersonEntity, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        AvatarComposable(p.fullName)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.fullName, fontWeight = FontWeight.Medium)
                if (p.doNotContact) {
                    Spacer(Modifier.width(6.dp))
                    DoNotContactBadge()
                }
            }
            val sub = listOfNotNull(p.title, p.companyName).joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            else if (!p.email.isNullOrBlank()) Text(p.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
    }
}
