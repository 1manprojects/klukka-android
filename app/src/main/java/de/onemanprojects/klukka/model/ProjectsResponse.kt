package de.onemanprojects.klukka.model

/** Wrapper matching the server's {"payload": UserProjects} envelope. */
data class ProjectsResponse(val payload: UserProjects?)
