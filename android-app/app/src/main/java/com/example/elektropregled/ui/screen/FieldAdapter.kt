package com.example.elektropregled.ui.screen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.elektropregled.R
import com.example.elektropregled.data.api.dto.PoljeDto

class FieldAdapter(
    private val onItemClick: (PoljeDto) -> Unit
) : ListAdapter<PoljeDto, FieldAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val field = getItem(position)
        holder.bind(field, onItemClick)
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)
        
        fun bind(field: PoljeDto, onItemClick: (PoljeDto) -> Unit) {
            text1.text = field.nazPolje
            val voltage = if (field.napRazina != null) "${field.napRazina} kV" else ""
            text2.text = "$voltage • ${itemView.context.getString(R.string.devices_count, field.brojUredaja)}"
            
            itemView.setOnClickListener { onItemClick(field) }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<PoljeDto>() {
        override fun areItemsTheSame(oldItem: PoljeDto, newItem: PoljeDto): Boolean {
            return oldItem.idPolje == newItem.idPolje
        }
        
        override fun areContentsTheSame(oldItem: PoljeDto, newItem: PoljeDto): Boolean {
            return oldItem == newItem
        }
    }
}
