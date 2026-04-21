package de.onemanprojects.klukka

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.onemanprojects.klukka.model.Project
import de.onemanprojects.klukka.model.Tracked
import de.onemanprojects.klukka.model.UpdateTrackedRequest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class EditTrackedDialogFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_TRACKED = "tracked"
        private const val ARG_PROJECTS = "projects"

        fun newInstance(tracked: Tracked, projects: List<Project>): EditTrackedDialogFragment =
            EditTrackedDialogFragment().apply {
                arguments = Bundle().apply {
                    val gson = Gson()
                    putString(ARG_TRACKED, gson.toJson(tracked))
                    putString(ARG_PROJECTS, gson.toJson(projects))
                }
            }
    }

    /** Wraps a Project so its toString() returns the title for MaterialAutoCompleteTextView. */
    private inner class ProjectItem(val project: Project) {
        override fun toString() = project.title ?: "Project ${project.id}"
    }

    private inner class ProjectAdapter(projects: List<Project>) :
        ArrayAdapter<ProjectItem>(requireContext(), 0, projects.map { ProjectItem(it) }) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            buildItemView(position, convertView, parent)

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            buildItemView(position, convertView, parent)

        private fun buildItemView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_project_dropdown, parent, false)
            val item = getItem(position) ?: return view
            view.findViewById<TextView>(R.id.tv_project_title).text = item.toString()
            val dot = view.findViewById<View>(R.id.view_color_dot)
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(parseColor(item.project.color))
            }
            dot.background = drawable
            return view
        }

        private fun parseColor(colorStr: String?): Int {
            if (colorStr.isNullOrBlank()) return Color.LTGRAY
            return try { Color.parseColor(colorStr) } catch (_: Exception) { Color.LTGRAY }
        }
    }

    private val calendarViewModel: CalendarViewModel by viewModels({ requireParentFragment() })

    private lateinit var tracked: Tracked
    private lateinit var nonArchivedProjects: List<Project>
    private var currentProjectId: Int = 0
    private var startDateTime: LocalDateTime? = null
    private var endDateTime: LocalDateTime? = null

    private val dateDisplayFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val timeDisplayFmt = DateTimeFormatter.ofPattern("HH:mm")
    private val serverFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_edit_tracked, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gson = Gson()
        tracked = gson.fromJson(requireArguments().getString(ARG_TRACKED), Tracked::class.java)
        val allProjects: List<Project> = gson.fromJson(
            requireArguments().getString(ARG_PROJECTS),
            object : TypeToken<List<Project>>() {}.type
        )

        nonArchivedProjects = allProjects.filter { !it.archived }
        val currentProject = allProjects.find { it.id == tracked.projectId }
        val isArchived = currentProject?.archived == true

        currentProjectId = tracked.projectId

        val isNewEntry = tracked.id == 0

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_title)
        val cardArchivedNotice = view.findViewById<MaterialCardView>(R.id.card_archived_notice)
        val layoutProject = view.findViewById<TextInputLayout>(R.id.layout_project)
        val dropdownProject = view.findViewById<MaterialAutoCompleteTextView>(R.id.dropdown_project)
        val layoutStartDate = view.findViewById<TextInputLayout>(R.id.layout_start_date)
        val layoutStartTime = view.findViewById<TextInputLayout>(R.id.layout_start_time)
        val layoutEndDate = view.findViewById<TextInputLayout>(R.id.layout_end_date)
        val layoutEndTime = view.findViewById<TextInputLayout>(R.id.layout_end_time)
        val layoutComment = view.findViewById<TextInputLayout>(R.id.layout_comment)
        val etStartDate = view.findViewById<TextInputEditText>(R.id.et_start_date)
        val etStartTime = view.findViewById<TextInputEditText>(R.id.et_start_time)
        val etEndDate = view.findViewById<TextInputEditText>(R.id.et_end_date)
        val etEndTime = view.findViewById<TextInputEditText>(R.id.et_end_time)
        val etComment = view.findViewById<TextInputEditText>(R.id.et_comment)
        val tvTrackedDuration = view.findViewById<TextView>(R.id.tv_tracked_duration)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save)
        val btnDelete = view.findViewById<MaterialButton>(R.id.btn_delete)

        tvTitle.setText(if (isNewEntry) R.string.edit_tracked_title_new else R.string.edit_tracked_title)
        if (isNewEntry) btnDelete.visibility = View.GONE

        // Project dropdown with color dots
        val adapter = ProjectAdapter(nonArchivedProjects)
        dropdownProject.setAdapter(adapter)
        val currentTitle = currentProject?.title ?: "Project ${tracked.projectId}"
        dropdownProject.setText(
            if (isArchived) "$currentTitle (${getString(R.string.edit_tracked_archived)})" else currentTitle,
            false
        )
        dropdownProject.setOnItemClickListener { _, _, position, _ ->
            currentProjectId = nonArchivedProjects[position].id
        }

        // Parse existing start/end into LocalDateTime in the device's local timezone for display.
        // For new entries default to current hour → current hour + 1.
        val localZone = ZoneId.systemDefault()
        startDateTime = Tracked.parseToEpochMillis(tracked.start)?.let {
            Instant.ofEpochMilli(it).atZone(localZone).toLocalDateTime()
        } ?: if (isNewEntry) {
            LocalDateTime.now(localZone).withMinute(0).withSecond(0).withNano(0)
        } else null
        endDateTime = Tracked.parseToEpochMillis(tracked.end)?.let {
            Instant.ofEpochMilli(it).atZone(localZone).toLocalDateTime()
        } ?: if (isNewEntry) startDateTime?.plusHours(1) else null

        fun updateDurationDisplay() {
            val s = startDateTime
            val e = endDateTime
            if (s != null && e != null) {
                val ms = (e.toInstant(ZoneOffset.UTC).toEpochMilli() -
                        s.toInstant(ZoneOffset.UTC).toEpochMilli()).coerceAtLeast(0)
                val totalSecs = ms / 1000
                val h = totalSecs / 3600
                val m = (totalSecs % 3600) / 60
                val sec = totalSecs % 60
                tvTrackedDuration.text = String.format("%dh %02dm %02ds", h, m, sec)
            } else {
                tvTrackedDuration.text = "—"
            }
        }

        fun updateStartDisplay() {
            etStartDate.setText(startDateTime?.format(dateDisplayFmt) ?: "")
            etStartTime.setText(startDateTime?.format(timeDisplayFmt) ?: "")
            updateDurationDisplay()
        }

        fun updateEndDisplay() {
            etEndDate.setText(endDateTime?.format(dateDisplayFmt) ?: "")
            etEndTime.setText(endDateTime?.format(timeDisplayFmt) ?: "")
            updateDurationDisplay()
        }

        updateStartDisplay()
        updateEndDisplay()
        etComment.setText(tracked.comment ?: "")

        // Archived: show notice, disable editing (delete stays available)
        if (isArchived) {
            cardArchivedNotice.visibility = View.VISIBLE
            layoutProject.isEnabled = false
            layoutStartDate.isEnabled = false
            layoutStartTime.isEnabled = false
            layoutEndDate.isEnabled = false
            layoutEndTime.isEnabled = false
            layoutComment.isEnabled = false
            btnSave.isEnabled = false
        } else {
            // Date/time picker listeners
            etStartDate.setOnClickListener {
                showDatePicker(startDateTime?.atZone(localZone)?.toInstant()?.toEpochMilli()) { date ->
                    startDateTime = startDateTime?.withYearMonthDay(date) ?: date.atStartOfDay()
                    updateStartDisplay()
                }
            }
            etStartTime.setOnClickListener {
                showTimePicker(startDateTime?.hour ?: 0, startDateTime?.minute ?: 0) { h, m ->
                    startDateTime = (startDateTime ?: LocalDateTime.now(localZone)).withHour(h).withMinute(m).withSecond(0)
                    updateStartDisplay()
                }
            }
            etEndDate.setOnClickListener {
                showDatePicker(endDateTime?.atZone(localZone)?.toInstant()?.toEpochMilli()) { date ->
                    endDateTime = endDateTime?.withYearMonthDay(date) ?: date.atStartOfDay()
                    updateEndDisplay()
                }
            }
            etEndTime.setOnClickListener {
                showTimePicker(endDateTime?.hour ?: 0, endDateTime?.minute ?: 0) { h, m ->
                    endDateTime = (endDateTime ?: LocalDateTime.now(localZone)).withHour(h).withMinute(m).withSecond(0)
                    updateEndDisplay()
                }
            }

            btnSave.setOnClickListener {
                val request = UpdateTrackedRequest(
                    id = tracked.id,
                    projectId = currentProjectId,
                    active = tracked.active,
                    start = startDateTime?.atZone(localZone)?.withZoneSameInstant(ZoneOffset.UTC)?.format(serverFmt),
                    end = endDateTime?.atZone(localZone)?.withZoneSameInstant(ZoneOffset.UTC)?.format(serverFmt),
                    timezone = tracked.timezone,
                    comment = etComment.text?.toString()
                )
                calendarViewModel.updateTracked(request)
            }
        }

        // Delete button — available regardless of archived state
        btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_tracked_delete_confirm_title)
                .setMessage(R.string.edit_tracked_delete_confirm_message)
                .setPositiveButton(R.string.edit_tracked_delete) { _, _ ->
                    calendarViewModel.deleteTracked(tracked.id)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        // Observe save result
        calendarViewModel.trackedUpdated.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                calendarViewModel.onUpdateHandled()
                dismiss()
            } else if (success == false) {
                calendarViewModel.onUpdateHandled()
            }
        }

        // Observe delete result
        calendarViewModel.trackedDeleted.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                calendarViewModel.onDeleteHandled()
                dismiss()
            } else if (success == false) {
                calendarViewModel.onDeleteHandled()
            }
        }
    }

    private fun showDatePicker(initialMillis: Long?, onPicked: (LocalDate) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(initialMillis ?: System.currentTimeMillis())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            onPicked(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate())
        }
        picker.show(childFragmentManager, "date_picker")
    }

    private fun showTimePicker(initialHour: Int, initialMinute: Int, onPicked: (Int, Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setHour(initialHour)
            .setMinute(initialMinute)
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .build()
        picker.addOnPositiveButtonClickListener { onPicked(picker.hour, picker.minute) }
        picker.show(childFragmentManager, "time_picker")
    }

    private fun LocalDateTime.withYearMonthDay(date: LocalDate): LocalDateTime =
        withYear(date.year).withMonth(date.monthValue).withDayOfMonth(date.dayOfMonth)
}
