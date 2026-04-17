package de.onemanprojects.klukka

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.onemanprojects.klukka.model.Project
import de.onemanprojects.klukka.model.ProjectListItem

class ProjectAdapter(
    private var items: List<ProjectListItem>,
    private val onProjectClick: (Project) -> Unit,
    private val onArchiveClick: (Project) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHeader: TextView = itemView.findViewById(R.id.tv_section_header)
    }

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorAccent: View = itemView.findViewById(R.id.view_color_accent)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_project_title)
        val tvComment: TextView = itemView.findViewById(R.id.tv_project_comment)
        val btnArchive: ImageButton = itemView.findViewById(R.id.btn_archive)
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ProjectListItem.Header -> VIEW_TYPE_HEADER
        is ProjectListItem.Entry -> VIEW_TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_section_header, parent, false)
            )
            else -> EntryViewHolder(
                inflater.inflate(R.layout.item_project, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ProjectListItem.Header -> (holder as HeaderViewHolder).tvHeader.text = item.title
            is ProjectListItem.Entry -> {
                val project = item.project
                (holder as EntryViewHolder).apply {
                    itemView.setOnClickListener { onProjectClick(project) }
                    tvTitle.text = project.title ?: ""
                    tvComment.text = project.description ?: ""
                    val color = try {
                        if (!project.color.isNullOrBlank()) Color.parseColor(project.color)
                        else Color.GRAY
                    } catch (e: IllegalArgumentException) {
                        Color.GRAY
                    }
                    colorAccent.setBackgroundColor(color)
                    if (item.isOwn) {
                        btnArchive.visibility = View.VISIBLE
                        btnArchive.setOnClickListener { onArchiveClick(project) }
                    } else {
                        btnArchive.visibility = View.GONE
                        btnArchive.setOnClickListener(null)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ProjectListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ENTRY = 1
    }
}
