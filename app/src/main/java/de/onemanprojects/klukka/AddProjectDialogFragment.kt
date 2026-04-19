package de.onemanprojects.klukka

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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

    private var selectedColor: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_add_project, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutTitle = view.findViewById<TextInputLayout>(R.id.layout_title)
        val etTitle = view.findViewById<TextInputEditText>(R.id.et_title)
        val etDescription = view.findViewById<TextInputEditText>(R.id.et_description)
        val colorPickerRow = view.findViewById<ViewGroup>(R.id.ll_color_picker)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btn_create)

        buildColorPicker(colorPickerRow)

        btnCreate.setOnClickListener {
            val title = etTitle.text?.toString()?.trim() ?: ""
            if (title.isEmpty()) {
                layoutTitle.error = getString(R.string.project_title_required)
                return@setOnClickListener
            }
            layoutTitle.error = null
            val description = etDescription.text?.toString() ?: ""
            viewModel.addProject(title, description, selectedColor ?: "")
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

    private fun buildColorPicker(container: ViewGroup) {
        val d = resources.displayMetrics.density
        val sizePx = (40 * d).toInt()
        val marginPx = (6 * d).toInt()
        val strokeWidthPx = (3 * d).toInt()

        val dotViews = mutableListOf<View>()

        PRESET_COLORS.forEach { hex ->
            val dot = View(requireContext())
            dot.layoutParams = ViewGroup.MarginLayoutParams(sizePx, sizePx).apply {
                marginEnd = marginPx
            }
            dot.background = buildDotDrawable(hex, false, strokeWidthPx)
            dot.setOnClickListener {
                selectedColor = hex
                dotViews.forEach { v ->
                    val isSelected = (v.tag as? String) == hex
                    v.background = buildDotDrawable(v.tag as String, isSelected, strokeWidthPx)
                }
            }
            dot.tag = hex
            dotViews.add(dot)
            container.addView(dot)
        }
    }

    private fun buildDotDrawable(hex: String, selected: Boolean, strokeWidthPx: Int): GradientDrawable {
        val color = try { Color.parseColor(hex) } catch (_: Exception) { Color.LTGRAY }
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (selected) setStroke(strokeWidthPx, Color.WHITE)
        }
    }

    companion object {
        private val PRESET_COLORS = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
            "#4CAF50", "#8BC34A", "#FF9800", "#795548"
        )
    }
}
