package de.onemanprojects.klukka

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import de.onemanprojects.klukka.model.ProjectListItem
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

private const val TAG = "ProjectsFragment"

class ProjectsFragment : Fragment() {

    private val viewModel: ProjectsViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: ProjectAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_projects, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_projects)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_empty)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_project)

        adapter = ProjectAdapter(emptyList()) { project ->
            AppLogger.i(TAG, "Project tapped: id=${project.id} title=${project.title}")
            viewModel.startTracking(project)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fab.setOnClickListener { /* placeholder */ }

        swipeRefresh.setOnRefreshListener {
            AppLogger.d(TAG, "Manual refresh triggered")
            viewModel.loadProjects()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
        }

        viewModel.projects.observe(viewLifecycleOwner) { sections ->
            val items = mutableListOf<ProjectListItem>()
            if (sections.own.isNotEmpty()) {
                items.add(ProjectListItem.Header(getString(R.string.section_own_projects)))
                sections.own.forEach { items.add(ProjectListItem.Entry(it)) }
            }
            if (sections.group.isNotEmpty()) {
                items.add(ProjectListItem.Header(getString(R.string.section_group_projects)))
                sections.group.forEach { items.add(ProjectListItem.Entry(it)) }
            }
            adapter.updateItems(items)
            tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Snackbar.make(requireView(), errorMsg, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.unauthorized.observe(viewLifecycleOwner) { isUnauthorized ->
            if (isUnauthorized == true) redirectToLogin()
        }

        viewModel.trackingStarted.observe(viewLifecycleOwner) { event ->
            if (event != null) {
                viewModel.onTrackingNavigated()
                mainViewModel.onTrackingStarted(event)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProjects()
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
