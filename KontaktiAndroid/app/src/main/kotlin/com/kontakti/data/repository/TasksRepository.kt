package com.kontakti.data.repository

import com.kontakti.data.model.CreateTaskRequest
import com.kontakti.data.model.Task
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksRepository @Inject constructor(private val api: ApiService) {
    suspend fun listForPerson(personId: String): List<Task> = api.getPersonTasks(personId)
    suspend fun create(personId: String, title: String, dueAt: String? = null, priority: String? = null): Task =
        api.createTask(CreateTaskRequest(title = title, dueAt = dueAt, priority = priority, taskableId = personId))
    suspend fun complete(id: String): Task = api.completeTask(id)
}
