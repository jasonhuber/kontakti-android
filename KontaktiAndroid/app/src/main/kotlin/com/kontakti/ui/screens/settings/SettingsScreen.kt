package com.kontakti.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.datastore.TokenStore
import com.kontakti.data.model.GoogleAccount
import com.kontakti.data.repository.GoogleAccountsRepository
import com.kontakti.data.repository.PushRepository
import com.kontakti.ui.components.SectionHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val googleRepo: GoogleAccountsRepository,
    private val pushRepo: PushRepository,
    private val tokenStore: TokenStore
) : ViewModel() {
    private val _accounts = MutableStateFlow<List<GoogleAccount>>(emptyList())
    val accounts: StateFlow<List<GoogleAccount>> = _accounts.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    init { loadAccounts() }

    fun loadAccounts() {
        viewModelScope.launch {
            _accounts.value = runCatching { googleRepo.list() }.getOrDefault(emptyList())
        }
    }

    fun makePrimary(id: String) {
        viewModelScope.launch {
            runCatching { googleRepo.update(id, isPrimary = true) }
            loadAccounts()
        }
    }

    fun unlink(id: String) {
        viewModelScope.launch {
            runCatching { googleRepo.unlink(id) }
            loadAccounts()
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            tokenStore.clearToken()
            onDone()
        }
    }

    fun setNotifications(enabled: Boolean, token: String?) {
        _notificationsEnabled.value = enabled
        token?.let {
            viewModelScope.launch {
                if (enabled) pushRepo.register(it) else pushRepo.unregister(it)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenGroups: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenImport: () -> Unit,
    onLinkGoogle: () -> Unit,
    fcmTokenProvider: () -> String? = { null },
    onSignedOut: () -> Unit = {},
    onOpenActivity: () -> Unit = {},
    onOpenReview: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val accounts by vm.accounts.collectAsState()
    val notifications by vm.notificationsEnabled.collectAsState()
    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotifications(granted, fcmTokenProvider())
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { SectionHeader("Linked Google accounts") }
            items(accounts, key = { it.id }) { acc ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(acc.label ?: acc.email, fontWeight = FontWeight.Medium)
                        Text(acc.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    if (acc.isPrimary) AssistChip(onClick = {}, label = { Text("Primary") })
                    else TextButton(onClick = { vm.makePrimary(acc.id) }) { Text("Set primary") }
                    TextButton(onClick = { vm.unlink(acc.id) }) { Text("Unlink") }
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
            item {
                TextButton(onClick = onLinkGoogle, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null); Text(" Link Google account")
                }
            }

            item { SectionHeader("Data") }
            item { SettingsRow("Activity feed", Icons.Default.Inbox, onClick = onOpenActivity) }
            item { SettingsRow("Import contacts", Icons.Default.Contacts, onClick = onOpenImport) }
            item { SettingsRow("Social groups", Icons.Default.Group, onClick = onOpenGroups) }
            item { SettingsRow("Duplicates", Icons.Default.ContentCopy, onClick = onOpenDuplicates) }
            item { SettingsRow("Review contacts", Icons.Default.FactCheck, onClick = onOpenReview) }

            item { SectionHeader("Notifications") }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Enable push notifications", modifier = Modifier.weight(1f))
                    Switch(
                        checked = notifications,
                        onCheckedChange = { wanted ->
                            if (wanted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                vm.setNotifications(wanted, fcmTokenProvider())
                            }
                        }
                    )
                }
            }

            item { SectionHeader("Account") }
            item { SettingsRow("Sign out", Icons.Default.ExitToApp, onClick = { vm.signOut(onSignedOut) }) }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SettingsRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}
