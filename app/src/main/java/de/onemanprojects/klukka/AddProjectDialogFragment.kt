package de.onemanprojects.klukka

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AddProjectDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: ProjectsViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_add_project, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutTitle = view.findViewById<TextInputLayout>(R.id.layout_title)
        val etTitle = view.findViewById<TextInputEditText>(R.id.et_title)
        val etDescription = view.findViewById<TextInputEditText>(R.id.et_description)
        val etColor = view.findViewById<TextInputEditText>(R.id.et_color)
        val colorPreview = view.findViewById<View>(R.id.view_color_preview)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btn_create)

        // Circular color preview
        val previewDrawable = GradientDrawable().apply { shape = GradientDrawable.OVAL }
        colorPreview.background = previewDrawable

        fun updateColorPreview(hex: String) {
            val color = try {
                val raw = if (hex.startsWith("#")) hex else "#$hex"
                Color.parseColor(raw)
            } catch (_: Exception) {
                Color.LTGRAY
            }
            previewDrawable.setColor(color)
        }
        updateColorPreview("")

        etColor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                updateColorPreview(s?.toString() ?: "")
            }
        })

        btnCreate.setOnClickListener {
            val title = etTitle.text?.toString()?.trim() ?: ""
            if (title.isEmpty()) {
                layoutTitle.error = getString(R.string.project_title_required)
                return@setOnClickListener
            }
            layoutTitle.error = null
            val description = etDescription.text?.toString() ?: ""
            val colorRaw = etColor.text?.toString()?.trim() ?: ""
            val color = if (colorRaw.isNotEmpty()) {
                if (colorRaw.startsWith("#")) colorRaw else "#$colorRaw"
            } else ""
            viewModel.addProject(title, description, color)
        }

        viewModel.projectCreated.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                viewModel.onProjectCreatedHandled()
                dismiss()
            } else if (success == false) {
                viewModel.onProjectCreatedHandled()
            }
        }
    }
}
