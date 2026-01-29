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
            .inflate(R.layout.facility_list_item, parent, false)
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
            
            val userInfo = if (facility.zadnjiKorisnik != null) " • ${facility.zadnjiKorisnik}" else ""
            text2.text = "$location • $lastInspection$userInfo"
            
            // Apply font size preference and get theme preference
            val prefs = itemView.context.getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
            val fontSizeIndex = prefs.getInt("font_size", 0)
            val isDarkTheme = prefs.getBoolean("is_dark_theme", false)
            val baseSizeText1 = 16f
            val baseSizeText2 = 14f
            val multiplier = when (fontSizeIndex) {
                0 -> 1.0f
                1 -> 1.2f
                else -> 1.4f
            }
            text1.textSize = baseSizeText1 * multiplier
            text1.setTypeface(null, android.graphics.Typeface.BOLD)
            text2.textSize = baseSizeText2 * multiplier
            text2.setTypeface(null, android.graphics.Typeface.BOLD)
            
            val nameColor = if (isDarkTheme) {
                ContextCompat.getColor(itemView.context, android.R.color.white)
            } else {
                ContextCompat.getColor(itemView.context, R.color.very_dark_grey)
            }
            
            if (isOverdue) {
                text1.setTextColor(nameColor)
                text2.setTextColor(ContextCompat.getColor(itemView.context, R.color.overdue_red))
            } else {
                text1.setTextColor(nameColor)
                text2.setTextColor(ContextCompat.getColor(itemView.context, R.color.synced_green))
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
