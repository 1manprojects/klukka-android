package de.onemanprojects.klukka.model

import com.google.gson.annotations.SerializedName

data class Project(
    val id: Int,
    val title: String?,
    val description: String?,
    val color: String?,
    @SerializedName("trackedThisMonth") val trackedThisMonth: Double,
    val ref: Int,
    val archived: Boolean
)
