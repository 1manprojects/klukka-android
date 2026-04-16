package de.onemanprojects.klukka

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appPreferences = AppPreferences(this)
        AppLogger.isDebugEnabled = appPreferences.isDebugLogging

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        bottomNav = findViewById(R.id.bottom_nav)

        // Tracking tab starts hidden — only shown when a session is active
        bottomNav.menu.findItem(R.id.nav_tracking).isVisible = false

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_projects -> showFragment(ProjectsFragment(), TAG_PROJECTS)
                R.id.nav_archived -> showFragment(ArchivedProjectsFragment(), TAG_ARCHIVED)
                R.id.nav_tracking -> {
                    if (mainViewModel.activeTracking.value != null) {
                        showFragment(ActiveTrackingFragment(), TAG_TRACKING)
                    }
                }
            }
            true
        }

        // Show/hide tracking tab and navigate when tracking state changes
        mainViewModel.activeTracking.observe(this) { tracking ->
            val trackingItem = bottomNav.menu.findItem(R.id.nav_tracking)
            if (tracking != null) {
                trackingItem.isVisible = true
            } else {
                trackingItem.isVisible = false
                // If currently on tracking tab, navigate back to projects
                if (bottomNav.selectedItemId == R.id.nav_tracking) {
                    bottomNav.selectedItemId = R.id.nav_projects
                }
            }
        }

        // One-shot: navigate to tracking fragment when a session starts
        mainViewModel.pendingNavToTracking.observe(this) { event ->
            if (event != null) {
                bottomNav.menu.findItem(R.id.nav_tracking).isVisible = true
                showFragment(ActiveTrackingFragment(), TAG_TRACKING)
                bottomNav.selectedItemId = R.id.nav_tracking
                mainViewModel.onNavigatedToTracking()
            }
        }

        mainViewModel.unauthorized.observe(this) { isUnauthorized ->
            if (isUnauthorized == true) {
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
        }

        // Load initial fragment only on first creation (not on config change).
        // selectedItemId triggers the listener which calls showFragment — do NOT also
        // call showFragment directly or two fragment instances will be created and the
        // first one's coroutine will be cancelled before it finishes.
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_projects
            mainViewModel.checkActiveTracking()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_debug_logging).isChecked = AppLogger.isDebugEnabled
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_debug_logging) {
            val newState = !item.isChecked
            item.isChecked = newState
            AppLogger.isDebugEnabled = newState
            appPreferences.isDebugLogging = newState
            val msg = if (newState) getString(R.string.debug_on) else getString(R.string.debug_off)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
    }

    companion object {
        private const val TAG_PROJECTS = "projects"
        private const val TAG_ARCHIVED = "archived"
        private const val TAG_TRACKING = "tracking"
    }
}
