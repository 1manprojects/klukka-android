package de.onemanprojects.klukka

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator

class ActiveTrackingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRACKING_ID = "extra_tracking_id"
        const val EXTRA_PROJECT_TITLE = "extra_project_title"
        const val EXTRA_PROJECT_COMMENT = "extra_project_comment"
        const val EXTRA_START_TIME = "extra_start_time"
    }

    private val viewModel: ActiveTrackingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_active_tracking)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.active_tracking_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val trackingId = intent.getIntExtra(EXTRA_TRACKING_ID, -1)
        val projectTitle = intent.getStringExtra(EXTRA_PROJECT_TITLE) ?: ""
        val projectComment = intent.getStringExtra(EXTRA_PROJECT_COMMENT) ?: ""
        val startTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())

        val tvTitle = findViewById<TextView>(R.id.tv_tracking_title)
        val tvComment = findViewById<TextView>(R.id.tv_tracking_comment)
        val tvElapsed = findViewById<TextView>(R.id.tv_elapsed_time)
        val progressSeconds = findViewById<CircularProgressIndicator>(R.id.progress_seconds)
        val btnStop = findViewById<FloatingActionButton>(R.id.btn_stop)

        tvTitle.text = projectTitle
        tvComment.text = projectComment

        viewModel.startTimer(startTime)

        viewModel.elapsedSeconds.observe(this) { seconds ->
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            tvElapsed.text = String.format("%02d:%02d:%02d", h, m, s)
            progressSeconds.setProgressCompat((seconds % 60).toInt(), true)
        }

        btnStop.setOnClickListener {
            if (trackingId != -1) {
                viewModel.stopTracking(trackingId)
            }
        }

        viewModel.trackingStopped.observe(this) { stopped ->
            if (stopped == true) {
                finish()
            }
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
    }
}
