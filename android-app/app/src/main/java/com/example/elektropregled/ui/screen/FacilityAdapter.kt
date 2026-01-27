package com.example.elektropregled.ui.screen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.elektropregled.R
import com.example.elektropregled.data.api.dto.PostrojenjeSummary
import com.example.elektropregled.ui.viewmodel.FacilityListViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FacilityAdapter(
    private val onItemClick: (PostrojenjeSummary) -> Unit
) : ListAdapter<PostrojenjeSummary, FacilityAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val facility = getItem(position)
        holder.bind(facility, onItemClick)
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)
        
        fun bind(facility: PostrojenjeSummary, onItemClick: (PostrojenjeSummary) -> Unit) {
            text1.text = facility.nazPostr
            val location = facility.lokacija ?: ""
            val lastInspection = if (facility.zadnjiPregled != null) {
                try {
                    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    val date = LocalDateTime.parse(facility.zadnjiPregled, formatter)
                    val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    date.format(displayFormatter)
                } catch (e: Exception) {
                    facility.zadnjiPregled
                }
            } else {
                itemView.context.getString(R.string.no_inspections)
            }
            
            val isOverdue = facility.zadnjiPregled == null || 
                java.time.temporal.ChronoUnit.MONTHS.between(
                    LocalDateTime.parse(facility.zadnjiPregled, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    LocalDateTime.now()
                ) >= 1
            
            text2.text = "$location • $lastInspection"
            
            if (isOverdue) {
                text1.setTextColor(ContextCompat.getColor(itemView.context, R.color.overdue_red))
            } else {
                text1.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.primary_text_light))
            }
            
            itemView.setOnClickListener { onItemClick(facility) }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<PostrojenjeSummary>() {
        override fun areItemsTheSame(oldItem: PostrojenjeSummary, newItem: PostrojenjeSummary): Boolean {
            return oldItem.idPostr == newItem.idPostr
        }
        
        override fun areContentsTheSame(oldItem: PostrojenjeSummary, newItem: PostrojenjeSummary): Boolean {
            return oldItem == newItem
        }
    }
}
