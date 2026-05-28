package com.example.data

import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncPayload(
    val email: String,
    val tasks: List<Task>,
    val settings: AppSettings
)

interface TodoBackupApi {
    @POST("/api/sync/backup")
    suspend fun backupData(
        @Query("email") email: String,
        @Body payload: SyncPayload
    ): retrofit2.Response<Void>

    @GET("/api/sync/restore")
    suspend fun restoreData(
        @Query("email") email: String
    ): SyncPayload
}

object SyncApiClient {
    // Note: The user MUST provide a valid backend URL, e.g., via a BuildConfig variable or settings.
    // We are using a placeholder here because they have not deployed an API yet.
    // If they have one, they should update the BASE_URL below.
    private const val BASE_URL = "https://your-custom-backend.com"

    private val moshi = Moshi.Builder().build()

    val api: TodoBackupApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TodoBackupApi::class.java)
    }
}
