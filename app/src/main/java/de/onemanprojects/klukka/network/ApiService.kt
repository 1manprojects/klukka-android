package de.onemanprojects.klukka.network

import de.onemanprojects.klukka.model.UserProjects
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {

    @GET("api/projects")
    suspend fun getProjects(
        @Header("Authorization") token: String
    ): UserProjects
}
