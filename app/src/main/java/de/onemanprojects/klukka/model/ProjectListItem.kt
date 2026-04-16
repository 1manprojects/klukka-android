package de.onemanprojects.klukka.model

sealed class ProjectListItem {
    data class Header(val title: String) : ProjectListItem()
    data class Entry(val project: Project) : ProjectListItem()
}
