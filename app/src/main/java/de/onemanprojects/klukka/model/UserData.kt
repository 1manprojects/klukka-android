package de.onemanprojects.klukka.model

data class UserData(
    val user: User?,
    val projects: List<Project>?,
    val groups: List<Group>?,
    val tokens: List<UserApiToken>?
)
