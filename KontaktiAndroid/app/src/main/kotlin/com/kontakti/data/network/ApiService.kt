package com.kontakti.data.network

import com.kontakti.data.contacts.BulkImportRequest
import com.kontakti.data.contacts.ImportCandidate
import com.kontakti.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body req: RegisterRequest): LoginResponse

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body req: GoogleLoginRequest): LoginResponse

    @POST("auth/onboarding/complete")
    suspend fun completeOnboarding(): UserProfile

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

    @GET("people/{id}/notes")
    suspend fun listPersonNotes(@Path("id") id: String): List<Note>

    @PATCH("people/{id}")
    suspend fun updatePerson(
        @Path("id") id: String,
        @Body patch: com.google.gson.JsonObject
    ): Person

    @POST("notes")
    suspend fun createNote(@Body body: CreateNoteRequest): Note

    @PATCH("notes/{id}")
    suspend fun updateNote(
        @Path("id") id: String,
        @Body body: UpdateNoteRequest
    ): Note

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: String)

    @POST("tasks")
    suspend fun createTask(@Body body: CreateTaskRequest): Task

    @PATCH("tasks/{id}/complete")
    suspend fun completeTask(@Path("id") id: String): Task

    // Companies
    @GET("companies")
    suspend fun listCompanies(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1
    ): Paginated<Company>

    @POST("companies")
    suspend fun createCompany(@Body body: CreateCompanyRequest): Company

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

    @POST("people")
    suspend fun createPerson(@Body req: CreatePersonRequest): Person

    // Person photos (multi-photo gallery)
    @GET("people/{id}/photos")
    suspend fun listPhotos(@Path("id") id: String): List<PersonPhoto>

    /** Uploads a real image file. Accepts the same `source` values as the JSON variant. */
    @Multipart
    @POST("people/{id}/photos")
    suspend fun uploadPhotoMultipart(
        @Path("id") id: String,
        @Part file: MultipartBody.Part,
        @Part("source") source: RequestBody? = null
    ): PersonPhoto

    /** Uploads a data-URL (clipboard paste) or external URL pointer (LinkedIn CDN). */
    @POST("people/{id}/photos")
    suspend fun uploadPhotoJson(
        @Path("id") id: String,
        @Body body: PhotoUploadBody
    ): PersonPhoto

    @DELETE("people/{id}/photos/{photoId}")
    suspend fun deletePhoto(
        @Path("id") id: String,
        @Path("photoId") photoId: String
    )

    @POST("people/{id}/photos/{photoId}/primary")
    suspend fun setPrimaryPhoto(
        @Path("id") id: String,
        @Path("photoId") photoId: String
    ): PersonPhoto

    // Contacts import
    @POST("contacts/import")
    suspend fun importContacts(@Body body: BulkImportRequest): ImportResult

    @DELETE("people/{id}")
    suspend fun deletePerson(@Path("id") id: String)

    @POST("people/enrich")
    suspend fun enrichPerson(@Body body: com.google.gson.JsonObject): Person

    // Today inbox
    @GET("today")
    suspend fun getToday(@Query("limit") limit: Int = 10): TodayResponse

    @POST("today/items/{key}/draft")
    suspend fun draftTodayMessage(@Path("key") key: String): TodayDraftResponse

    @POST("today/items/{key}/log")
    suspend fun logTodayContact(
        @Path("key") key: String,
        @Body body: TodayLogRequest
    ): TodayLogResponse

    @POST("today/items/{key}/snooze")
    suspend fun snoozeTodayItem(@Path("key") key: String)

    @POST("today/items/{key}/skip")
    suspend fun skipTodayItem(@Path("key") key: String)

    // Daily quiz
    @POST("quiz/{id}/answer")
    suspend fun answerQuiz(
        @Path("id") id: String,
        @Body req: AnswerQuizRequest
    ): AnswerQuizResponse

    @POST("quiz/{id}/skip")
    suspend fun skipQuiz(@Path("id") id: String): Response<Unit>

    @GET("quiz/history")
    suspend fun quizHistory(): List<ContactPrompt>

    // Activity
    @GET("people/{id}/activity")
    suspend fun getPersonActivity(@Path("id") id: String): List<SocialActivity>

    @POST("people/{id}/activity/refresh")
    suspend fun refreshPersonActivity(@Path("id") id: String)

    @POST("activity/{id}/acknowledge")
    suspend fun acknowledgeActivity(@Path("id") id: String)

    // Social groups
    @GET("social-groups")
    suspend fun listSocialGroups(): List<SocialGroup>

    @POST("social-groups")
    suspend fun createSocialGroup(@Body body: CreateSocialGroupRequest): SocialGroup

    @POST("social-groups/{id}/sync")
    suspend fun syncSocialGroup(@Path("id") id: String)

    @DELETE("social-groups/{id}")
    suspend fun deleteSocialGroup(@Path("id") id: String)

    @GET("social-providers/facebook/groups")
    suspend fun getFacebookGroups(): FacebookGroupsResponse

    @GET("social-providers/whatsapp/status")
    suspend fun getWhatsappStatus(): WhatsappStatus

    @GET("social-providers/whatsapp/qr")
    suspend fun getWhatsappQR(): WhatsappQR

    @GET("social-providers/whatsapp/groups")
    suspend fun getWhatsappGroups(): WhatsappGroupsResponse

    // Duplicates
    @GET("duplicates")
    suspend fun listDuplicates(@Query("status") status: String = "pending"): List<DuplicateCandidate>

    @POST("duplicates/scan")
    suspend fun scanDuplicates()

    @POST("duplicates/{id}/merge")
    suspend fun mergeDuplicate(@Path("id") id: String, @Body body: MergeDuplicateRequest)

    @POST("duplicates/{id}/dismiss")
    suspend fun dismissDuplicate(@Path("id") id: String)

    // Voice
    @Multipart
    @POST("voice/capture")
    suspend fun captureVoice(
        @Part audio: MultipartBody.Part,
        @Part("person_id") personId: RequestBody? = null,
        @Part("context") context: RequestBody? = null
    ): VoiceCaptureResult

    // Natural search
    @POST("search/natural")
    suspend fun searchNatural(@Body body: NaturalSearchRequest): NaturalSearchResponse

    // Push
    @POST("push/register")
    suspend fun registerPush(@Body body: PushRegisterRequest)

    @HTTP(method = "DELETE", path = "push/register", hasBody = true)
    suspend fun unregisterPush(@Body body: PushUnregisterRequest)

    // Google accounts
    @GET("google-accounts")
    suspend fun listGoogleAccounts(): List<GoogleAccount>

    @POST("google-accounts/link")
    suspend fun linkGoogleAccount(@Body body: LinkGoogleAccountRequest): GoogleAccount

    @PATCH("google-accounts/{id}")
    suspend fun updateGoogleAccount(
        @Path("id") id: String,
        @Body body: UpdateGoogleAccountRequest
    ): GoogleAccount

    @DELETE("google-accounts/{id}")
    suspend fun unlinkGoogleAccount(@Path("id") id: String)
}
