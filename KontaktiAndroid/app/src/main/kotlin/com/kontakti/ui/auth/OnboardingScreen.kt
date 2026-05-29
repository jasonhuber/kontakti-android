package com.kontakti.ui.auth

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.contacts.BulkImportRequest
import com.kontakti.data.contacts.DeviceContactsImporter
import com.kontakti.data.contacts.ImportCandidate
import com.kontakti.data.google.GoogleAuthManager
import com.kontakti.data.google.GoogleContactsService
import com.kontakti.data.network.ApiService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Indigo = Color(0xFF4F46E5)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val deviceImporter: DeviceContactsImporter,
    private val googleService: GoogleContactsService,
    private val api: ApiService
) : ViewModel() {

    sealed class StepStatus {
        object Idle : StepStatus()
        object Loading : StepStatus()
        data class Ready(val candidates: List<ImportCandidate>) : StepStatus()
        data class Done(val count: Int) : StepStatus()
        data class Failed(val message: String) : StepStatus()
    }

    private val _phoneStatus = MutableStateFlow<StepStatus>(StepStatus.Idle)
    val phoneStatus: StateFlow<StepStatus> = _phoneStatus.asStateFlow()

    private val _googleStatus = MutableStateFlow<StepStatus>(StepStatus.Idle)
    val googleStatus: StateFlow<StepStatus> = _googleStatus.asStateFlow()

    private val _phoneImported = MutableStateFlow(0)
    val phoneImported: StateFlow<Int> = _phoneImported.asStateFlow()

    private val _googleImported = MutableStateFlow(0)
    val googleImported: StateFlow<Int> = _googleImported.asStateFlow()

    fun loadPhoneCandidates() {
        _phoneStatus.value = StepStatus.Loading
        viewModelScope.launch {
            runCatching { deviceImporter.fetchNewCandidates() }
                .onSuccess { _phoneStatus.value = StepStatus.Ready(it) }
                .onFailure { _phoneStatus.value = StepStatus.Failed(it.message ?: "Could not read contacts.") }
        }
    }

    fun importPhoneCandidates() {
        val current = _phoneStatus.value
        if (current !is StepStatus.Ready) return
        viewModelScope.launch {
            runCatching { api.importContacts(BulkImportRequest(contacts = current.candidates)) }
                .onSuccess {
                    _phoneImported.value = it.imported
                    _phoneStatus.value = StepStatus.Done(it.imported)
                }
                .onFailure { _phoneStatus.value = StepStatus.Failed(it.message ?: "Import failed.") }
        }
    }

    fun fetchGoogleCandidates(accessToken: String) {
        _googleStatus.value = StepStatus.Loading
        viewModelScope.launch {
            runCatching { googleService.fetchCandidates(accessToken) }
                .onSuccess { _googleStatus.value = StepStatus.Ready(it) }
                .onFailure { _googleStatus.value = StepStatus.Failed(it.message ?: "Could not fetch Google contacts.") }
        }
    }

    fun importGoogleCandidates() {
        val current = _googleStatus.value
        if (current !is StepStatus.Ready) return
        viewModelScope.launch {
            runCatching { api.importContacts(BulkImportRequest(contacts = current.candidates)) }
                .onSuccess {
                    _googleImported.value = it.imported
                    _googleStatus.value = StepStatus.Done(it.imported)
                }
                .onFailure { _googleStatus.value = StepStatus.Failed(it.message ?: "Import failed.") }
        }
    }
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (step) {
            0 -> WelcomeStep(
                onStart = { step = 1 },
                onSkip = onComplete
            )
            1 -> PhoneStep(
                vm = vm,
                onDone = { step = 2 },
                onSkip = { step = 2 }
            )
            2 -> GoogleStep(
                vm = vm,
                onDone = { step = 3 },
                onSkip = { step = 3 }
            )
            else -> DoneStep(
                phoneCount = vm.phoneImported.collectAsState().value,
                googleCount = vm.googleImported.collectAsState().value,
                onFinish = onComplete
            )
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit, onSkip: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("K", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Welcome to Kontakti",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Let's seed your network with real contacts so you're not starting from a blank page.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = Indigo),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Get started", fontWeight = FontWeight.SemiBold) }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Skip for now") }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun PhoneStep(
    vm: OnboardingViewModel,
    onDone: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val status by vm.phoneStatus.collectAsState()
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) vm.loadPhoneCandidates()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && status is OnboardingViewModel.StepStatus.Idle) {
            vm.loadPhoneCandidates()
        }
    }

    StepShell(
        icon = Icons.Default.PhoneAndroid,
        title = "Import from phone",
        subtitle = "We'll scan your contacts for people not yet in Kontakti.",
        onSkip = onSkip,
        primaryAction = {
            when (val s = status) {
                is OnboardingViewModel.StepStatus.Done -> PrimaryButton("Next") { onDone() }
                is OnboardingViewModel.StepStatus.Ready -> {
                    if (s.candidates.isEmpty()) PrimaryButton("Continue") { onDone() }
                    else PrimaryButton("Import ${s.candidates.size} contacts") {
                        vm.importPhoneCandidates()
                    }
                }
                is OnboardingViewModel.StepStatus.Idle -> {
                    if (!hasPermission) {
                        PrimaryButton("Allow contacts access") {
                            permLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }
                }
                else -> {}
            }
        }
    ) {
        when (val s = status) {
            OnboardingViewModel.StepStatus.Idle ->
                if (!hasPermission) Text("Kontakti needs permission to read your contacts.")
                else SpinnerText("Scanning contacts…")
            OnboardingViewModel.StepStatus.Loading -> SpinnerText("Scanning contacts…")
            is OnboardingViewModel.StepStatus.Ready ->
                if (s.candidates.isEmpty()) EmptyText("All your phone contacts are already in Kontakti.")
                else CandidatePreview(s.candidates)
            is OnboardingViewModel.StepStatus.Done -> SuccessText(s.count, "phone")
            is OnboardingViewModel.StepStatus.Failed -> ErrorText(s.message)
        }
    }
}

