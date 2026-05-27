package com.kontakti.data.repository

import com.kontakti.data.model.CreateNoteRequest
import com.kontakti.data.model.Note
import com.kontakti.data.model.UpdateNoteRequest
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor(private val api: ApiService) {
    suspend fun listForPerson(personId: String): List<Note> = api.listPersonNotes(personId)
    suspend fun create(personId: String, title: String?, body: String): Note =
        api.createNote(CreateNoteRequest(title, body, notableId = personId))
    suspend fun update(id: String, title: String? = null, body: String? = null): Note =
        api.updateNote(id, UpdateNoteRequest(title, body))
    suspend fun delete(id: String) = api.deleteNote(id)
}
