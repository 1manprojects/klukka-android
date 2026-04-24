package de.onemanprojects.klukka.model

data class ExportFilter(
    val filter: DataFilter?,
    val detailed: Boolean,
    val groupId: Int?
)
