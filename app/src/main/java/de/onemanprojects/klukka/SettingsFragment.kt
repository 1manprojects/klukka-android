package de.onemanprojects.klukka

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import de.onemanprojects.klukka.model.Group
import de.onemanprojects.klukka.model.UserApiToken

private const val TAG = "SettingsFragment"

class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_settings)
        val tvEmail = view.findViewById<TextView>(R.id.tv_email)
        val llTokens = view.findViewById<LinearLayout>(R.id.ll_tokens)
        val tvNoTokens = view.findViewById<TextView>(R.id.tv_no_tokens)
        val llGroups = view.findViewById<LinearLayout>(R.id.ll_groups)
        val tvNoGroups = view.findViewById<TextView>(R.id.tv_no_groups)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btn_logout)
        val btnDeleteAccount = view.findViewById<MaterialButton>(R.id.btn_delete_account)
        val toggleTheme = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_theme)

        val appPreferences = AppPreferences(requireContext())
        val initialButton = when (appPreferences.themeMode) {
            AppPreferences.THEME_LIGHT -> R.id.btn_theme_light
            AppPreferences.THEME_DARK -> R.id.btn_theme_dark
            else -> R.id.btn_theme_system
        }
        toggleTheme.check(initialButton)

        toggleTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.btn_theme_light -> {
                    appPreferences.themeMode = AppPreferences.THEME_LIGHT
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                R.id.btn_theme_dark -> {
                    appPreferences.themeMode = AppPreferences.THEME_DARK
                    AppCompatDelegate.MODE_NIGHT_YES
                }
                else -> {
                    appPreferences.themeMode = AppPreferences.THEME_SYSTEM
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        swipeRefresh.setOnRefreshListener {
            AppLogger.d(TAG, "Manual refresh triggered")
            viewModel.loadUserData()
        }

        btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.logout_btn) { _, _ -> viewModel.logout() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
        }

        viewModel.userData.observe(viewLifecycleOwner) { userData ->
            tvEmail.text = userData?.user?.mail ?: ""

            val tokens = userData?.tokens ?: emptyList()
            populateTokens(llTokens, tvNoTokens, tokens)

            val groups = userData?.groups ?: emptyList()
            populateGroups(llGroups, tvNoGroups, groups)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Snackbar.make(requireView(), errorMsg, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.unauthorized.observe(viewLifecycleOwner) { isUnauthorized ->
            if (isUnauthorized == true) redirectToLogin()
        }

        viewModel.accountDeleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted == true) redirectToLogin()
        }

        viewModel.loggedOut.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut == true) redirectToLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserData()
    }

    private fun populateTokens(
        container: LinearLayout,
        tvEmpty: TextView,
        tokens: List<UserApiToken>
    ) {
        container.removeAllViews()
        if (tokens.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
            tokens.forEach { token ->
                val itemView = layoutInflater.inflate(R.layout.item_token, container, false)
                itemView.findViewById<TextView>(R.id.tv_token_description).text =
                    token.description?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.token_no_description)
                itemView.findViewById<ImageButton>(R.id.btn_delete_token).setOnClickListener {
                    confirmDeleteToken(token)
                }
                container.addView(itemView)
            }
        }
    }

    private fun populateGroups(
        container: LinearLayout,
        tvEmpty: TextView,
        groups: List<Group>
    ) {
        container.removeAllViews()
        if (groups.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
            groups.forEach { group ->
                val itemView = layoutInflater.inflate(R.layout.item_group_setting, container, false)
                itemView.findViewById<TextView>(R.id.tv_group_title).text =
                    group.title ?: getString(R.string.group_no_title)
                val tvDesc = itemView.findViewById<TextView>(R.id.tv_group_description)
                if (!group.description.isNullOrBlank()) {
                    tvDesc.text = group.description
                    tvDesc.visibility = View.VISIBLE
                } else {
                    tvDesc.visibility = View.GONE
                }
                itemView.findViewById<ImageButton>(R.id.btn_leave_group).setOnClickListener {
                    confirmLeaveGroup(group)
                }
                container.addView(itemView)
            }
        }
    }

    private fun confirmDeleteToken(token: UserApiToken) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_token_title)
            .setMessage(R.string.delete_token_message)
            .setPositiveButton(R.string.delete_token_confirm_btn) { _, _ ->
                AppLogger.d(TAG, "Delete token confirmed: id=${token.id}")
                viewModel.deleteToken(token.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmLeaveGroup(group: Group) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.leave_group_title)
            .setMessage(getString(R.string.leave_group_message, group.title ?: ""))
            .setPositiveButton(R.string.leave_group_btn) { _, _ ->
                AppLogger.d(TAG, "Leave group confirmed: id=${group.id}")
                viewModel.leaveGroup(group)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteAccountConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_account_title)
            .setMessage(R.string.delete_account_message_1)
            .setPositiveButton(R.string.delete_account_continue) { _, _ ->
                showDeleteAccountFinalConfirmation()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteAccountFinalConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_account_confirm_title)
            .setMessage(R.string.delete_account_message_2)
            .setPositiveButton(R.string.delete_account_confirm_btn) { _, _ ->
                AppLogger.i(TAG, "Account deletion confirmed by user")
                viewModel.deleteAccount()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
