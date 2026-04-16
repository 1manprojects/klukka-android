package de.onemanprojects.klukka.model

import com.google.gson.annotations.SerializedName

data class Tracked(
    val id: Int,
    val projectId: Int,
    @SerializedName("isActive") val active: Boolean,
    val start: String?,
    val timezone: String?,
    val comment: String?
)