@Composable
private fun GoogleStep(
    vm: OnboardingViewModel,
    onDone: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by vm.googleStatus.collectAsState()

    val authManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            OnboardingEntryPoint::class.java
        ).googleAuthManager()
    }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            val token = authManager.handleSignInResult(result.data)
            if (token != null) {
                vm.fetchGoogleCandidates(token)
            }
        }
    }

    StepShell(
        icon = Icons.Default.Email,
        title = "Import from Google",
        subtitle = "Pull in Google Contacts and frequent Gmail senders.",
        onSkip = onSkip,
        primaryAction = {
            when (val s = status) {
                is OnboardingViewModel.StepStatus.Done -> PrimaryButton("Next") { onDone() }
                is OnboardingViewModel.StepStatus.Ready -> {
                    if (s.candidates.isEmpty()) PrimaryButton("Continue") { onDone() }
                    else PrimaryButton("Import ${s.candidates.size} contacts") {
                        vm.importGoogleCandidates()
                    }
                }
                OnboardingViewModel.StepStatus.Idle, is OnboardingViewModel.StepStatus.Failed ->
                    PrimaryButton("Connect Google Contacts") {
                        signInLauncher.launch(authManager.getSignInIntent())
                    }
                OnboardingViewModel.StepStatus.Loading -> {}
            }
        }
    ) {
        when (val s = status) {
            OnboardingViewModel.StepStatus.Idle -> Text(
                "Tap below to grant Kontakti read access to your Google contacts and Gmail senders.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            OnboardingViewModel.StepStatus.Loading -> SpinnerText("Fetching contacts…")
            is OnboardingViewModel.StepStatus.Ready ->
                if (s.candidates.isEmpty()) EmptyText("All your Google contacts are already in Kontakti.")
                else CandidatePreview(s.candidates)
            is OnboardingViewModel.StepStatus.Done -> SuccessText(s.count, "Google")
            is OnboardingViewModel.StepStatus.Failed -> ErrorText(s.message)
        }
    }
}

@Composable
private fun DoneStep(phoneCount: Int, googleCount: Int, onFinish: () -> Unit) {
    val total = phoneCount + googleCount
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Color(0xFF22C55E).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("You're all set!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (total > 0) "$total contacts imported and ready to manage."
            else "You can import contacts anytime from the People tab.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = Indigo),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Open Kontakti", fontWeight = FontWeight.SemiBold) }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface OnboardingEntryPoint {
    fun googleAuthManager(): GoogleAuthManager
}

// ── Shared step layout helpers ──────────────────────────────────────────────

@Composable
private fun StepShell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onSkip: () -> Unit,
    primaryAction: @Composable () -> Unit,
    body: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = Indigo, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            body()
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            primaryAction()
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Skip") }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Indigo),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) { Text(label, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun SpinnerText(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun EmptyText(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun SuccessText(count: Int, source: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF22C55E),
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("$count contacts imported from $source", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
private fun CandidatePreview(candidates: List<ImportCandidate>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "${candidates.size} contacts found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                candidates.take(4).forEach { c ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(c.firstName.take(1).uppercase(), fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name)
                            c.email?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
                if (candidates.size > 4) {
                    Text(
                        "+ ${candidates.size - 4} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
