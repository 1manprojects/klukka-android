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

class ArchivedProjectsActivity : AppCompatActivity() {

    private val viewModel: ArchivedProjectsViewModel by viewModels()
    private lateinit var adapter: ArchivedProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_archived_projects)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.archived_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_archived)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recyclerView = findViewById<RecyclerView>(R.id.rv_archived_projects)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar_archived)
        val tvEmpty = findViewById<TextView>(R.id.tv_empty_archived)

        adapter = ArchivedProjectAdapter(emptyList()) { project ->
            viewModel.unarchiveProject(project.id)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

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

        viewModel.loadArchivedProjects()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
