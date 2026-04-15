package de.onemanprojects.klukka.network

import de.onemanprojects.klukka.model.ApiResponse
import de.onemanprojects.klukka.model.ArchiveRequest
import de.onemanprojects.klukka.model.StartRequest
import de.onemanprojects.klukka.model.Tracked
import de.onemanprojects.klukka.model.UserProjects
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @GET("api/projects")
    suspend fun getProjects(
        @Header("Authorization") token: String
    ): UserProjects

    @POST("api/start")
    suspend fun startTracking(
        @Header("Authorization") token: String,
        @Body start: StartRequest
    ): ApiResponse

    @POST("api/user/stopTracking")
    suspend fun stopTracking(
        @Header("Authorization") token: String,
        @Body id: Int
    ): ApiResponse

    @GET("api/archived")
    suspend fun getArchivedProjects(
        @Header("Authorization") token: String
    ): UserProjects

    @POST("api/archive")
    suspend fun archiveProject(
        @Header("Authorization") token: String,
        @Body body: ArchiveRequest
    ): ApiResponse

    @GET("api/active")
    suspend fun getActiveTracking(
        @Header("Authorization") token: String
    ): Tracked
}
