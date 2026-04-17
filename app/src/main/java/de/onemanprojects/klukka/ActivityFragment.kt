package de.onemanprojects.klukka

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val TAG = "ActivityFragment"

class ActivityFragment : Fragment() {

    private val viewModel: ActivityViewModel by viewModels()
    private val dateDisplayFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_activity, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val togglePreset = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_preset)
        val etDateFrom = view.findViewById<TextInputEditText>(R.id.et_date_from)
        val etDateTo = view.findViewById<TextInputEditText>(R.id.et_date_to)
        val progressBar = view.findViewById<LinearProgressIndicator>(R.id.progress_loading)
        val barChart = view.findViewById<BarChartView>(R.id.bar_chart)
        val tvNoData = view.findViewById<TextView>(R.id.tv_no_data)
        val tvTotalTime = view.findViewById<TextView>(R.id.tv_total_time)
        val tvProjectCount = view.findViewById<TextView>(R.id.tv_project_count)
        val llProjects = view.findViewById<LinearLayout>(R.id.ll_projects)

        // Pre-select Week to match ViewModel default
        togglePreset.check(R.id.btn_preset_week)

        togglePreset.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btn_preset_today -> viewModel.selectPreset(ActivityPreset.TODAY)
                R.id.btn_preset_week -> viewModel.selectPreset(ActivityPreset.WEEK)
                R.id.btn_preset_month -> viewModel.selectPreset(ActivityPreset.MONTH)
            }
        }

        etDateFrom.setOnClickListener {
            val currentMs = viewModel.startDate.value
                ?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
            showDatePicker(currentMs) { picked ->
                val end = viewModel.endDate.value ?: picked
                viewModel.setCustomRange(picked, if (picked.isAfter(end)) picked else end)
                togglePreset.clearChecked()
            }
        }

        etDateTo.setOnClickListener {
            val currentMs = viewModel.endDate.value
                ?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
            showDatePicker(currentMs) { picked ->
                val start = viewModel.startDate.value ?: picked
                viewModel.setCustomRange(if (picked.isBefore(start)) picked else start, picked)
                togglePreset.clearChecked()
            }
        }

        viewModel.startDate.observe(viewLifecycleOwner) { date ->
            etDateFrom.setText(date?.format(dateDisplayFmt) ?: "")
        }

        viewModel.endDate.observe(viewLifecycleOwner) { date ->
            etDateTo.setText(date?.format(dateDisplayFmt) ?: "")
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.barData.observe(viewLifecycleOwner) { data ->
            val allZero = data.all { it.second == 0L }
            barChart.visibility = if (data.isNotEmpty() && !allZero) View.VISIBLE else View.GONE
            tvNoData.visibility = if (data.isEmpty() || allZero) View.VISIBLE else View.GONE
            barChart.setData(data)
        }

        viewModel.totalMinutes.observe(viewLifecycleOwner) { mins ->
            tvTotalTime.text = formatMinutes(mins)
        }

        viewModel.projectTotals.observe(viewLifecycleOwner) { totals ->
            tvProjectCount.text = totals.size.toString()
            populateProjectList(llProjects, totals)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
        }

        viewModel.unauthorized.observe(viewLifecycleOwner) { unauthorized ->
            if (unauthorized == true) redirectToLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    private fun populateProjectList(
        container: LinearLayout,
        totals: List<Pair<de.onemanprojects.klukka.model.Project, Long>>
    ) {
        container.removeAllViews()
        for ((project, mins) in totals) {
            val item = layoutInflater.inflate(R.layout.item_activity_project, container, false)
            item.findViewById<TextView>(R.id.tv_project_name).text =
                project.title ?: "Project ${project.id}"
            item.findViewById<TextView>(R.id.tv_project_time).text = formatMinutes(mins)

            val colorView = item.findViewById<View>(R.id.view_project_color)
            val parsedColor = try {
                if (!project.color.isNullOrBlank()) Color.parseColor(project.color) else null
            } catch (_: Exception) { null }
            if (parsedColor != null) {
                colorView.background = GradientDrawable().apply {
                    setColor(parsedColor)
                }
            }
            container.addView(item)
        }
    }

    private fun formatMinutes(mins: Long): String {
        val h = mins / 60
        val m = mins % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    private fun showDatePicker(initialMs: Long?, onPicked: (LocalDate) -> Unit) {
        val picker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setSelection(initialMs ?: System.currentTimeMillis())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            onPicked(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate())
        }
        picker.show(childFragmentManager, "date_picker_activity")
    }

    private fun redirectToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
