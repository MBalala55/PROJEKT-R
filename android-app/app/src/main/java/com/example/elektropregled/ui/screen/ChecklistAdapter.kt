package com.example.elektropregled.ui.screen

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.elektropregled.R
import com.example.elektropregled.data.api.dto.ChecklistParametar
import com.example.elektropregled.data.api.dto.ChecklistUredaj
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ChecklistAdapter(
    private val onValueChanged: (Int, Int, Any?) -> Unit,
    private val getValue: (Int, Int) -> Any?
) : ListAdapter<ChecklistUredaj, ChecklistAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist_device, parent, false)
        return ViewHolder(view, onValueChanged, getValue)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        itemView: View,
        private val onValueChanged: (Int, Int, Any?) -> Unit,
        private val getValue: (Int, Int) -> Any?
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val deviceName: TextView = itemView.findViewById(R.id.device_name)
        private val deviceInfo: TextView = itemView.findViewById(R.id.device_info)
        private val parametersContainer: LinearLayout = itemView.findViewById(R.id.parameters_container)
        
        fun bind(uredaj: ChecklistUredaj) {
            deviceName.text = "${uredaj.natpPlocica} - ${uredaj.nazVrUred}"
            deviceInfo.text = "${uredaj.tvBroj}"
            
            parametersContainer.removeAllViews()
            
            uredaj.parametri.sortedBy { it.redoslijed }.forEach { parametar ->
                val paramView = createParameterView(parametar, uredaj.idUred)
                parametersContainer.addView(paramView)
            }
        }
        
        private fun createParameterView(parametar: ChecklistParametar, uredajId: Int): View {
            val context = itemView.context
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
            }
            
            val nameText = TextView(context).apply {
                text = parametar.nazParametra
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            layout.addView(nameText)
            
            if (parametar.opis != null) {
                val descText = TextView(context).apply {
                    text = parametar.opis
                    textSize = 12f
                    setTextColor(context.getColor(android.R.color.darker_gray))
                }
                layout.addView(descText)
            }
            
            when (parametar.tipPodataka) {
                "BOOLEAN" -> {
                    val switch = Switch(context).apply {
                        text = context.getString(R.string.ok)
                        isChecked = (getValue(uredajId, parametar.idParametra) as? Boolean) ?: (parametar.defaultVrijednostBool ?: true)
                        setOnCheckedChangeListener { _, isChecked ->
                            onValueChanged(uredajId, parametar.idParametra, isChecked)
                        }
                    }
                    layout.addView(switch)
                }
                "NUMERIC" -> {
                    val inputLayout = TextInputLayout(context).apply {
                        hint = "${parametar.nazParametra} (${parametar.mjernaJedinica})"
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    val editText = TextInputEditText(context).apply {
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                        val currentValue = getValue(uredajId, parametar.idParametra) as? Double
                        setText(currentValue?.toString() ?: parametar.defaultVrijednostNum?.toString() ?: "")
                        setOnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus) {
                                val text = text?.toString()
                                if (text.isNullOrBlank()) {
                                    onValueChanged(uredajId, parametar.idParametra, null)
                                } else {
                                    try {
                                        val value = text.toDouble()
                                        if (parametar.minVrijednost != null && value < parametar.minVrijednost) {
                                            inputLayout.error = "Min: ${parametar.minVrijednost}"
                                        } else if (parametar.maxVrijednost != null && value > parametar.maxVrijednost) {
                                            inputLayout.error = "Max: ${parametar.maxVrijednost}"
                                        } else {
                                            inputLayout.error = null
                                            onValueChanged(uredajId, parametar.idParametra, value)
                                        }
                                    } catch (e: NumberFormatException) {
                                        inputLayout.error = "Neispravna vrijednost"
                                    }
                                }
                            }
                        }
                    }
                    inputLayout.addView(editText)
                    layout.addView(inputLayout)
                }
                "TEXT" -> {
                    val inputLayout = TextInputLayout(context).apply {
                        hint = parametar.nazParametra
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    val editText = TextInputEditText(context).apply {
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        val currentValue = getValue(uredajId, parametar.idParametra) as? String
                        setText(currentValue ?: parametar.defaultVrijednostTxt ?: "")
                        setOnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus) {
                                onValueChanged(uredajId, parametar.idParametra, text?.toString())
                            }
                        }
                    }
                    inputLayout.addView(editText)
                    layout.addView(inputLayout)
                }
            }
            
            return layout
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<ChecklistUredaj>() {
        override fun areItemsTheSame(oldItem: ChecklistUredaj, newItem: ChecklistUredaj): Boolean {
            return oldItem.idUred == newItem.idUred
        }
        
        override fun areContentsTheSame(oldItem: ChecklistUredaj, newItem: ChecklistUredaj): Boolean {
            return oldItem == newItem
        }
    }
}
