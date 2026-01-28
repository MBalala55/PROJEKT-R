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
import com.example.elektropregled.databinding.FragmentFacilityListBinding
import com.example.elektropregled.ui.viewmodel.FacilityListViewModel
import com.example.elektropregled.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class FacilityListFragment : Fragment() {
    
    private var _binding: FragmentFacilityListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: FacilityListViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as ElektropregledApplication)
    }
    
    private lateinit var adapter: FacilityAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFacilityListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Display logged in username
        val app = requireActivity().application as ElektropregledApplication
        val username = app.tokenStorage.getUsername()
        binding.usernameText.text = username ?: "Korisnik"
        
        // Setup logout button
        binding.logoutButton.setOnClickListener {
            app.tokenStorage.clearToken()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
        
        adapter = FacilityAdapter { facility ->
            // Navigate to field list
            val fragment = FieldListFragment.newInstance(facility.idPostr, facility.nazPostr)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        
        binding.facilitiesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.facilitiesRecycler.adapter = adapter
        
        viewModel.loadFacilities()
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                
                if (state.errorMessage != null) {
                    binding.errorText.text = state.errorMessage
                    binding.errorText.visibility = View.VISIBLE
                    binding.emptyText.visibility = View.GONE
                } else {
                    binding.errorText.visibility = View.GONE
                }
                
                if (state.facilities.isEmpty() && !state.isLoading) {
                    binding.emptyText.visibility = View.VISIBLE
                } else {
                    binding.emptyText.visibility = View.GONE
                    adapter.submitList(state.facilities)
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
