package de.onemanprojects.klukka.model

data class Timestamp(
    val year: Int,
    val month: Int,
    val date: Int,
    val day: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val timezoneOffset: Int,
    val time: Long,
    val nanos: Int
)
