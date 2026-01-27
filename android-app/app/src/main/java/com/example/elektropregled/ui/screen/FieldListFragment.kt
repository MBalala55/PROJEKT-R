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
        
        adapter = FieldAdapter { field ->
            // Navigate to checklist
            val fragment = ChecklistFragment.newInstance(postrojenjeId, field.idPolje, field.nazPolje)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        
        binding.fieldsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.fieldsRecycler.adapter = adapter
        
        viewModel.loadFields(postrojenjeId)
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                
                if (state.errorMessage != null) {
                    binding.errorText.text = state.errorMessage
                    binding.errorText.visibility = View.VISIBLE
                } else {
                    binding.errorText.visibility = View.GONE
                }
                
                adapter.submitList(state.fields)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
