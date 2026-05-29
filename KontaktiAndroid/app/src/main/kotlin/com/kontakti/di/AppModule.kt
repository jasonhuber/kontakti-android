package com.kontakti.di

import android.content.Context
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.kontakti.data.datastore.TokenStore
import com.kontakti.data.local.KontaktiDao
import com.kontakti.data.local.KontaktiDatabase
import com.kontakti.data.model.DiscussionType
import com.kontakti.data.model.QuestionKey
import com.kontakti.data.model.RelationshipStrength
import com.kontakti.data.model.TaskPriority
import com.kontakti.data.model.TodayItemKind
import com.kontakti.data.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private inline fun <reified T : Enum<T>> enumAdapter(): TypeAdapter<T> =
        object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) { out.value(value?.name) }
            override fun read(input: JsonReader): T? {
                val s = input.nextString()
                return enumValues<T>().firstOrNull { it.name.equals(s, ignoreCase = true) }
            }
        }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .registerTypeAdapter(RelationshipStrength::class.java, enumAdapter<RelationshipStrength>())
        .registerTypeAdapter(DiscussionType::class.java, enumAdapter<DiscussionType>())
        .registerTypeAdapter(TaskPriority::class.java, enumAdapter<TaskPriority>())
        .registerTypeAdapter(QuestionKey::class.java, enumAdapter<QuestionKey>())
        .registerTypeAdapter(TodayItemKind::class.java, enumAdapter<TodayItemKind>())
        .serializeNulls()
        .create()

    @Provides
    @Singleton
    fun provideOkHttp(tokenStore: TokenStore): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = kotlinx.coroutines.runBlocking { tokenStore.tokenFlow.first() }
                val builder = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                if (!token.isNullOrBlank()) {
                    builder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(okHttpClient: OkHttpClient, gson: Gson): ApiService =
        Retrofit.Builder()
            .baseUrl("https://kontakti.app/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KontaktiDatabase =
        KontaktiDatabase.create(context)

    @Provides
    @Singleton
    fun provideDao(db: KontaktiDatabase): KontaktiDao = db.dao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore = TokenStore(context)
}
