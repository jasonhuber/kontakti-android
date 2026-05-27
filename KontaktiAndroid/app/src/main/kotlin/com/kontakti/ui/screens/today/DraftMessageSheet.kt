package com.kontakti.ui.screens.today

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kontakti.data.model.TodayItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftMessageSheet(
    item: TodayItem,
    text: String,
    loading: Boolean,
    onText: (String) -> Unit,
    onDismiss: () -> Unit,
    onSendVia: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp)) {
            Text("Draft message to ${item.person.firstName}", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedTextField(
                value = text,
                onValueChange = onText,
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                minLines = 5,
                placeholder = { Text("Write something thoughtful…") }
            )
            Spacer(Modifier.height(12.dp))
            Text("Send via", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                val phone = item.person.phone ?: item.person.phones.firstOrNull()?.value
                val email = item.person.email ?: item.person.emails.firstOrNull()?.value
                Button(
                    enabled = phone != null,
                    onClick = {
                        phone?.let {
                            val uri = Uri.parse("smsto:$it?body=${Uri.encode(text)}")
                            context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
                        }
                        onSendVia("sms")
                    }
                ) { Text("SMS") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    enabled = email != null,
                    onClick = {
                        email?.let {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$it")
                                putExtra(Intent.EXTRA_SUBJECT, "Hi")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(intent)
                        }
                        onSendVia("email")
                    }
                ) { Text("Email") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    enabled = phone != null,
                    onClick = {
                        phone?.let {
                            val cleaned = it.filter { c -> c.isDigit() || c == '+' }
                            val uri = Uri.parse("https://wa.me/$cleaned?text=${Uri.encode(text)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                        onSendVia("whatsapp")
                    }
                ) { Text("WhatsApp") }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onSendVia("logged") }) { Text("Just log as contacted") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
