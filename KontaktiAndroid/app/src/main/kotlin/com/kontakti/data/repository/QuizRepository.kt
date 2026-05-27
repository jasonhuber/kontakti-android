package com.kontakti.data.repository

import com.kontakti.data.model.AnswerQuizRequest
import com.kontakti.data.model.AnswerQuizResponse
import com.kontakti.data.model.ContactPrompt
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository @Inject constructor(private val api: ApiService) {

    suspend fun answer(
        promptId: String,
        answer: String,
        structured: Map<String, Any?>? = null
    ): AnswerQuizResponse = api.answerQuiz(promptId, AnswerQuizRequest(answer, structured))

    suspend fun skip(promptId: String) {
        api.skipQuiz(promptId)
    }

    suspend fun history(): List<ContactPrompt> = api.quizHistory()

    suspend fun historyForPerson(personId: String): List<ContactPrompt> =
        runCatching { history().filter { it.person.id == personId } }.getOrDefault(emptyList())
}
