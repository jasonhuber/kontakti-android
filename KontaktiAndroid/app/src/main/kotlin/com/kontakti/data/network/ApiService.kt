package com.kontakti.data.network

import com.kontakti.data.contacts.ImportCandidate
import com.kontakti.data.model.*
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout()

    @GET("auth/me")
    suspend fun me(): UserProfile

    // People
    @GET("people")
    suspend fun listPeople(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1
    ): Paginated<Person>

    @GET("people/{id}")
    suspend fun getPerson(@Path("id") id: String): Person

    @GET("people/{id}/timeline")
    suspend fun getTimeline(@Path("id") id: String): List<TimelineEvent>

    @GET("people/{id}/discussions")
    suspend fun getPersonDiscussions(@Path("id") id: String): List<Discussion>

    @GET("people/{id}/tasks")
    suspend fun getPersonTasks(@Path("id") id: String): List<Task>

    // Companies
    @GET("companies")
    suspend fun listCompanies(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1
    ): Paginated<Company>

    @GET("companies/{id}")
    suspend fun getCompany(@Path("id") id: String): Company

    @GET("companies/{id}/people")
    suspend fun getCompanyPeople(@Path("id") id: String): List<Person>

    // Discussions
    @GET("discussions")
    suspend fun listDiscussions(
        @Query("q") query: String? = null,
        @Query("type") type: String? = null,
        @Query("page") page: Int = 1
    ): Paginated<Discussion>

    @GET("discussions/{id}")
    suspend fun getDiscussion(@Path("id") id: String): Discussion

    @POST("discussions")
    suspend fun createDiscussion(@Body req: CreateDiscussionRequest): Discussion

    // Feed + Search
    @GET("feed")
    suspend fun getFeed(): List<FeedItem>

    @GET("search")
    suspend fun search(@Query("q") query: String): SearchResponse

    // Contacts import
    @POST("contacts/import")
    suspend fun importContacts(@Body candidates: List<ImportCandidate>): ImportResult
}
