package de.onemanprojects.klukka.model

data class UserApiToken(
    val id: Int,
    val description: String?,
    val expiration: Timestamp?
)
