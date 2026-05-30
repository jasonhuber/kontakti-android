package com.kontakti.ui.screens.people

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.*
import com.kontakti.data.repository.PeopleRepository
import com.kontakti.ui.components.PhotoGallery
import com.kontakti.ui.components.SectionHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal val EMAIL_LABELS = listOf("work", "home", "personal", "other")
internal val PHONE_LABELS = listOf("mobile", "work", "home", "other")

/** Seed editor email rows from a Person, merging the legacy single-column
 *  `email` if not already present (deduped by lowercased value). Mirrors the
 *  web's `seedEmails` in EditPersonModal.tsx. */
private fun seedEmails(p: Person): List<PersonEmail> {
    val out = mutableListOf<PersonEmail>()
    val seen = mutableSetOf<String>()
    for (e in p.emails) {
        val k = e.value.trim().lowercase()
        if (k.isEmpty() || k in seen) continue
        seen += k
        out += e
    }
    p.email?.let {
        val k = it.trim().lowercase()
        if (k.isNotEmpty() && k !in seen) {
            out += PersonEmail(value = it, label = "personal", isPrimary = out.isEmpty())
        }
    }
    return out
}

private fun seedPhones(p: Person): List<PersonPhone> {
    val out = mutableListOf<PersonPhone>()
    val seen = mutableSetOf<String>()
    fun norm(s: String): String {
        val d = s.filter { it.isDigit() }
        return if (d.length == 11 && d.startsWith("1")) d.drop(1) else d
    }
    for (ph in p.phones) {
        val k = norm(ph.value)
        if (k.isEmpty() || k in seen) continue
        seen += k
        out += ph
    }
    p.phone?.let {
        val k = norm(it)
        if (k.isNotEmpty() && k !in seen) {
            out += PersonPhone(value = it, label = "mobile", isPrimary = out.isEmpty())
        }
    }
    return out
}

