package de.onemanprojects.klukka

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProjectsActivity : AppCompatActivity() {

    private val viewModel: ProjectsViewModel by viewModels()
    private lateinit var adapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_projects)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.projects_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_projects)
        setSupportActionBar(toolbar)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_archived) {
                startActivity(Intent(this, ArchivedProjectsActivity::class.java))
                true
            } else false
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_projects)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val tvEmpty = findViewById<TextView>(R.id.tv_empty)
        val fab = findViewById<FloatingActionButton>(R.id.fab_add_project)

        adapter = ProjectAdapter(emptyList()) { project ->
            viewModel.startTracking(project)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fab.setOnClickListener {
            // Placeholder – add project functionality will be implemented later
        }

        viewModel.loading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.projects.observe(this) { projects ->
            adapter.updateProjects(projects)
            tvEmpty.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.unauthorized.observe(this) { isUnauthorized ->
            if (isUnauthorized == true) {
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
        }

        viewModel.trackingStarted.observe(this) { event ->
            if (event != null) {
                viewModel.onTrackingNavigated()
                val intent = Intent(this, ActiveTrackingActivity::class.java).apply {
                    putExtra(ActiveTrackingActivity.EXTRA_TRACKING_ID, event.trackingId)
                    putExtra(ActiveTrackingActivity.EXTRA_PROJECT_TITLE, event.project.title ?: "")
                    putExtra(ActiveTrackingActivity.EXTRA_PROJECT_COMMENT, event.project.description ?: "")
                    putExtra(ActiveTrackingActivity.EXTRA_START_TIME, event.startTime)
                }
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProjects()
    }
}
