package de.onemanprojects.klukka.model

data class NewProjectRequest(
    val id: Int = -1,
    val title: String,
    val description: String?,
    val color: String?,
    val trackedThisMonth: Double = 0.0,
    val ref: Int = -1,
    val refType: String = "USER",
    val archived: Boolean = false
)
