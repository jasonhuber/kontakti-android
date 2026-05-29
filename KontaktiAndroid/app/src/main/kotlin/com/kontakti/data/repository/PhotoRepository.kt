package com.kontakti.data.repository

import com.kontakti.data.model.PersonPhoto
import com.kontakti.data.model.PhotoUploadBody
import com.kontakti.data.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the multi-photo endpoints on a Person. Three upload paths exist
 * because the server accepts three body shapes:
 *  - multipart file (picker / paste of a content URI we already opened)
 *  - data URL (rarely used on Android, but kept for symmetry with web)
 *  - external URL pointer (e.g. LinkedIn CDN; server doesn't download it)
 */
@Singleton
class PhotoRepository @Inject constructor(private val api: ApiService) {
    suspend fun list(personId: String): List<PersonPhoto> = api.listPhotos(personId)

    /**
     * Uploads raw image bytes as multipart/form-data. [filename] should end
     * with the appropriate extension so the server can sniff the mime; if you
     * don't know it, leave the default — the [mimeType] header still wins.
     */
    suspend fun uploadBytes(
        personId: String,
        bytes: ByteArray,
        mimeType: String = "image/jpeg",
        filename: String = "photo.jpg",
        source: String = "manual_upload"
    ): PersonPhoto {
        val media = mimeType.toMediaTypeOrNull()
        val body: RequestBody = bytes.toRequestBody(media)
        val part = MultipartBody.Part.createFormData("file", filename, body)
        val sourceBody = source.toRequestBody("text/plain".toMediaTypeOrNull())
        return api.uploadPhotoMultipart(personId, part, sourceBody)
    }

    /** Uploads a `data:image/...;base64,...` URL (e.g. produced from a clipboard bitmap). */
    suspend fun uploadDataUrl(
        personId: String,
        dataUrl: String,
        source: String = "paste"
    ): PersonPhoto = api.uploadPhotoJson(
        personId,
        PhotoUploadBody(data = dataUrl, source = source)
    )

    /** Stores an external URL as a pointer (no download). */
    suspend fun uploadUrl(
        personId: String,
        url: String,
        source: String = "linkedin"
    ): PersonPhoto = api.uploadPhotoJson(
        personId,
        PhotoUploadBody(url = url, source = source)
    )

    suspend fun delete(personId: String, photoId: String) =
        api.deletePhoto(personId, photoId)

    suspend fun setPrimary(personId: String, photoId: String): PersonPhoto =
        api.setPrimaryPhoto(personId, photoId)
}
