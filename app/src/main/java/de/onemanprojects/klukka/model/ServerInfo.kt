package de.onemanprojects.klukka.model

data class ServerInfo(
    val version: String?,
    val frontend: List<Dep>?,
    val backend: List<Dep>?
)

data class Dep(
    val name: String?,
    val version: String?,
    val url: String?,
    val license: String?
)

data class ServerInfoResponse(val payload: ServerInfo?)
