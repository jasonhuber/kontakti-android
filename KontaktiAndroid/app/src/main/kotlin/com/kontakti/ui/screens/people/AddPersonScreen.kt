package com.kontakti.ui.screens.people

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.CreatePersonRequest
import com.kontakti.data.model.PersonEmail
import com.kontakti.data.model.PersonPhone
import com.kontakti.data.repository.PeopleRepository
import com.kontakti.ui.components.SectionHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddPersonViewModel @Inject constructor(private val repo: PeopleRepository) : ViewModel() {
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun create(req: CreatePersonRequest, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            val r = runCatching { repo.createPerson(req) }
            _saving.value = false
            onDone(r.getOrNull()?.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    vm: AddPersonViewModel = hiltViewModel()
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var emails by remember { mutableStateOf<List<PersonEmail>>(emptyList()) }
    var phones by remember { mutableStateOf<List<PersonPhone>>(emptyList()) }
    var company by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var linkedin by remember { mutableStateOf("") }
    val saving by vm.saving.collectAsState()

    fun submit() {
        // Strip empty rows, normalise primary, mirror primary into legacy fields.
        fun <T> normalisePrimary(
            arr: List<T>, isPrimary: (T) -> Boolean, setPrimary: (T, Boolean) -> T
        ): List<T> {
            if (arr.isEmpty()) return arr
            val firstFlagged = arr.indexOfFirst(isPrimary)
            val keep = if (firstFlagged == -1) 0 else firstFlagged
            return arr.mapIndexed { i, x -> setPrimary(x, i == keep) }
        }
        val cleanEmails = emails.filter { it.value.trim().isNotEmpty() }
            .map { it.copy(value = it.value.trim()) }
        val cleanPhones = phones.filter { it.value.trim().isNotEmpty() }
            .map { it.copy(value = it.value.trim()) }
        val finalEmails = normalisePrimary(
            cleanEmails, { it.isPrimary }, { e, p -> e.copy(isPrimary = p) }
        )
        val finalPhones = normalisePrimary(
            cleanPhones, { it.isPrimary }, { ph, p -> ph.copy(isPrimary = p) }
        )
        val primaryEmail = finalEmails.firstOrNull { it.isPrimary }?.value
        val primaryPhone = finalPhones.firstOrNull { it.isPrimary }?.value

        vm.create(
            CreatePersonRequest(
                firstName = firstName.ifBlank { null },
                lastName = lastName.ifBlank { null },
                email = primaryEmail,
                phone = primaryPhone,
                emails = finalEmails.ifEmpty { null },
                phones = finalPhones.ifEmpty { null },
                companyName = company.ifBlank { null },
                title = title.ifBlank { null },
                linkedinUrl = linkedin.ifBlank { null },
                notes = notes.ifBlank { null }
            )
        ) { id -> id?.let(onCreated) ?: onBack() }
    }

    val canSave = !saving && (
        firstName.isNotBlank() ||
        lastName.isNotBlank() ||
        emails.any { it.value.isNotBlank() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add person") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    TextButton(enabled = canSave, onClick = ::submit) {
                        Text(if (saving) "Saving…" else "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            OutlinedTextField(firstName, { firstName = it }, label = { Text("First name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(lastName, { lastName = it }, label = { Text("Last name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            SectionHeader("Emails")
            emails.forEachIndexed { idx, e ->
                ContactEntryRow(
                    value = e.value,
                    label = e.label ?: "personal",
                    labelOptions = EMAIL_LABELS,
                    isPrimary = e.isPrimary,
                    placeholder = "email@example.com",
                    onValue = { v -> emails = emails.toMutableList().also { it[idx] = e.copy(value = v) } },
                    onLabel = { l -> emails = emails.toMutableList().also { it[idx] = e.copy(label = l) } },
                    onPrimary = { emails = emails.mapIndexed { i, em -> em.copy(isPrimary = i == idx) } },
                    onRemove = { emails = emails.toMutableList().also { it.removeAt(idx) } }
                )
            }
            TextButton(onClick = { emails = emails + PersonEmail(value = "", label = "personal", isPrimary = emails.isEmpty()) }) {
                Icon(Icons.Default.Add, contentDescription = null); Text(" Add email")
            }

            SectionHeader("Phones")
            phones.forEachIndexed { idx, ph ->
                ContactEntryRow(
                    value = ph.value,
                    label = ph.label ?: "mobile",
                    labelOptions = PHONE_LABELS,
                    isPrimary = ph.isPrimary,
                    placeholder = "+1 555 0123",
                    onValue = { v -> phones = phones.toMutableList().also { it[idx] = ph.copy(value = v) } },
                    onLabel = { l -> phones = phones.toMutableList().also { it[idx] = ph.copy(label = l) } },
                    onPrimary = { phones = phones.mapIndexed { i, em -> em.copy(isPrimary = i == idx) } },
                    onRemove = { phones = phones.toMutableList().also { it.removeAt(idx) } }
                )
            }
            TextButton(onClick = { phones = phones + PersonPhone(value = "", label = "mobile", isPrimary = phones.isEmpty()) }) {
                Icon(Icons.Default.Add, contentDescription = null); Text(" Add phone")
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(company, { company = it }, label = { Text("Company") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(linkedin, { linkedin = it }, label = { Text("LinkedIn URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), minLines = 3)
            Spacer(Modifier.height(40.dp))
        }
    }
}
