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
import com.example.elektropregled.ui.viewmodel.FieldListViewModel
import com.example.elektropregled.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChecklistFragment : Fragment() {
    
    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null. Fragment view may have been destroyed.")
    
    private val viewModel: ChecklistViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as ElektropregledApplication)
    }
    
    private val fieldListViewModel: FieldListViewModel by activityViewModels {
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
        
        // Register this ViewModel with the parent FieldListViewModel
        fieldListViewModel.setChecklistViewModel(viewModel)
        
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
            },
            onNapomenaChanged = { uredajId, napomena ->
                viewModel.updateDeviceNapomena(uredajId, napomena)
            },
            getNapomena = { uredajId ->
                viewModel.getDeviceNapomena(uredajId)
            }
        )
        
        binding.checklistRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.checklistRecycler.adapter = adapter
        
        // Next field button
        binding.nextFieldButton.setOnClickListener {
            // Validate all required parameters
            val (isValid, errorMessage) = viewModel.validateAllRequiredParametersAreFilled()
            if (!isValid) {
                binding.errorText.text = errorMessage
                binding.errorText.visibility = View.VISIBLE
                
                // Hide error message after 3 seconds
                binding.errorText.postDelayed({
                    binding.errorText.visibility = View.GONE
                }, 3000)
                
                return@setOnClickListener
            }
            
            // Mark field as reviewed and save
            val poljeId = arguments?.getInt(ARG_POLJE_ID) ?: 0
            val postrojenjeId = arguments?.getInt(ARG_POSTROJENJE_ID) ?: 0
            
            // Navigate back after validation passed
            viewLifecycleOwner.lifecycleScope.launch {
                // Spremi podatke prije navigacije
                viewModel.saveFieldReview()
                
                // Označi polje kao pregledano
                if (poljeId != 0) {
                    // Uređaj u polju - koristi pozitivni ID polja
                    fieldListViewModel.markFieldAsReviewed(poljeId)
                } else {
                    // Uređaj direktno na postrojenju (poljeId = 0) - koristi negativni ID postrojenja
                    fieldListViewModel.markFieldAsReviewed(-postrojenjeId)
                }
                
                // Small delay to let the UI update
                delay(100)
                // Use the new back press handling
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                } else {
                    @Suppress("DEPRECATION")
                    requireActivity().onBackPressed()
                }
            }
        }
        
        // Use existing pregled from FieldListViewModel instead of creating a new one
        val pregledId = fieldListViewModel.getCurrentPregledId()
        if (pregledId != null) {
            viewModel.setPregledId(pregledId)
        } else {
            // Fallback: create new pregled only if FieldListViewModel doesn't have one
            viewModel.startInspection(postrojenjeId)
        }
        
        // Load checklist
        viewModel.loadChecklist(postrojenjeId, if (poljeId == 0) null else poljeId)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                
                if (state.errorMessage != null) {
                    binding.errorText.text = state.errorMessage
                    binding.errorText.visibility = View.VISIBLE
                } else {
                    binding.errorText.visibility = View.GONE
                }
                
                if (state.uredaji.isNotEmpty()) {
                    adapter.submitList(state.uredaji)
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
