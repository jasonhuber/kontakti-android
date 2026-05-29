package com.kontakti.ui.screens.groups

import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.*
import com.kontakti.data.repository.SocialGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupImportViewModel @Inject constructor(private val repo: SocialGroupRepository) : ViewModel() {
    data class FbState(val loading: Boolean = false, val groups: List<FacebookGroup> = emptyList(), val error: String? = null, val remediation: String? = null)
    data class WaState(val status: WhatsappStatus? = null, val qr: WhatsappQR? = null, val groups: List<WhatsappGroup> = emptyList(), val polling: Boolean = false)

    private val _fb = MutableStateFlow(FbState())
    val fb: StateFlow<FbState> = _fb.asStateFlow()
    private val _wa = MutableStateFlow(WaState())
    val wa: StateFlow<WaState> = _wa.asStateFlow()

    fun loadFacebook() {
        viewModelScope.launch {
            _fb.value = FbState(loading = true)
            try {
                val r = repo.facebookGroups()
                _fb.value = FbState(groups = r.groups, error = r.error, remediation = r.remediation)
            } catch (e: Exception) {
                _fb.value = FbState(error = e.message)
            }
        }
    }

    fun loadWhatsapp() {
        viewModelScope.launch {
            val status = runCatching { repo.whatsappStatus() }.getOrNull()
            _wa.value = _wa.value.copy(status = status)
            if (status?.paired == true) {
                val groups = runCatching { repo.whatsappGroups().groups }.getOrDefault(emptyList())
                _wa.value = _wa.value.copy(groups = groups)
            } else if (status?.qrRequired == true) {
                fetchQRAndPoll()
            }
        }
    }

    private fun fetchQRAndPoll() {
        viewModelScope.launch {
            _wa.value = _wa.value.copy(polling = true)
            val qr = runCatching { repo.whatsappQR() }.getOrNull()
            _wa.value = _wa.value.copy(qr = qr)
            // Poll status until paired
            repeat(60) {
                delay(3000)
                val s = runCatching { repo.whatsappStatus() }.getOrNull()
                if (s?.paired == true) {
                    val groups = runCatching { repo.whatsappGroups().groups }.getOrDefault(emptyList())
                    _wa.value = WaState(status = s, qr = null, groups = groups, polling = false)
                    return@launch
                }
            }
            _wa.value = _wa.value.copy(polling = false)
        }
    }

    fun importGroup(source: String, externalId: String, name: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.create(source, externalId, name) }
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupImportWizard(
    onBack: () -> Unit,
    vm: GroupImportViewModel = hiltViewModel()
) {
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(tab) {
        when (tab) {
            0 -> vm.loadFacebook()
            1 -> vm.loadWhatsapp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import groups") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Facebook") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("WhatsApp") })
            }
            when (tab) {
                0 -> FacebookTab(vm, onBack)
                1 -> WhatsappTab(vm, onBack)
            }
        }
    }
}

@Composable
private fun FacebookTab(vm: GroupImportViewModel, onBack: () -> Unit) {
    val state by vm.fb.collectAsState()
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.error != null -> Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Couldn't load Facebook groups", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(state.error ?: "")
            state.remediation?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.loadFacebook() }) { Text("Retry") }
        }
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(state.groups, key = { it.id }) { g ->
                GroupPickRow(
                    title = g.name,
                    subtitle = g.memberCount?.let { "$it members" },
                    onPick = { vm.importGroup("facebook", g.id, g.name, onBack) }
                )
            }
        }
    }
}

@Composable
private fun WhatsappTab(vm: GroupImportViewModel, onBack: () -> Unit) {
    val state by vm.wa.collectAsState()
    val s = state.status
    when {
        s == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        s.paired -> LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text("Paired as ${s.phoneNumber ?: "unknown"}", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
            }
            items(state.groups, key = { it.id }) { g ->
                GroupPickRow(
                    title = g.name,
                    subtitle = g.memberCount?.let { "$it members" },
                    onPick = { vm.importGroup("whatsapp", g.id, g.name, onBack) }
                )
            }
        }
        else -> QrPairingView(state.qr, polling = state.polling)
    }
}

@Composable
fun GroupPickRow(title: String, subtitle: String?, onPick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
        }
        Button(onClick = onPick) { Text("Import") }
    }
    HorizontalDivider(thickness = 0.5.dp)
}

@Composable
fun QrPairingView(qr: WhatsappQR?, polling: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Scan this QR with WhatsApp on your phone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        if (qr == null || qr.qrDataUrl.isNullOrBlank()) {
            CircularProgressIndicator()
        } else {
            val bmp = remember(qr.qrDataUrl) { decodeDataUrl(qr.qrDataUrl) }
            if (bmp != null) {
                Image(bitmap = bmp, contentDescription = "WhatsApp QR", modifier = Modifier.size(260.dp))
            } else {
                Text(qr.qrDataUrl, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (polling) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Waiting for pairing…")
            }
        }
    }
}

private fun decodeDataUrl(dataUrl: String): ImageBitmap? = runCatching {
    val base64 = dataUrl.substringAfter("base64,", "")
    if (base64.isBlank()) return null
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()
