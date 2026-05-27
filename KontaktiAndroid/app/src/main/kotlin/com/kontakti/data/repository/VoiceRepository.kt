package com.kontakti.data.repository

import com.kontakti.data.model.VoiceCaptureResult
import com.kontakti.data.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepository @Inject constructor(private val api: ApiService) {
    suspend fun capture(
        file: File,
        personId: String? = null,
        context: String? = null
    ): VoiceCaptureResult {
        val body = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("audio", file.name, body)
        val plain = "text/plain".toMediaTypeOrNull()
        val pid = personId?.toRequestBody(plain)
        val ctx = context?.toRequestBody(plain)
        return api.captureVoice(part, pid, ctx)
    }
}
