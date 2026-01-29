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
import com.example.elektropregled.databinding.FragmentFieldListBinding
import com.example.elektropregled.ui.viewmodel.FieldListViewModel
import com.example.elektropregled.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class FieldListFragment : Fragment() {
    
    private var _binding: FragmentFieldListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: FieldListViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as ElektropregledApplication)
    }
    
    private lateinit var adapter: FieldAdapter
    
    companion object {
        private const val ARG_POSTROJENJE_ID = "postrojenje_id"
        private const val ARG_POSTROJENJE_NAME = "postrojenje_name"
        
        fun newInstance(postrojenjeId: Int, postrojenjeName: String): FieldListFragment {
            return FieldListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_POSTROJENJE_ID, postrojenjeId)
                    putString(ARG_POSTROJENJE_NAME, postrojenjeName)
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFieldListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val postrojenjeId = arguments?.getInt(ARG_POSTROJENJE_ID) ?: return
        val postrojenjeName = arguments?.getString(ARG_POSTROJENJE_NAME) ?: ""
        
        binding.toolbar.title = "$postrojenjeName - ${getString(R.string.fields_title)}"
        
        adapter = FieldAdapter(
            onItemClick = { field ->
                // Ako je idPolje null, koristi 0 kao marker za "Direktno na postrojenju"
                val poljeId = field.idPolje ?: 0
                val fragment = ChecklistFragment.newInstance(postrojenjeId, poljeId, field.nazPolje)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            postrojenjeId = postrojenjeId
        )
        
        binding.fieldsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.fieldsRecycler.adapter = adapter
        
        binding.finishInspectionButton.setOnClickListener {
            viewModel.finishInspection()
        }
        
        viewModel.loadFields(postrojenjeId)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                
                if (state.errorMessage != null) {
                    binding.errorText.text = state.errorMessage
                    binding.errorText.visibility = View.VISIBLE
                } else {
                    binding.errorText.visibility = View.GONE
                }
                
                // Update button text with progress
                val totalFields = state.fields.size
                val reviewedFields = state.reviewedFieldsCount
                binding.finishInspectionButton.text = 
                    "Završi pregled ($reviewedFields/$totalFields)"
                
                // DEBUG: Log button state
                android.util.Log.d("FieldListFragment", "Button state: totalFields=$totalFields, reviewedFields=$reviewedFields, isSaving=${state.isSaving}, enabled=${totalFields > 0 && reviewedFields == totalFields && !state.isSaving}")
                
                // Enable button only if all fields are reviewed
                binding.finishInspectionButton.isEnabled = 
                    totalFields > 0 && reviewedFields == totalFields && !state.isSaving
                
                if (state.saveSuccess) {
                    viewModel.resetSaveSuccess()
                    requireActivity().onBackPressed()
                }
                
                // Update adapter with reviewed fields
                adapter.setReviewedFields(state.reviewedFieldIds)
                adapter.submitList(state.fields)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
