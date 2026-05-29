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
import com.kontakti.data.model.Discussion
import com.kontakti.data.model.Person
import com.kontakti.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscussionDetailViewModel @Inject constructor(
    private val api: ApiService,
    savedState: SavedStateHandle
) : ViewModel() {
    private val id: String = savedState.get<String>("discussionId").orEmpty()

    private val _discussion = MutableStateFlow<Discussion?>(null)
    val discussion: StateFlow<Discussion?> = _discussion.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { api.getDiscussion(id) }.getOrNull()?.let { _discussion.value = it }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionDetailScreen(
    onBack: () -> Unit,
    onOpenPerson: (String) -> Unit,
    vm: DiscussionDetailViewModel = hiltViewModel()
) {
    val d by vm.discussion.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(d?.title ?: "Discussion") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val discussion = d ?: run {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(discussion.type.emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(discussion.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        discussion.date.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            discussion.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                Spacer(Modifier.height(16.dp))
                Text("Summary", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(summary, modifier = Modifier.padding(12.dp)) }
            }

            discussion.body?.takeIf { it.isNotBlank() }?.let { body ->
                Spacer(Modifier.height(16.dp))
                Text("Notes", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(body)
            }

            discussion.participants?.takeIf { it.isNotEmpty() }?.let { participants ->
                Spacer(Modifier.height(20.dp))
                Text("Participants", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                participants.forEach { person ->
                    ParticipantRow(person = person, onClick = { onOpenPerson(person.id) })
                }
            }
        }
    }
}

@Composable
private fun ParticipantRow(person: Person, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
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
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(person.fullName, fontWeight = FontWeight.Medium)
            person.email?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
