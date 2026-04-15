package de.onemanprojects.klukka.model

data class TrackedTimestamp(val time: Long)

data class Tracked(
    val id: Int,
    val projectId: Int,
    val active: Boolean,
    val start: TrackedTimestamp?
)
