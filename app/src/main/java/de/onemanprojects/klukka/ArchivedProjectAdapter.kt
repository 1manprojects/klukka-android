package de.onemanprojects.klukka

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.onemanprojects.klukka.model.Project

class ArchivedProjectAdapter(
    private var projects: List<Project>,
    private val onUnarchiveClick: (Project) -> Unit,
    private val onDeleteClick: (Project) -> Unit
) : RecyclerView.Adapter<ArchivedProjectAdapter.ArchivedProjectViewHolder>() {

    class ArchivedProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorAccent: View = itemView.findViewById(R.id.view_color_accent)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_project_title)
        val tvComment: TextView = itemView.findViewById(R.id.tv_project_comment)
        val btnUnarchive: ImageButton = itemView.findViewById(R.id.btn_unarchive)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_project)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchivedProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_archived_project, parent, false)
        return ArchivedProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArchivedProjectViewHolder, position: Int) {
        val project = projects[position]
        holder.tvTitle.text = project.title ?: ""
        holder.tvComment.text = project.description ?: ""
        holder.btnUnarchive.setOnClickListener { onUnarchiveClick(project) }
        holder.btnDelete.setOnClickListener { onDeleteClick(project) }
        val accentColor = try {
            if (!project.color.isNullOrBlank()) Color.parseColor(project.color)
            else Color.GRAY
        } catch (e: IllegalArgumentException) {
            Color.GRAY
        }
        holder.colorAccent.setBackgroundColor(accentColor)
    }

    override fun getItemCount(): Int = projects.size

    fun updateProjects(newProjects: List<Project>) {
        projects = newProjects
        notifyDataSetChanged()
    }
}
