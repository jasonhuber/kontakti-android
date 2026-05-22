package com.kontakti.data.network

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.kontakti.data.model.DiscussionType
import com.kontakti.data.model.RelationshipStrength
import com.kontakti.data.model.TaskPriority
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private var tokenProvider: () -> String? = { null }

    fun init(tokenProvider: () -> String?) {
        this.tokenProvider = tokenProvider
    }

    private inline fun <reified T : Enum<T>> enumTypeAdapter(): TypeAdapter<T> = object : TypeAdapter<T>() {
        override fun write(out: JsonWriter, value: T?) {
            out.value(value?.name)
        }

        override fun read(input: JsonReader): T? {
            val s = input.nextString()
            return enumValues<T>().firstOrNull { it.name.equals(s, ignoreCase = true) }
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(RelationshipStrength::class.java, enumTypeAdapter<RelationshipStrength>())
        .registerTypeAdapter(DiscussionType::class.java, enumTypeAdapter<DiscussionType>())
        .registerTypeAdapter(TaskPriority::class.java, enumTypeAdapter<TaskPriority>())
        .serializeNulls()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okhttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = tokenProvider()
            val req = chain.request().newBuilder()
                .addHeader("Accept", "application/json")
                .apply {
                    if (token != null) addHeader("Authorization", "Bearer $token")
                }
                .build()
            chain.proceed(req)
        }
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://kontakti.app/api/v1/")
        .client(okhttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val service: ApiService = retrofit.create(ApiService::class.java)
}
