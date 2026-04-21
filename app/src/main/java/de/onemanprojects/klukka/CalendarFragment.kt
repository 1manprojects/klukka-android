package de.onemanprojects.klukka

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import de.onemanprojects.klukka.model.Tracked

private const val TAG = "CalendarFragment"

class CalendarFragment : Fragment() {

    private val viewModel: CalendarViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_calendar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tv_calendar_title)
        val btnPrev = view.findViewById<ImageButton>(R.id.btn_prev)
        val btnNext = view.findViewById<ImageButton>(R.id.btn_next)
        val btnToday = view.findViewById<MaterialButton>(R.id.btn_today)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_view_type)
        val calendarGrid = view.findViewById<CalendarGridView>(R.id.calendar_grid)
        val scrollView = view.findViewById<ScrollView>(R.id.scroll_calendar)
        val progressBar = view.findViewById<LinearProgressIndicator>(R.id.progress_loading)
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fab_add)

        // Pre-select Week to match ViewModel default
        toggleGroup.check(R.id.btn_week)

        btnPrev.setOnClickListener { viewModel.navigatePrev() }
        btnNext.setOnClickListener { viewModel.navigateNext() }
        btnToday.setOnClickListener { viewModel.navigateToday() }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btn_day -> viewModel.setViewType(CalendarViewType.DAY)
                R.id.btn_week -> viewModel.setViewType(CalendarViewType.WEEK)
                R.id.btn_work_week -> viewModel.setViewType(CalendarViewType.WORK_WEEK)
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            tvTitle.text = state.title
            calendarGrid.setDays(state.days)
        }

        viewModel.data.observe(viewLifecycleOwner) { analysisData ->
            val tracked = analysisData?.tracked ?: emptyList()
            val projects = (analysisData?.projects ?: emptyList()) +
                    (analysisData?.groupProjects ?: emptyList())
            calendarGrid.setData(tracked, projects)
        }

        calendarGrid.onTrackedClickListener = { tracked ->
            val analysisData = viewModel.data.value
            val allProjects = (analysisData?.projects ?: emptyList()) +
                    (analysisData?.groupProjects ?: emptyList())
            EditTrackedDialogFragment.newInstance(tracked, allProjects)
                .show(childFragmentManager, "edit_tracked")
        }

        fabAdd.setOnClickListener {
            val analysisData = viewModel.data.value
            val allProjects = (analysisData?.projects ?: emptyList()) +
                    (analysisData?.groupProjects ?: emptyList())
            val template = Tracked(
                id = -1,
                projectId = allProjects.firstOrNull { !it.archived }?.id ?: 0,
                active = false,
                start = null,
                end = null,
                timezone = null,
                comment = null
            )
            EditTrackedDialogFragment.newInstance(template, allProjects)
                .show(childFragmentManager, "add_tracked")
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
            }
        }

        // Scroll to 7 AM after layout so the user sees working hours by default
        scrollView.post {
            val hourHeightPx = 56f * resources.displayMetrics.density
            scrollView.scrollTo(0, (hourHeightPx * 7).toInt())
        }
    }
}
