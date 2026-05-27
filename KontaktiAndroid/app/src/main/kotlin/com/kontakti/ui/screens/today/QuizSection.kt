package com.kontakti.ui.screens.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kontakti.data.model.ContactPrompt
import com.kontakti.data.model.QuestionKey
import com.kontakti.ui.components.AvatarComposable

/**
 * Daily 5-person contact quiz, shown above the reach-out items on TodayScreen.
 * Renders a horizontal LazyRow of cards; each card animates out on answer/skip.
 * When fully empty, collapses with a small "thanks" message.
 */
@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun QuizSection(
    prompts: List<ContactPrompt>,
    answeredCount: Int,
    onAnswer: (id: String, answer: String, structured: Map<String, Any?>?) -> Unit,
    onSkip: (id: String) -> Unit
) {
    // If nothing to show and nothing answered this session, render nothing.
    val showThanks = prompts.isEmpty() && answeredCount > 0
    if (prompts.isEmpty() && !showThanks) return

    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Quick check",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            if (prompts.isNotEmpty()) {
                Text(
                    "${prompts.size} left",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        AnimatedContent(
            targetState = showThanks,
            label = "quiz-section",
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { thanks ->
            if (thanks) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Thanks — saved $answeredCount answer${if (answeredCount == 1) "" else "s"} today.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    items(prompts, key = { it.id }) { prompt ->
                        QuizCard(
                            prompt = prompt,
                            onAnswer = { answer, structured -> onAnswer(prompt.id, answer, structured) },
                            onSkip = { onSkip(prompt.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun QuizCard(
    prompt: ContactPrompt,
    onAnswer: (answer: String, structured: Map<String, Any?>?) -> Unit,
    onSkip: () -> Unit
) {
    var dismissed by remember(prompt.id) { mutableStateOf(false) }
    var textValue by remember(prompt.id) { mutableStateOf("") }

    val isFreeText = prompt.questionKey == QuestionKey.notable ||
        prompt.questionKey == QuestionKey.how_we_met ||
        prompt.questionKey == QuestionKey.other

    AnimatedVisibility(
        visible = !dismissed,
        exit = fadeOut() + shrinkHorizontally()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.width(300.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarComposable(prompt.person.fullName, size = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(prompt.person.fullName, fontWeight = FontWeight.SemiBold)
                        prompt.person.company?.name?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(prompt.questionText, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))

                if (prompt.suggestedResponses.isNotEmpty()) {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        prompt.suggestedResponses.forEach { suggestion ->
                            FilledTonalButton(
                                onClick = {
                                    dismissed = true
                                    onAnswer(suggestion, null)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(suggestion, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                if (isFreeText) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        placeholder = { Text("Type your answer…") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                val answer = textValue.trim()
                                if (answer.isNotEmpty()) {
                                    dismissed = true
                                    onAnswer(answer, null)
                                }
                            },
                            enabled = textValue.trim().isNotEmpty()
                        ) { Text("Save") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            dismissed = true
                            onSkip()
                        }) { Text("Skip") }
                    }
                } else {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = {
                        dismissed = true
                        onSkip()
                    }) { Text("Skip") }
                }
            }
        }
    }
}
