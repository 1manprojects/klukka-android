package de.onemanprojects.klukka

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

private const val TAG = "ArchivedFragment"

class ArchivedProjectsFragment : Fragment() {

    private val viewModel: ArchivedProjectsViewModel by viewModels()
    private lateinit var adapter: ArchivedProjectAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_archived_projects, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_archived)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_archived_projects)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_empty_archived)

        adapter = ArchivedProjectAdapter(
            projects = emptyList(),
            onUnarchiveClick = { project ->
                AppLogger.d(TAG, "Unarchive tapped: id=${project.id} title=${project.title}")
                viewModel.unarchiveProject(project.id)
            },
            onDeleteClick = { project ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_project_confirm_title)
                    .setMessage(R.string.delete_project_confirm_message)
                    .setPositiveButton(R.string.delete_project) { _, _ ->
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.delete_project_confirm2_title)
                            .setMessage(R.string.delete_project_confirm2_message)
                            .setPositiveButton(R.string.delete_project_confirm2_btn) { _, _ ->
                                AppLogger.d(TAG, "Delete confirmed (2/2): id=${project.id} title=${project.title}")
                                viewModel.deleteProject(project.id)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            AppLogger.d(TAG, "Manual refresh triggered")
            viewModel.loadArchivedProjects()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
        }

        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            adapter.updateProjects(projects)
            tvEmpty.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Snackbar.make(requireView(), errorMsg, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.unauthorized.observe(viewLifecycleOwner) { isUnauthorized ->
            if (isUnauthorized == true) redirectToLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadArchivedProjects()
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
