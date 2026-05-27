package com.kontakti.data.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var output: File? = null
    var isRecording: Boolean = false
        private set

    fun start(): File {
        stop() // ensure clean state
        val outFile = File(context.cacheDir, "voice-${System.currentTimeMillis()}.m4a")
        @Suppress("DEPRECATION")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        mr.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outFile.absolutePath)
            prepare()
            start()
        }
        recorder = mr
        output = outFile
        isRecording = true
        return outFile
    }

    fun stop(): File? {
        val mr = recorder ?: run { isRecording = false; return null }
        return try {
            mr.stop()
            mr.release()
            recorder = null
            isRecording = false
            output
        } catch (_: Exception) {
            runCatching { mr.release() }
            recorder = null
            isRecording = false
            null
        }
    }

    fun cancel() {
        val mr = recorder ?: return
        runCatching { mr.stop() }
        runCatching { mr.release() }
        recorder = null
        isRecording = false
        output?.delete()
        output = null
    }
}
