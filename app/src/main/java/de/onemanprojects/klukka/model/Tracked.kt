package de.onemanprojects.klukka.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Tracked(
    val id: Int,
    val projectId: Int,
    @SerializedName("isActive") val active: Boolean,
    val start: String?,
    val end: String?,
    val timezone: String?,
    val comment: String?
) {
    companion object {
        // Matches "Apr 16, 2026, 5:34:29 AM" — handles regular and narrow no-break spaces
        private val TIMESTAMP_REGEX = Regex(
            """(\w{3})\s+(\d{1,2}),\s+(\d{4}),\s+(\d{1,2}):(\d{2}):(\d{2})\s+(AM|PM)""",
            RegexOption.IGNORE_CASE
        )
        private val MONTH_MAP = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
            "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
            "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
        )

        /** Parses the server's timestamp string into UTC epoch milliseconds, or null on failure. */
        fun parseToEpochMillis(raw: String?): Long? {
            if (raw.isNullOrBlank()) return null
            val match = TIMESTAMP_REGEX.find(raw.trim()) ?: return null
            val (monthStr, dayStr, yearStr, hourStr, minStr, secStr, ampm) = match.destructured
            val month = MONTH_MAP[monthStr.lowercase(Locale.ROOT)] ?: return null
            var hour = hourStr.toInt()
            if (ampm.uppercase(Locale.ROOT) == "PM" && hour != 12) hour += 12
            if (ampm.uppercase(Locale.ROOT) == "AM" && hour == 12) hour = 0
            val ldt = LocalDateTime.of(yearStr.toInt(), month, dayStr.toInt(), hour, minStr.toInt(), secStr.toInt())
            return ldt.toInstant(ZoneOffset.UTC).toEpochMilli()
        }
    }
}
