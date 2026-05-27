package com.kontakti.ui.screens.voice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kontakti.data.model.VoiceCaptureResult
import com.kontakti.ui.components.SectionHeader

@Composable
fun VoiceResultReview(res: VoiceCaptureResult, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Transcript", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Text(res.transcript.orEmpty(), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }
        if (!res.summary.isNullOrBlank()) {
            SectionHeader("Summary")
            Text(res.summary)
        }
        if (res.personRefs.isNotEmpty()) {
            SectionHeader("Mentioned people")
            res.personRefs.forEach { p -> Text("• ${p.fullName}") }
        }
        if (res.discussions.isNotEmpty()) {
            SectionHeader("Discussions extracted")
            res.discussions.forEach { d -> Text("${d.type.emoji} ${d.title}") }
        }
        if (res.tasks.isNotEmpty()) {
            SectionHeader("Tasks extracted")
            res.tasks.forEach { t -> Text("• ${t.title}") }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
