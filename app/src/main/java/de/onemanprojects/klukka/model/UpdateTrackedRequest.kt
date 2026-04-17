package de.onemanprojects.klukka.model

import com.google.gson.annotations.SerializedName

data class UpdateTrackedRequest(
    val id: Int,
    val projectId: Int,
    @SerializedName("isActive") val active: Boolean,
    val start: String?,
    val end: String?,
    val timezone: String?,
    val comment: String?
)
