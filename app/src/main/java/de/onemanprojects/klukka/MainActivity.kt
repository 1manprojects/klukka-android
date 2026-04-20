package de.onemanprojects.klukka

import android.content.Intent
import android.net.Uri
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.materialswitch.MaterialSwitch
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val navHeader = navView.getHeaderView(0)
        val baseTopPadding = navHeader.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(navHeader) { view, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusBarTop + baseTopPadding)
            insets
        }

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Tracking item starts hidden — only shown when a session is active
        navView.menu.findItem(R.id.nav_tracking).isVisible = false

        // Dark mode toggle — initialise switch state then react to user changes
        val darkModeSwitch = navView.menu.findItem(R.id.nav_dark_mode)
            .actionView?.findViewById<MaterialSwitch>(R.id.switch_dark_mode)
        darkModeSwitch?.isChecked = appPreferences.isDarkMode
        darkModeSwitch?.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            appPreferences.isDarkMode = isChecked
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_projects -> showFragment(ProjectsFragment(), TAG_PROJECTS)
                R.id.nav_calendar -> showFragment(CalendarFragment(), TAG_CALENDAR)
                R.id.nav_archived -> showFragment(ArchivedProjectsFragment(), TAG_ARCHIVED)
                R.id.nav_activity -> showFragment(ActivityFragment(), TAG_ACTIVITY)
                R.id.nav_settings -> showFragment(SettingsFragment(), TAG_SETTINGS)
                R.id.nav_about -> showFragment(AboutFragment(), TAG_ABOUT)
                R.id.nav_tracking -> {
                    if (mainViewModel.activeTracking.value != null) {
                        showFragment(ActiveTrackingFragment(), TAG_TRACKING)
                    }
                }
                R.id.nav_dark_mode -> {
                    darkModeSwitch?.toggle()
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_github_app -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com")))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_github_backend -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com")))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener true
                }
            }
            item.isChecked = true
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Show/hide tracking item and navigate when tracking state changes
        mainViewModel.activeTracking.observe(this) { tracking ->
            val trackingItem = navView.menu.findItem(R.id.nav_tracking)
            if (tracking != null) {
                trackingItem.isVisible = true
            } else {
                trackingItem.isVisible = false
                // If currently on tracking screen, navigate back to projects
                if (navView.checkedItem?.itemId == R.id.nav_tracking) {
                    navView.setCheckedItem(R.id.nav_projects)
                    showFragment(ProjectsFragment(), TAG_PROJECTS)
                }
            }
        }

        // One-shot: navigate to the tracking screen when a session starts or is detected on startup.
        mainViewModel.pendingNavToTracking.observe(this) { event ->
            if (event != null) {
                navView.setCheckedItem(R.id.nav_tracking)
                showFragment(ActiveTrackingFragment(), TAG_TRACKING)
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

        if (savedInstanceState == null) {
            navView.setCheckedItem(R.id.nav_projects)
            showFragment(ProjectsFragment(), TAG_PROJECTS)
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
        private const val TAG_CALENDAR = "calendar"
        private const val TAG_ARCHIVED = "archived"
        private const val TAG_TRACKING = "tracking"
        private const val TAG_ACTIVITY = "activity"
        private const val TAG_SETTINGS = "settings"
        private const val TAG_ABOUT = "about"
    }
}
