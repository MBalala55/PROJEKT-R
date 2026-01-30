package com.example.elektropregled.ui.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.elektropregled.ElektropregledApplication
import com.example.elektropregled.R
import com.example.elektropregled.databinding.FragmentFacilityListBinding
import com.example.elektropregled.data.api.dto.PostrojenjeSummary
import com.example.elektropregled.ui.viewmodel.FacilityListViewModel
import com.example.elektropregled.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

class FacilityListFragment : Fragment() {
    
    private var _binding: FragmentFacilityListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: FacilityListViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as ElektropregledApplication)
    }
    
    private lateinit var adapter: FacilityAdapter
    private var currentSort = "date" // "name" or "date"
    private var currentFacilities: List<PostrojenjeSummary> = emptyList()
    
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
        
        // Setup menu button
        binding.menuButton.setOnClickListener {
            showMenu(binding.menuButton)
        }
        
        // Setup sort buttons
        binding.sortAbcButton.setOnClickListener {
            currentSort = "name"
            applySortToList()
        }
        
        binding.sortDateButton.setOnClickListener {
            currentSort = "date"
            applySortToList()
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
                // Check if binding is still valid (view might be destroyed)
                val currentBinding = _binding ?: return@collect
                
                currentBinding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                
                if (state.errorMessage != null) {
                    currentBinding.errorText.text = state.errorMessage
                    currentBinding.errorText.visibility = View.VISIBLE
                    currentBinding.emptyText.visibility = View.GONE
                } else {
                    currentBinding.errorText.visibility = View.GONE
                }
                
                if (state.facilities.isEmpty() && !state.isLoading) {
                    currentBinding.emptyText.visibility = View.VISIBLE
                } else {
                    currentBinding.emptyText.visibility = View.GONE
                    currentFacilities = state.facilities
                    applySortToList()
                }
            }
        }
    }
    
    private fun applySortToList() {
        // Use Croatian locale collator for proper sorting of Croatian letters (č, ć, đ, š, ž)
        val collator = Collator.getInstance(Locale("hr", "HR"))
        
        val sorted = when (currentSort) {
            "date" -> currentFacilities.sortedByDescending { it.zadnjiPregled }
            else -> currentFacilities.sortedWith { a, b ->
                collator.compare(a.nazPostr, b.nazPostr)
            }
        }
        adapter.submitList(sorted)
    }
    
    private fun toggleTheme() {
        val prefs = requireContext().getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
        val isDarkTheme = prefs.getBoolean("is_dark_theme", false)
        
        // Toggle and save
        prefs.edit().putBoolean("is_dark_theme", !isDarkTheme).apply()
        
        // Recreate activity to apply theme
        requireActivity().recreate()
    }
    
    private fun toggleFontSize() {
        val prefs = requireContext().getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
        val currentSize = prefs.getInt("font_size", 0) // 0 = normal, 1 = large, 2 = extra large
        val nextSize = (currentSize + 1) % 3
        
        // Save preference
        prefs.edit().putInt("font_size", nextSize).apply()
        
        // Refresh adapter to apply new font sizes
        adapter.notifyDataSetChanged()
    }
    
    private fun showMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_facility_list, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_theme -> {
                    toggleTheme()
                    true
                }
                R.id.menu_font_size -> {
                    toggleFontSize()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
