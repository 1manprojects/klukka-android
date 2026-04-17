package de.onemanprojects.klukka.model

data class User(
    val id: Int,
    val mail: String?,
    val roles: List<String>?
)
