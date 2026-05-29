package com.kontakti.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontakti.data.model.HealthBucket
import com.kontakti.data.model.HealthSample
import com.kontakti.data.model.PeopleHealth
import com.kontakti.data.network.ApiService
import com.kontakti.ui.components.SectionHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val IndigoTint = Color(0xFF4F46E5)

private data class BucketEntry(val key: String, val label: String, val icon: ImageVector)

private val bucketOrder = listOf(
    BucketEntry("needs_review",         "Flagged on import",    Icons.Default.ErrorOutline),
    BucketEntry("missing_first_name",   "Missing first name",   Icons.AutoMirrored.Filled.HelpOutline),
    BucketEntry("missing_last_name",    "Missing last name",    Icons.AutoMirrored.Filled.HelpOutline),
    BucketEntry("missing_contact_info", "No email or phone",    Icons.Default.PersonOutline),
    BucketEntry("invalid_email",        "Suspect email",        Icons.Default.MailOutline),
    BucketEntry("duplicate_email",      "Duplicate emails",     Icons.Default.ContentCopy),
    BucketEntry("unlinked_company",     "Company not linked",   Icons.Default.Apartment),
    BucketEntry("imported_unreviewed",  "Imported, unreviewed", Icons.Default.MoveToInbox)
)

@HiltViewModel
class ReviewContactsViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _health = MutableStateFlow<PeopleHealth?>(null)
    val health: StateFlow<PeopleHealth?> = _health.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _reviewedIds = MutableStateFlow<Set<String>>(emptySet())
    val reviewedIds: StateFlow<Set<String>> = _reviewedIds.asStateFlow()

    private val _markingIds = MutableStateFlow<Set<String>>(emptySet())
    val markingIds: StateFlow<Set<String>> = _markingIds.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { api.getPeopleHealth() }
                .onSuccess { _health.value = it }
                .onFailure { _error.value = it.message ?: "Couldn't load review queue." }
            _loading.value = false
        }
    }

    fun markReviewed(id: String) {
        if (id in _reviewedIds.value || id in _markingIds.value) return
        viewModelScope.launch {
            _markingIds.value = _markingIds.value + id
            runCatching { api.markPersonReviewed(id) }
                .onSuccess { _reviewedIds.value = _reviewedIds.value + id }
            _markingIds.value = _markingIds.value - id
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewContactsScreen(
    onBack: () -> Unit,
    vm: ReviewContactsViewModel = hiltViewModel()
) {
    val health by vm.health.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    var openBucket by remember { mutableStateOf<BucketEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(openBucket?.label ?: "Review contacts") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (openBucket != null) openBucket = null else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val mod = Modifier.padding(padding).fillMaxSize()
        val current = openBucket
        when {
            loading && health == null -> Box(mod, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null && health == null -> Box(mod, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            health != null && current == null -> OverviewList(
                modifier = mod,
                health = health!!,
                onOpenBucket = { openBucket = it }
            )
            health != null && current != null -> {
                val bucket = health!!.buckets[current.key]
                if (bucket == null) {
                    openBucket = null
                } else {
                    BucketDetailList(
                        modifier = mod,
                        bucket = bucket,
                        reviewedIds = vm.reviewedIds.collectAsState().value,
                        markingIds = vm.markingIds.collectAsState().value,
                        onMarkReviewed = vm::markReviewed
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewList(
    modifier: Modifier,
    health: PeopleHealth,
    onOpenBucket: (BucketEntry) -> Unit
) {
    val visible = bucketOrder.mapNotNull { entry ->
        health.buckets[entry.key]?.takeIf { it.count > 0 }?.let { entry to it }
    }
    LazyColumn(modifier) {
        item { SectionHeader("Overview") }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total contacts", modifier = Modifier.weight(1f))
                Text(
                    health.total.toString(),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            HorizontalDivider()
        }

        item { SectionHeader("Needs cleanup") }
        if (visible.isEmpty()) {
            item {
                Text(
                    "Everything looks clean — no rows flagged.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        } else {
            items(visible, key = { it.first.key }) { (entry, bucket) ->
                BucketRow(
                    label = entry.label,
                    icon = entry.icon,
                    count = bucket.count,
                    onClick = { onOpenBucket(entry) }
                )
                HorizontalDivider()
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun BucketRow(
    label: String,
    icon: ImageVector,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(IndigoTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = IndigoTint)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f))
        Text(
            count.toString(),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}

@Composable
private fun BucketDetailList(
    modifier: Modifier,
    bucket: HealthBucket,
    reviewedIds: Set<String>,
    markingIds: Set<String>,
    onMarkReviewed: (String) -> Unit
) {
    LazyColumn(modifier) {
        item {
            Text(
                "${bucket.count} total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider()
        }

        item { SectionHeader("Sample") }
        if (bucket.samples.isEmpty()) {
            item {
                Text(
                    "No rows in this bucket.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        } else {
            items(bucket.samples, key = { it.id }) { sample ->
                SampleRow(
                    sample = sample,
                    reviewed = sample.id in reviewedIds,
                    marking = sample.id in markingIds,
                    onMark = { onMarkReviewed(sample.id) }
                )
                HorizontalDivider()
            }
        }

        if (bucket.count > bucket.samples.size) {
            item {
                Text(
                    "Showing ${bucket.samples.size} of ${bucket.count}. Open the People tab and filter by 'needs_review' to see the rest.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun SampleRow(
    sample: HealthSample,
    reviewed: Boolean,
    marking: Boolean,
    onMark: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(sample.displayName)
            val email = sample.email
            if (!email.isNullOrEmpty()) {
                Text(
                    email,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        when {
            reviewed -> Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Reviewed",
                tint = Color(0xFF16A34A)
            )
            marking -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            else -> TextButton(onClick = onMark) {
                Text("Reviewed", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
