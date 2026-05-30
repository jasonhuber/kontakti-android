package com.kontakti.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.datastore.TokenStore
import com.kontakti.data.sync.SyncDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncDirectionViewModel @Inject constructor(
    private val tokenStore: TokenStore
) : ViewModel() {

    val direction: StateFlow<SyncDirection> = tokenStore.syncDirectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncDirection.TWO_WAY)

    fun select(d: SyncDirection) {
        viewModelScope.launch { tokenStore.saveSyncDirection(d) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDirectionScreen(
    onBack: () -> Unit,
    vm: SyncDirectionViewModel = hiltViewModel()
) {
    val current by vm.direction.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Direction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                "Choose how Kontakti syncs data between this device and the server.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            SyncDirection.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.select(option) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = current == option,
                        onClick = { vm.select(option) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = when (option) {
                                SyncDirection.TWO_WAY -> "Changes on this device upload; server changes download."
                                SyncDirection.DOWNLOAD_ONLY -> "Only pull updates from the server. Local changes stay offline."
                                SyncDirection.UPLOAD_ONLY -> "Only push local changes. No server data is downloaded."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}
