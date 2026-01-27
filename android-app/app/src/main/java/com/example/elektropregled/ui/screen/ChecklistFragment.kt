package com.example.elektropregled.ui.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.elektropregled.ElektropregledApplication
import com.example.elektropregled.R
import com.example.elektropregled.databinding.FragmentChecklistBinding
import com.example.elektropregled.ui.viewmodel.ChecklistViewModel
import com.example.elektropregled.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class ChecklistFragment : Fragment() {
    
    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ChecklistViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as ElektropregledApplication)
    }
    
    private lateinit var adapter: ChecklistAdapter
    
    companion object {
        private const val ARG_POSTROJENJE_ID = "postrojenje_id"
        private const val ARG_POLJE_ID = "polje_id"
        private const val ARG_POLJE_NAME = "polje_name"
        
        fun newInstance(postrojenjeId: Int, poljeId: Int?, poljeName: String): ChecklistFragment {
            return ChecklistFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_POSTROJENJE_ID, postrojenjeId)
                    putInt(ARG_POLJE_ID, poljeId ?: 0)
                    putString(ARG_POLJE_NAME, poljeName)
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val postrojenjeId = arguments?.getInt(ARG_POSTROJENJE_ID) ?: return
        val poljeId = arguments?.getInt(ARG_POLJE_ID)
        val poljeName = arguments?.getString(ARG_POLJE_NAME) ?: ""
        
        binding.toolbar.title = poljeName
        
        adapter = ChecklistAdapter(
            onValueChanged = { uredajId, parametarId, value ->
                viewModel.updateParameterValue(uredajId, parametarId, value)
            },
            getValue = { uredajId, parametarId ->
                viewModel.getParameterValue(uredajId, parametarId)
            }
        )
        
        binding.checklistRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.checklistRecycler.adapter = adapter
        
        // Start inspection
        viewModel.startInspection(postrojenjeId)
        
        // Load checklist
        viewModel.loadChecklist(postrojenjeId, if (poljeId == 0) null else poljeId)
        
        binding.finishButton.setOnClickListener {
            viewModel.saveInspection()
        }
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.finishButton.isEnabled = !state.isLoading && !state.isSaving
                
                if (state.errorMessage != null) {
                    binding.errorText.text = state.errorMessage
                    binding.errorText.visibility = View.VISIBLE
                } else {
                    binding.errorText.visibility = View.GONE
                }
                
                if (state.uredaji.isNotEmpty()) {
                    adapter.submitList(state.uredaji)
                }
                
                if (state.saveSuccess) {
                    // Navigate back or show success message
                    requireActivity().onBackPressed()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