@HiltViewModel
class PersonEditViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val repo: PeopleRepository
) : ViewModel() {
    val personId: String = checkNotNull(saved["personId"])

    private val _person = MutableStateFlow<Person?>(null)
    val person: StateFlow<Person?> = _person.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    init {
        viewModelScope.launch {
            _person.value = runCatching { repo.getPerson(personId) }.getOrNull()
        }
    }

    fun save(patch: PersonPatch, onDone: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            runCatching { repo.updatePerson(personId, patch) }
            _saving.value = false
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditScreen(
    personId: String,
    onBack: () -> Unit,
    vm: PersonEditViewModel = hiltViewModel()
) {
    val person by vm.person.collectAsState()
    val saving by vm.saving.collectAsState()

    if (person == null) {
        Scaffold(topBar = { TopAppBar(title = { Text("Edit") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }) { p ->
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        return
    }
    val p = person!!

    var firstName by remember { mutableStateOf(p.firstName) }
    var lastName by remember { mutableStateOf(p.lastName) }
    var nickname by remember { mutableStateOf(p.nickname.orEmpty()) }
    var title by remember { mutableStateOf(p.title.orEmpty()) }
    var jobDept by remember { mutableStateOf(p.jobDepartment.orEmpty()) }
    var companyName by remember { mutableStateOf(p.company?.name.orEmpty()) }
    var birthday by remember { mutableStateOf(p.birthday.orEmpty()) }
    var notes by remember { mutableStateOf(p.notes.orEmpty()) }
    var strength by remember { mutableStateOf(p.relationshipStrength) }

    var cadence by remember { mutableStateOf(p.contactCadence ?: "biannual") }
    var onBirthday by remember { mutableStateOf(p.contactOnBirthday ?: true) }
    var onHolidays by remember { mutableStateOf(p.contactOnHolidays ?: false) }

    var linkedinUrl by remember { mutableStateOf(p.linkedinUrl.orEmpty()) }
    var facebookUrl by remember { mutableStateOf(p.facebookUrl.orEmpty()) }
    var instagram by remember { mutableStateOf(p.instagramHandle.orEmpty()) }
    var twitter by remember { mutableStateOf(p.twitterXHandle.orEmpty()) }
    var tiktok by remember { mutableStateOf(p.tiktokHandle.orEmpty()) }
    var whatsapp by remember { mutableStateOf(p.whatsappPhone.orEmpty()) }

    var city by remember { mutableStateOf(p.city.orEmpty()) }
    var region by remember { mutableStateOf(p.region.orEmpty()) }
    var country by remember { mutableStateOf(p.country.orEmpty()) }
    var howWeMet by remember { mutableStateOf(p.howWeMet.orEmpty()) }

    var emails by remember { mutableStateOf(seedEmails(p)) }
    var phones by remember { mutableStateOf(seedPhones(p)) }
    var previousEmployers by remember { mutableStateOf(p.previousEmployers) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit ${p.firstName}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    TextButton(
                        enabled = !saving,
                        onClick = {
                            // Strip empty value rows, normalise primary flag so
                            // exactly one (or zero) entries per side are flagged.
                            // The server uses replace-list semantics so we always
                            // send the full intended state.
                            fun <T> normalisePrimary(
                                arr: List<T>,
                                isPrimary: (T) -> Boolean,
                                setPrimary: (T, Boolean) -> T
                            ): List<T> {
                                if (arr.isEmpty()) return arr
                                val firstFlagged = arr.indexOfFirst(isPrimary)
                                val keep = if (firstFlagged == -1) 0 else firstFlagged
                                return arr.mapIndexed { i, x -> setPrimary(x, i == keep) }
                            }
                            val cleanedEmails = emails
                                .filter { it.value.trim().isNotEmpty() }
                                .map { it.copy(value = it.value.trim()) }
                            val cleanedPhones = phones
                                .filter { it.value.trim().isNotEmpty() }
                                .map { it.copy(value = it.value.trim()) }
                            val finalEmails = normalisePrimary(
                                cleanedEmails,
                                { it.isPrimary },
                                { e, primary -> e.copy(isPrimary = primary) }
                            )
                            val finalPhones = normalisePrimary(
                                cleanedPhones,
                                { it.isPrimary },
                                { ph, primary -> ph.copy(isPrimary = primary) }
                            )
                            // Mirror the primary into the legacy single-column
                            // fields so list views, search, and any code still
                            // reading person.email / person.phone keep working.
                            val primaryEmail = finalEmails.firstOrNull { it.isPrimary }?.value.orEmpty()
                            val primaryPhone = finalPhones.firstOrNull { it.isPrimary }?.value.orEmpty()

                            val patch = PersonPatch(
                                firstName = firstName.takeIf { it != p.firstName },
                                lastName = lastName.takeIf { it != p.lastName },
                                nickname = nickname.takeIf { it != p.nickname.orEmpty() },
                                title = title.takeIf { it != p.title.orEmpty() },
                                jobDepartment = jobDept.takeIf { it != p.jobDepartment.orEmpty() },
                                companyName = companyName.takeIf { it != p.company?.name.orEmpty() },
                                birthday = birthday.takeIf { it != p.birthday.orEmpty() },
                                linkedinUrl = linkedinUrl.takeIf { it != p.linkedinUrl.orEmpty() },
                                facebookUrl = facebookUrl.takeIf { it != p.facebookUrl.orEmpty() },
                                instagramHandle = instagram.takeIf { it != p.instagramHandle.orEmpty() },
                                twitterXHandle = twitter.takeIf { it != p.twitterXHandle.orEmpty() },
                                tiktokHandle = tiktok.takeIf { it != p.tiktokHandle.orEmpty() },
                                whatsappPhone = whatsapp.takeIf { it != p.whatsappPhone.orEmpty() },
                                city = city.takeIf { it != p.city.orEmpty() },
                                region = region.takeIf { it != p.region.orEmpty() },
                                country = country.takeIf { it != p.country.orEmpty() },
                                howWeMet = howWeMet.takeIf { it != p.howWeMet.orEmpty() },
                                relationshipStrength = strength.name.takeIf { strength != p.relationshipStrength },
                                contactCadence = cadence,
                                contactOnBirthday = onBirthday,
                                contactOnHolidays = onHolidays,
                                notes = notes.takeIf { it != p.notes.orEmpty() },
                                email = primaryEmail.takeIf { it != p.email.orEmpty() },
                                phone = primaryPhone.takeIf { it != p.phone.orEmpty() },
                                emails = finalEmails,
                                phones = finalPhones,
                                previousEmployers = previousEmployers
                            )
                            vm.save(patch, onBack)
                        }
                    ) { Text(if (saving) "Saving…" else "Save") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader("Photos")
            PhotoGallery(personId = personId, editable = true)

            SectionHeader("Basics")
            FieldRow("First name", firstName) { firstName = it }
            FieldRow("Last name", lastName) { lastName = it }
            FieldRow("Nickname", nickname) { nickname = it }
            FieldRow("Title", title) { title = it }
            FieldRow("Department", jobDept) { jobDept = it }
            FieldRow("Company", companyName) { companyName = it }
            FieldRow("Birthday (YYYY-MM-DD)", birthday) { birthday = it }

            SectionHeader("Relationship")
            Row(Modifier.padding(horizontal = 16.dp)) {
                RelationshipStrength.entries.forEach { s ->
                    FilterChip(
                        selected = s == strength,
                        onClick = { strength = s },
                        label = { Text(s.label) }
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }

            SectionHeader("Stay in touch")
            Row(Modifier.padding(horizontal = 16.dp)) {
                listOf("monthly" to "1mo", "quarterly" to "3mo", "biannual" to "6mo", "annual" to "1yr", "none" to "Off").forEach { (value, lbl) ->
                    FilterChip(
                        selected = cadence == value,
                        onClick = { cadence = value },
                        label = { Text(lbl) }
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Remind on birthday", Modifier.weight(1f))
                Switch(checked = onBirthday, onCheckedChange = { onBirthday = it })
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Remind around holidays", Modifier.weight(1f))
                Switch(checked = onHolidays, onCheckedChange = { onHolidays = it })
            }

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
            TextButton(onClick = { emails = emails + PersonEmail(value = "", label = "personal", isPrimary = emails.isEmpty()) }, modifier = Modifier.padding(horizontal = 12.dp)) {
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
            TextButton(onClick = { phones = phones + PersonPhone(value = "", label = "mobile", isPrimary = phones.isEmpty()) }, modifier = Modifier.padding(horizontal = 12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null); Text(" Add phone")
            }

            SectionHeader("Social")
            FieldRow("LinkedIn URL", linkedinUrl) { linkedinUrl = it }
            FieldRow("Facebook URL", facebookUrl) { facebookUrl = it }
            FieldRow("Instagram handle", instagram) { instagram = it }
            FieldRow("X (Twitter) handle", twitter) { twitter = it }
            FieldRow("TikTok handle", tiktok) { tiktok = it }
            FieldRow("WhatsApp phone", whatsapp) { whatsapp = it }

            SectionHeader("Location")
            FieldRow("City", city) { city = it }
            FieldRow("Region", region) { region = it }
            FieldRow("Country", country) { country = it }
            FieldRow("How we met", howWeMet) { howWeMet = it }

            SectionHeader("Career history")
            previousEmployers.forEachIndexed { idx, emp ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = emp, onValueChange = { v -> previousEmployers = previousEmployers.toMutableList().also { it[idx] = v } }, modifier = Modifier.weight(1f), singleLine = true)
                    IconButton(onClick = { previousEmployers = previousEmployers.toMutableList().also { it.removeAt(idx) } }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }
            TextButton(onClick = { previousEmployers = previousEmployers + "" }, modifier = Modifier.padding(horizontal = 12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null); Text(" Add employer")
            }

            SectionHeader("Notes")
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(min = 120.dp),
                minLines = 4
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

/** A repeating row for an email / phone: value field, label dropdown, star
 *  toggle for primary, and a trash button. Shared with the AddPerson screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactEntryRow(
    value: String,
    label: String,
    labelOptions: List<String>,
    isPrimary: Boolean,
    placeholder: String,
    onValue: (String) -> Unit,
    onLabel: (String) -> Unit,
    onPrimary: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            placeholder = { Text(placeholder) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))

        // Label dropdown
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(120.dp)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                labelOptions.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = { onLabel(opt); expanded = false }
                    )
                }
            }
        }

        // Primary star toggle
        IconToggleButton(checked = isPrimary, onCheckedChange = { if (!isPrimary) onPrimary() }) {
            Icon(
                imageVector = if (isPrimary) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isPrimary) "Primary" else "Make primary",
                tint = if (isPrimary) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
