package de.onemanprojects.klukka

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator

class ActiveTrackingFragment : Fragment() {

    private val viewModel: ActiveTrackingViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_active_tracking, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val event = mainViewModel.activeTracking.value
        if (event == null) return

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

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
