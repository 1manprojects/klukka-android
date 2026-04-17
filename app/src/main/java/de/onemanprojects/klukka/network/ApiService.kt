package de.onemanprojects.klukka.network

import de.onemanprojects.klukka.model.AnalysisDataResponse
import de.onemanprojects.klukka.model.ApiResponse
import de.onemanprojects.klukka.model.ArchiveRequest
import de.onemanprojects.klukka.model.CommentUpdate
import de.onemanprojects.klukka.model.DataFilter
import de.onemanprojects.klukka.model.Project
import de.onemanprojects.klukka.model.ProjectsResponse
import de.onemanprojects.klukka.model.StartRequest
import de.onemanprojects.klukka.model.TrackedResponse
import de.onemanprojects.klukka.model.UpdateTrackedRequest
import de.onemanprojects.klukka.model.UserDataResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @GET("api/projects")
    suspend fun getProjects(
        @Header("Authorization") token: String
    ): ProjectsResponse

    @POST("api/start")
    suspend fun startTracking(
        @Header("Authorization") token: String,
        @Body start: StartRequest
    ): ApiResponse

    @POST("api/user/stop")
    suspend fun stopTracking(
        @Header("Authorization") token: String,
        @Body id: Int
    ): ApiResponse

    @GET("api/archived")
    suspend fun getArchivedProjects(
        @Header("Authorization") token: String
    ): ProjectsResponse

    @POST("api/archive")
    suspend fun archiveProject(
        @Header("Authorization") token: String,
        @Body body: ArchiveRequest
    ): ApiResponse

    @GET("api/active")
    suspend fun getActiveTracking(
        @Header("Authorization") token: String
    ): TrackedResponse

    @POST("api/comment")
    suspend fun updateComment(
        @Header("Authorization") token: String,
        @Body body: CommentUpdate
    ): ApiResponse

    @POST("api/data")
    suspend fun getData(
        @Header("Authorization") token: String,
        @Body filter: DataFilter
    ): AnalysisDataResponse

    @POST("api/update")
    suspend fun updateTracked(
        @Header("Authorization") token: String,
        @Body body: UpdateTrackedRequest
    ): ApiResponse

    @POST("api/delete")
    suspend fun deleteTracked(
        @Header("Authorization") token: String,
        @Body id: Int
    ): ApiResponse

    @POST("api/user/addPersonalProject")
    suspend fun addPersonalProject(
        @Header("Authorization") token: String,
        @Body project: Project
    ): ApiResponse

    @POST("api/deleteProject")
    suspend fun deleteProject(
        @Header("Authorization") token: String,
        @Body projectId: Int
    ): ApiResponse

    @GET("api/user/data")
    suspend fun getUserData(
        @Header("Authorization") token: String
    ): UserDataResponse

    @POST("api/user/deleteToken")
    suspend fun deleteToken(
        @Header("Authorization") token: String,
        @Body tokenId: Int
    ): ApiResponse

    @POST("api/user/leaveGroup")
    suspend fun leaveGroup(
        @Header("Authorization") token: String,
        @Body groupId: Int
    ): ApiResponse

    @GET("api/user/delete")
    suspend fun deleteAccount(
        @Header("Authorization") token: String
    ): ApiResponse
}
