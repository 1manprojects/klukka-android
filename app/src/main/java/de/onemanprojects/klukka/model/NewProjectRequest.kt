package de.onemanprojects.klukka.model

data class NewProjectRequest(
    val title: String,
    val description: String?,
    val color: String?
)
