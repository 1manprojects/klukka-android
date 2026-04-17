package de.onemanprojects.klukka.model

data class AnalysisData(
    val projects: List<Project>?,
    val groupProjects: List<Project>?,
    val tracked: List<Tracked>?
)
