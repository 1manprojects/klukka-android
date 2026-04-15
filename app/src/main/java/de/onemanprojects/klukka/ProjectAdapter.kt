package de.onemanprojects.klukka

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.onemanprojects.klukka.model.Project

class ProjectAdapter(
    private var projects: List<Project>,
    private val onProjectClick: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorAccent: View = itemView.findViewById(R.id.view_color_accent)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_project_title)
        val tvComment: TextView = itemView.findViewById(R.id.tv_project_comment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]

        holder.itemView.setOnClickListener { onProjectClick(project) }
        holder.tvTitle.text = project.title ?: ""
        holder.tvComment.text = project.description ?: ""

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
