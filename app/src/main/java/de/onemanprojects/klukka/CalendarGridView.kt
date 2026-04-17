package de.onemanprojects.klukka

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import de.onemanprojects.klukka.model.Project
import de.onemanprojects.klukka.model.Tracked
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class CalendarGridView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val HOUR_COUNT = 24
        private const val HOUR_HEIGHT_DP = 56f
        private const val HEADER_HEIGHT_DP = 48f
        private const val TIME_COL_WIDTH_DP = 52f
        private const val EVENT_H_PAD_DP = 2f
        private const val EVENT_V_PAD_DP = 1f
        private const val CORNER_RADIUS_DP = 3f
        private const val MIN_EVENT_HEIGHT_DP = 4f
        private const val TEXT_LABEL_DP = 10f
        private const val TEXT_HEADER_DP = 11f
        private const val TEXT_EVENT_DP = 11f
    }

    private val d = resources.displayMetrics.density

    private val hourHeightPx = HOUR_HEIGHT_DP * d
    private val headerHeightPx = HEADER_HEIGHT_DP * d
    private val timeColWidthPx = TIME_COL_WIDTH_DP * d
    private val cornerRadiusPx = CORNER_RADIUS_DP * d
    private val minEventHeightPx = MIN_EVENT_HEIGHT_DP * d

    // Data
    private var days: List<LocalDate> = emptyList()
    private var trackedItems: List<Tracked> = emptyList()
    private var projectsById: Map<Int, Project> = emptyMap()

    // Tap listener
    var onTrackedClickListener: ((Tracked) -> Unit)? = null
    private val eventHitAreas = mutableListOf<Pair<RectF, Tracked>>()

    // Reusable rect to avoid allocation in onDraw
    private val eventRect = RectF()

    // Colours resolved once from the theme
    private val colorPrimary: Int
    private val colorSurface: Int
    private val colorOnSurface: Int
    private val colorOnSurfaceVariant: Int
    private val colorOutlineVariant: Int
    private val colorTodayHighlight: Int
    private val colorError: Int

    // Paints – all created once in init to avoid allocs in onDraw
    private val gridLinePaint = Paint()
    private val todayColPaint = Paint()
    private val headerBgPaint = Paint()
    private val timeBgPaint = Paint()
    private val dayNamePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dayNameTodayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dayNumPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dayNumTodayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val timeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eventFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eventTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val currentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val currentDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        colorPrimary = themeColor("colorPrimary", Color.BLUE)
        colorSurface = themeColor("colorSurface", Color.WHITE)
        colorOnSurface = themeColor("colorOnSurface", Color.BLACK)
        colorOnSurfaceVariant = themeColor("colorOnSurfaceVariant", Color.DKGRAY)
        colorOutlineVariant = themeColor("colorOutlineVariant", Color.LTGRAY)
        val colorSecContainer = themeColor("colorSecondaryContainer", 0xFFE8DEF8.toInt())
        colorTodayHighlight = ColorUtils.setAlphaComponent(colorSecContainer, 90)
        colorError = themeColor("colorError", Color.RED)

        setBackgroundColor(colorSurface)

        gridLinePaint.apply {
            color = colorOutlineVariant
            strokeWidth = 0.5f * d
            style = Paint.Style.STROKE
            isAntiAlias = false
        }
        todayColPaint.apply {
            color = colorTodayHighlight
            style = Paint.Style.FILL
        }
        headerBgPaint.apply {
            color = colorSurface
            style = Paint.Style.FILL
        }
        timeBgPaint.apply {
            color = colorSurface
            style = Paint.Style.FILL
        }
        dayNamePaint.apply {
            color = colorOnSurfaceVariant
            textSize = TEXT_HEADER_DP * d
            textAlign = Paint.Align.CENTER
        }
        dayNameTodayPaint.apply {
            color = colorPrimary
            textSize = TEXT_HEADER_DP * d
            textAlign = Paint.Align.CENTER
        }
        dayNumPaint.apply {
            color = colorOnSurface
            textSize = (TEXT_HEADER_DP + 3f) * d
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        dayNumTodayPaint.apply {
            color = colorPrimary
            textSize = (TEXT_HEADER_DP + 3f) * d
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        timeLabelPaint.apply {
            color = colorOnSurfaceVariant
            textSize = TEXT_LABEL_DP * d
            textAlign = Paint.Align.RIGHT
        }
        eventFillPaint.apply {
            style = Paint.Style.FILL
        }
        eventTextPaint.apply {
            color = Color.WHITE
            textSize = TEXT_EVENT_DP * d
        }
        currentLinePaint.apply {
            color = colorError
            strokeWidth = 2f * d
            style = Paint.Style.STROKE
        }
        currentDotPaint.apply {
            color = colorError
            style = Paint.Style.FILL
        }
    }

    // ── Public API ───────────────────────────────────────────────────────────

    fun setDays(days: List<LocalDate>) {
        this.days = days
        invalidate()
    }

    fun setData(tracked: List<Tracked>, projects: List<Project>) {
        this.trackedItems = tracked
        this.projectsById = projects.associateBy { it.id }
        invalidate()
    }

    // ── Measure / Draw ───────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (headerHeightPx + HOUR_COUNT * hourHeightPx).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y
            for ((rect, tracked) in eventHitAreas) {
                if (rect.contains(x, y)) {
                    onTrackedClickListener?.invoke(tracked)
                    return true
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        if (days.isEmpty()) return

        eventHitAreas.clear()

        val w = width.toFloat()
        val gridTop = headerHeightPx
        val gridBottom = headerHeightPx + HOUR_COUNT * hourHeightPx
        val dayCount = days.size
        val dayColWidth = (w - timeColWidthPx) / dayCount
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        // 1. Highlight today's column
        val todayIdx = days.indexOf(today)
        if (todayIdx >= 0) {
            val left = timeColWidthPx + todayIdx * dayColWidth
            canvas.drawRect(left, gridTop, left + dayColWidth, gridBottom, todayColPaint)
        }

        // 2. Events – clipped to the grid area so they never overlap the header
        canvas.save()
        canvas.clipRect(timeColWidthPx, gridTop, w, gridBottom)
        val dayDurationMs = 24L * 3_600_000L
        for ((colIdx, day) in days.withIndex()) {
            val colLeft = timeColWidthPx + colIdx * dayColWidth + EVENT_H_PAD_DP * d
            val colRight = timeColWidthPx + (colIdx + 1) * dayColWidth - EVENT_H_PAD_DP * d
            val dayStartMs = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEndMs = dayStartMs + dayDurationMs

            for (tracked in trackedItems) {
                val startMs = Tracked.parseToEpochMillis(tracked.start) ?: continue
                val endMs = Tracked.parseToEpochMillis(tracked.end) ?: (startMs + 15 * 60_000L)
                if (endMs <= dayStartMs || startMs >= dayEndMs) continue

                val clampedStart = startMs.coerceIn(dayStartMs, dayEndMs)
                val clampedEnd = endMs.coerceIn(dayStartMs, dayEndMs)
                val topFrac = (clampedStart - dayStartMs).toFloat() / dayDurationMs
                val bottomFrac = (clampedEnd - dayStartMs).toFloat() / dayDurationMs
                val top = gridTop + topFrac * (HOUR_COUNT * hourHeightPx) + EVENT_V_PAD_DP * d
                val bottom = (gridTop + bottomFrac * (HOUR_COUNT * hourHeightPx) - EVENT_V_PAD_DP * d)
                    .coerceAtLeast(top + minEventHeightPx)

                val project = projectsById[tracked.projectId]
                val color = parseColor(project?.color, colorPrimary)
                eventFillPaint.color = color

                eventRect.set(colLeft, top, colRight, bottom)
                canvas.drawRoundRect(eventRect, cornerRadiusPx, cornerRadiusPx, eventFillPaint)
                eventHitAreas.add(Pair(RectF(eventRect), tracked))

                // Text content — only drawn when the block is tall enough
                if (bottom - top >= 16f * d) {
                    eventTextPaint.color = contrastColor(color)
                    val textX = colLeft + 4f * d
                    val lineH = (TEXT_EVENT_DP + 2f) * d

                    // Line 1: project title
                    val title = project?.title ?: "#${tracked.projectId}"
                    canvas.drawText(title, textX, top + lineH, eventTextPaint)

                    // Line 2: duration in h / m
                    if (bottom - top >= lineH * 2f) {
                        val durationMs = (endMs - startMs).coerceAtLeast(0)
                        val totalMins = (durationMs / 60_000L).toInt()
                        val hours = totalMins / 60
                        val mins = totalMins % 60
                        val durationText = when {
                            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                            hours > 0 -> "${hours}h"
                            else -> "${mins}m"
                        }
                        canvas.drawText(durationText, textX, top + lineH * 2f, eventTextPaint)
                    }

                    // Line 3: comment (truncated to fit)
                    if (!tracked.comment.isNullOrBlank() && bottom - top >= lineH * 3f) {
                        val comment = tracked.comment!!.let {
                            if (it.length > 18) it.take(17) + "…" else it
                        }
                        canvas.drawText(comment, textX, top + lineH * 3f, eventTextPaint)
                    }
                }
            }
        }
        canvas.restore()

        // 3. Hour grid lines
        for (hour in 0..HOUR_COUNT) {
            val y = gridTop + hour * hourHeightPx
            canvas.drawLine(timeColWidthPx, y, w, y, gridLinePaint)
        }

        // 4. Day column separator lines
        for (i in 0..dayCount) {
            val x = timeColWidthPx + i * dayColWidth
            canvas.drawLine(x, gridTop, x, gridBottom, gridLinePaint)
        }

        // 5. Current-time indicator
        if (todayIdx >= 0) {
            val fracOfDay = LocalTime.now().toSecondOfDay().toFloat() / (24f * 3_600f)
            val y = gridTop + fracOfDay * (HOUR_COUNT * hourHeightPx)
            val left = timeColWidthPx + todayIdx * dayColWidth
            val right = left + dayColWidth
            canvas.drawCircle(left + 4f * d, y, 4f * d, currentDotPaint)
            canvas.drawLine(left + 4f * d, y, right, y, currentLinePaint)
        }

        // 6. Time-label column – opaque background then labels
        canvas.drawRect(0f, gridTop, timeColWidthPx, gridBottom, timeBgPaint)
        for (hour in 1 until HOUR_COUNT) {
            val y = gridTop + hour * hourHeightPx
            canvas.drawText(
                String.format(Locale.getDefault(), "%02d:00", hour),
                timeColWidthPx - 4f * d,
                y - 3f * d,
                timeLabelPaint
            )
        }

        // 7. Day-column header – opaque background then texts
        canvas.drawRect(0f, 0f, w, headerHeightPx, headerBgPaint)
        canvas.drawLine(timeColWidthPx, headerHeightPx, w, headerHeightPx, gridLinePaint)

        for ((colIdx, day) in days.withIndex()) {
            val cx = timeColWidthPx + colIdx * dayColWidth + dayColWidth / 2f
            val isToday = day == today
            val namePaint = if (isToday) dayNameTodayPaint else dayNamePaint
            val numPaint = if (isToday) dayNumTodayPaint else dayNumPaint
            val dayName = day.dayOfWeek
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .uppercase(Locale.getDefault())
            canvas.drawText(dayName, cx, headerHeightPx * 0.40f + TEXT_HEADER_DP * d / 2f, namePaint)
            canvas.drawText(
                day.dayOfMonth.toString(),
                cx,
                headerHeightPx * 0.82f + (TEXT_HEADER_DP + 3f) * d / 2f,
                numPaint
            )
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun parseColor(colorStr: String?, fallback: Int): Int {
        if (colorStr.isNullOrBlank()) return fallback
        return try { Color.parseColor(colorStr) } catch (_: Exception) { fallback }
    }

    /** Returns black or white depending on which contrasts better with [bg]. */
    private fun contrastColor(bg: Int): Int {
        val lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0
        return if (lum > 0.5) Color.BLACK else Color.WHITE
    }

    /**
     * Resolves a theme colour by attribute name. Looks in the app's package first,
     * then in the Material library package, falling back to [default].
     */
    private fun themeColor(attrName: String, default: Int): Int {
        val tv = TypedValue()
        // Try to find the attr ID in the app package (merged attrs from libs may be here)
        var id = context.resources.getIdentifier(attrName, "attr", context.packageName)
        // Fall back to Material's package when non-transitive R is in use
        if (id == 0) id = context.resources.getIdentifier(attrName, "attr", "com.google.android.material")
        if (id == 0) return default
        return if (context.theme.resolveAttribute(id, tv, true)) tv.data else default
    }
}
