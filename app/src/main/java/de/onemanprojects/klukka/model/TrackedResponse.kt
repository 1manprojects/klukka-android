package de.onemanprojects.klukka.model

/** Wrapper matching the server's {"payload": Tracked} envelope. */
data class TrackedResponse(val payload: Tracked?)
