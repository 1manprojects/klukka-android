package de.onemanprojects.klukka

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val Y_LABEL_WIDTH_DP = 44f
        private const val X_LABEL_HEIGHT_DP = 32f
        private const val BAR_H_PAD_DP = 2f
        private const val CORNER_DP = 3f
        private const val TEXT_DP = 10f
        private const val GRID_LINES = 4
    }

    private val d = resources.displayMetrics.density
    private var barData: List<Pair<LocalDate, List<ProjectSegment>>> = emptyList()
    private val barRect = RectF()

    private val colorPrimary: Int
    private val colorSurface: Int
    private val colorOnSurfaceVariant: Int
    private val colorOutlineVariant: Int

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint()
    private val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noDataPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        colorPrimary = themeColor("colorPrimary", Color.BLUE)
        colorSurface = themeColor("colorSurface", Color.WHITE)
        colorOnSurfaceVariant = themeColor("colorOnSurfaceVariant", Color.DKGRAY)
        colorOutlineVariant = themeColor("colorOutlineVariant", Color.LTGRAY)

        setBackgroundColor(colorSurface)

        barPaint.color = colorPrimary
        barPaint.style = Paint.Style.FILL

        gridPaint.color = colorOutlineVariant
        gridPaint.strokeWidth = 0.5f * d
        gridPaint.style = Paint.Style.STROKE
        gridPaint.isAntiAlias = false

        yLabelPaint.color = colorOnSurfaceVariant
        yLabelPaint.textSize = TEXT_DP * d
        yLabelPaint.textAlign = Paint.Align.RIGHT

        xLabelPaint.color = colorOnSurfaceVariant
        xLabelPaint.textSize = TEXT_DP * d
        xLabelPaint.textAlign = Paint.Align.CENTER

        noDataPaint.color = colorOnSurfaceVariant
        noDataPaint.textSize = (TEXT_DP + 2f) * d
        noDataPaint.textAlign = Paint.Align.CENTER
    }

    fun setData(data: List<Pair<LocalDate, List<ProjectSegment>>>) {
        barData = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (barData.isEmpty()) {
            canvas.drawText(
                context.getString(R.string.activity_no_data),
                width / 2f, height / 2f,
                noDataPaint
            )
            return
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val yLabelW = Y_LABEL_WIDTH_DP * d
        val xLabelH = X_LABEL_HEIGHT_DP * d
        val chartLeft = yLabelW
        val chartRight = w - 4f * d
        val chartTop = 4f * d
        val chartBottom = h - xLabelH
        val chartH = chartBottom - chartTop
        val chartW = chartRight - chartLeft

        val maxMins = barData.maxOfOrNull { (_, segs) -> segs.sumOf { it.minutes } }
            ?.coerceAtLeast(1L) ?: 1L
        val maxHours = ((maxMins + 59) / 60).coerceAtLeast(1L)
        val scale = maxHours * 60L // total minutes at top of chart

        // Grid lines + Y labels
        for (i in 0..GRID_LINES) {
            val y = chartBottom - i.toFloat() / GRID_LINES * chartH
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
            val labelMins = i.toLong() * scale / GRID_LINES
            val labelHours = labelMins / 60
            canvas.drawText("${labelHours}h", yLabelW - 4f * d, y + TEXT_DP * d / 2f, yLabelPaint)
        }

        // Stacked bars — one coloured segment per project per day
        val barCount = barData.size
        val slotW = chartW / barCount
        val padPx = BAR_H_PAD_DP * d
        val cornerPx = CORNER_DP * d

        for ((idx, entry) in barData.withIndex()) {
            val (_, segments) = entry
            if (segments.isEmpty()) continue
            val totalDayMins = segments.sumOf { it.minutes }
            if (totalDayMins <= 0L) continue

            val left = chartLeft + idx * slotW + padPx
            val right = chartLeft + (idx + 1) * slotW - padPx
            var currentBottom = chartBottom

            for (seg in segments) {
                val segFrac = seg.minutes.toFloat() / scale.toFloat()
                val segTop = (currentBottom - segFrac * chartH)
                    .coerceAtMost(currentBottom)
                barPaint.color = parseProjectColor(seg.colorString)
                barRect.set(left, segTop, right, currentBottom)
                canvas.drawRoundRect(barRect, cornerPx, cornerPx, barPaint)
                currentBottom = segTop
            }
        }

        // X labels
        val lineOneY = chartBottom + TEXT_DP * d + 2f * d
        val lineTwoY = chartBottom + TEXT_DP * d * 2 + 6f * d

        for ((idx, entry) in barData.withIndex()) {
            val (date, _: List<ProjectSegment>) = entry
            val cx = chartLeft + (idx + 0.5f) * slotW

            when {
                barCount <= 7 -> {
                    val dayName = date.dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        .uppercase(Locale.getDefault())
                    canvas.drawText(dayName, cx, lineOneY, xLabelPaint)
                    canvas.drawText(date.dayOfMonth.toString(), cx, lineTwoY, xLabelPaint)
                }
                barCount <= 14 -> {
                    if (idx % 2 == 0)
                        canvas.drawText(date.dayOfMonth.toString(), cx, lineOneY, xLabelPaint)
                }
                else -> {
                    val step = if (barCount <= 21) 4 else 7
                    if (idx % step == 0)
                        canvas.drawText(date.dayOfMonth.toString(), cx, lineOneY, xLabelPaint)
                }
            }
        }
    }

    private fun parseProjectColor(colorStr: String?): Int {
        if (colorStr.isNullOrBlank()) return colorPrimary
        return try { Color.parseColor(colorStr) } catch (_: Exception) { colorPrimary }
    }

    private fun themeColor(attrName: String, default: Int): Int {
        val tv = TypedValue()
        var id = context.resources.getIdentifier(attrName, "attr", context.packageName)
        if (id == 0) id = context.resources.getIdentifier(attrName, "attr", "com.google.android.material")
        if (id == 0) return default
        return if (context.theme.resolveAttribute(id, tv, true)) tv.data else default
    }
}
