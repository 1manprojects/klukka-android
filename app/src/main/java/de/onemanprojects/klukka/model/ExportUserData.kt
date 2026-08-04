package de.onemanprojects.klukka.model

data class ExportUserData(
    val projects: List<Project>?,
    val trackedItems: List<Tracked>?
)
