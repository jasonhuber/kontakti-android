package com.kontakti.ui.components

import android.content.ClipDescription
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kontakti.data.model.PersonPhoto
import com.kontakti.data.repository.PhotoRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

/**
 * The API returns photo URLs in two shapes: full absolute URLs (LinkedIn CDN,
 * etc.) and storage-relative paths starting with `/photos/`. The relative ones
 * need to be prefixed with the API host so Coil can fetch them. The base URL
 * lives in [com.kontakti.di.AppModule] / [com.kontakti.data.network.ApiClient]
 * — both use `https://kontakti.app/api/v1/`, so the asset host is just
 * `https://kontakti.app`.
 */
private const val ASSET_HOST = "https://kontakti.app"

internal fun PersonPhoto.absoluteUrl(): String =
    if (url.startsWith("http://") || url.startsWith("https://")) url
    else ASSET_HOST + (if (url.startsWith("/")) url else "/$url")

/**
 * Hilt EntryPoint so an embedded composable can pull the singleton
 * [PhotoRepository] without owning a `@HiltViewModel` (which would require
 * passing personId through SavedStateHandle, awkward for non-route VMs).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PhotoRepoEntryPoint {
    fun photoRepository(): PhotoRepository
}

private data class PhotoGalleryUiState(
    val photos: List<PersonPhoto> = emptyList(),
    val uploading: Boolean = false,
    val error: String? = null
)

private fun sortPrimaryFirst(list: List<PersonPhoto>): List<PersonPhoto> =
    list.sortedWith(compareByDescending<PersonPhoto> { it.isPrimary }.thenBy { it.sortOrder })

private fun filenameFor(mime: String): String = when {
    mime.contains("png") -> "photo.png"
    mime.contains("webp") -> "photo.webp"
    mime.contains("gif") -> "photo.gif"
    else -> "photo.jpg"
}

/**
 * Horizontal strip of square photo tiles for a Person.
 *
 *  - Read-only mode: just renders the tiles.
 *  - Editable mode: trailing "+" tile launches the system photo picker
 *    (multi-select via [ActivityResultContracts.PickMultipleVisualMedia]),
 *    each tile long-press opens a "Make primary / Delete" menu, and a
 *    "Paste image" button sits below the strip.
 *
 * The photo picker is permission-less — Android grants read access to the
 * SAF URIs it returns. The clipboard read is also permission-less; on
 * Android 12+ the OS shows a one-shot toast when an app reads the clipboard,
 * but only if the user explicitly tapped the paste button so this is fine.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGallery(
    personId: String,
    editable: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PhotoRepoEntryPoint::class.java
        ).photoRepository()
    }
    val scope = rememberCoroutineScope()

    var ui by remember(personId) { mutableStateOf(PhotoGalleryUiState()) }

    LaunchedEffect(personId) {
        ui = ui.copy(error = null)
        runCatching { repo.list(personId) }
            .onSuccess { ui = ui.copy(photos = sortPrimaryFirst(it)) }
            .onFailure { ui = ui.copy(error = it.message) }
    }

    fun uploadBytes(bytes: ByteArray, mimeType: String, source: String) {
        ui = ui.copy(uploading = true, error = null)
        scope.launch {
            runCatching {
                repo.uploadBytes(personId, bytes, mimeType, filenameFor(mimeType), source)
            }.onSuccess { newPhoto ->
                ui = ui.copy(
                    photos = sortPrimaryFirst(ui.photos + newPhoto),
                    uploading = false
                )
            }.onFailure {
                ui = ui.copy(uploading = false, error = it.message)
            }
        }
    }

    fun setPrimary(photoId: String) {
        scope.launch {
            runCatching { repo.setPrimary(personId, photoId) }
                .onSuccess {
                    ui = ui.copy(
                        photos = sortPrimaryFirst(
                            ui.photos.map { p -> p.copy(isPrimary = p.id == photoId) }
                        )
                    )
                }
                .onFailure { ui = ui.copy(error = it.message) }
        }
    }

    fun delete(photoId: String) {
        val previous = ui.photos
        ui = ui.copy(photos = previous.filterNot { it.id == photoId })
        scope.launch {
            runCatching { repo.delete(personId, photoId) }
                .onFailure { ui = ui.copy(photos = previous, error = it.message) }
        }
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val (bytes, mime) = readUriBytes(context, uri) ?: return@forEach
            uploadBytes(bytes, mime, source = "manual_upload")
        }
    }

    val photos = ui.photos

    Column(modifier) {
        if (photos.isEmpty() && !editable) {
            // Nothing to show in read-only mode.
            return@Column
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(photos, key = { it.id }) { photo ->
                PhotoTile(
                    photo = photo,
                    editable = editable,
                    onMakePrimary = { setPrimary(photo.id) },
                    onDelete = { delete(photo.id) }
                )
            }
            if (editable) {
                item("add") {
                    AddTile(
                        uploading = ui.uploading,
                        onClick = {
                            pickMedia.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    )
                }
            }
        }

        if (editable) {
            var pasteFeedback by remember { mutableStateOf<String?>(null) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        pasteFeedback = null
                        val pasted = readClipboardImage(context)
                        if (pasted != null) {
                            uploadBytes(pasted.first, pasted.second, source = "paste")
                        } else {
                            pasteFeedback = "No image in clipboard."
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Paste image")
                }
                if (pasteFeedback != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        pasteFeedback!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        ui.error?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { ui = ui.copy(error = null) }) { Text("Dismiss") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoTile(
    photo: PersonPhoto,
    editable: Boolean,
    onMakePrimary: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (photo.isPrimary)
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(10.dp)
                        )
                    else Modifier
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = { if (editable) menuOpen = true }
                )
        ) {
            AsyncImage(
                model = photo.absoluteUrl(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (photo.isPrimary) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Primary photo",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (!photo.isPrimary) {
                DropdownMenuItem(
                    text = { Text("Make primary") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    onClick = { menuOpen = false; onMakePrimary() }
                )
            }
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = { menuOpen = false; onDelete() }
            )
        }
    }
}

@Composable
private fun AddTile(uploading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (uploading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add photo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Add",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Reads the bytes behind a content / file URI. Returns the raw bytes paired
 * with the resolver's reported MIME type (defaults to `image/jpeg`).
 */
private fun readUriBytes(context: Context, uri: Uri): Pair<ByteArray, String>? {
    return runCatching {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null
        bytes to mime
    }.getOrNull()
}

/**
 * Returns the first image found in the primary clipboard, or null.
 *
 * Android's clipboard exposes images as content URIs alongside their MIME
 * type. Screenshot apps, share sheets, Gboard's image picker, and "copy
 * image" gestures all produce this shape. We read the first item only — if
 * someone has copied multiple images we treat it as the most recent one.
 */
private fun readClipboardImage(context: Context): Pair<ByteArray, String>? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        ?: return null
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val desc: ClipDescription = clip.description
    val hasImage = (0 until desc.mimeTypeCount).any { desc.getMimeType(it).startsWith("image/") }
    if (!hasImage) return null
    val item = clip.getItemAt(0) ?: return null
    val uri = item.uri ?: return null
    return readUriBytes(context, uri)
}
