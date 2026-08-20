package de.onemanprojects.klukka

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class ActiveTrackingFragment : Fragment() {

    private val viewModel: ActiveTrackingViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private var autostopReceiver: BroadcastReceiver? = null
    private var autostopDialog: AlertDialog? = null
    private var autostopCountdown: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_active_tracking, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val event = mainViewModel.activeTracking.value
        if (event == null) return

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_tracking)
        val tvTitle = view.findViewById<TextView>(R.id.tv_tracking_title)
        val tvComment = view.findViewById<TextView>(R.id.tv_tracking_comment)
        val tvElapsed = view.findViewById<TextView>(R.id.tv_elapsed_time)
        val progressSeconds = view.findViewById<CircularProgressIndicator>(R.id.progress_seconds)
        val btnStop = view.findViewById<FloatingActionButton>(R.id.btn_stop)
        val etComment = view.findViewById<TextInputEditText>(R.id.et_comment)

        tvTitle.text = event.project.title ?: ""
        tvComment.text = event.project.description ?: ""
        if (event.comment.isNotBlank()) {
            etComment.setText(event.comment)
        }

        // Add TextWatcher after setting initial text to avoid a spurious debounce trigger
        etComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onCommentChanged(event.trackingId, s?.toString() ?: "")
            }
        })

        AppLogger.i("ActiveTrackingFragment", "onViewCreated: event.startTime=${event.startTime} now=${System.currentTimeMillis()} expectedElapsed=${(System.currentTimeMillis() - event.startTime) / 1000}s")
        viewModel.startTimer(event.startTime)

        viewModel.elapsedSeconds.observe(viewLifecycleOwner) { seconds ->
            AppLogger.i("ActiveTrackingFragment", "elapsedSeconds observer: seconds=$seconds")
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            tvElapsed.text = String.format("%02d:%02d:%02d", h, m, s)
            progressSeconds.setProgressCompat((seconds % 60).toInt(), true)
        }

        // When a pull-to-refresh fetches a corrected start time from the server, restart the timer.
        mainViewModel.activeTracking.observe(viewLifecycleOwner) { updated ->
            if (updated != null && updated.startTime != event.startTime) {
                viewModel.startTimer(updated.startTime)
            }
        }

        swipeRefresh.setOnRefreshListener {
            mainViewModel.refreshActiveTracking()
        }

        mainViewModel.trackingRefreshing.observe(viewLifecycleOwner) { refreshing ->
            swipeRefresh.isRefreshing = refreshing == true
        }

        btnStop.setOnClickListener {
            viewModel.stopTracking(event.trackingId)
        }

        viewModel.trackingStopped.observe(viewLifecycleOwner) { stopped ->
            if (stopped == true) {
                mainViewModel.onTrackingStopped()
            }
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
        val filter = IntentFilter(TrackingAlarmReceiver.ACTION_AUTOSTOP_WARNING)
        autostopReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                showAutostopDialog()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(autostopReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            requireContext().registerReceiver(autostopReceiver, filter)
        }
        // Show dialog if alarm fired while the fragment was not visible
        val prefs = AppPreferences(requireContext())
        if (prefs.autostopPending) {
            showAutostopDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        autostopReceiver?.let {
            try { requireContext().unregisterReceiver(it) } catch (_: IllegalArgumentException) {}
        }
        autostopReceiver = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autostopCountdown?.cancel()
        autostopDialog?.dismiss()
    }

    private fun showAutostopDialog() {
        if (!isAdded) return
        if (autostopDialog?.isShowing == true) return
        val event = mainViewModel.activeTracking.value ?: return
        val projectName = event.project.title ?: ""

        AppPreferences(requireContext()).autostopPending = false

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.autostop_dialog_title)
            .setMessage(getString(R.string.autostop_dialog_message, projectName, 30))
            .setPositiveButton(R.string.autostop_keep_tracking) { d, _ ->
                autostopCountdown?.cancel()
                d.dismiss()
            }
            .setCancelable(false)
            .create()

        autostopDialog = dialog
        dialog.show()

        autostopCountdown = object : CountDownTimer(30_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000L).toInt() + 1
                dialog.setMessage(getString(R.string.autostop_dialog_message, projectName, seconds))
            }
            override fun onFinish() {
                dialog.dismiss()
                viewModel.stopTracking(event.trackingId)
            }
        }.start()
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
