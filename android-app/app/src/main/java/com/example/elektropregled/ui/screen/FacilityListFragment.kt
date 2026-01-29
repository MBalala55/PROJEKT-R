package com.example.elektropregled.ui.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
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
        
        // Setup menu button
        binding.menuButton.setOnClickListener {
            showMenu(binding.menuButton)
        }
        
        // Setup sort buttons
        binding.sortAbcButton.setOnClickListener {
            currentSort = "name"
            updateSortButtonStyles()
            applySortToList()
        }
        
        binding.sortDateButton.setOnClickListener {
            currentSort = "date"
            updateSortButtonStyles()
            applySortToList()
        }
        
        // Initialize button styles
        updateSortButtonStyles()
        
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
    
    private fun updateSortButtonStyles() {
        if (currentSort == "date") {
            // DATUM is active (white pill with purple text)
            binding.sortDateButton.apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.active_button_background)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_700))
            }
            // ABECEDNO is inactive (white text on purple bg)
            binding.sortAbcButton.apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.inactive_button_background)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        } else {
            // ABECEDNO is active (white pill with purple text)
            binding.sortAbcButton.apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.active_button_background)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_700))
            }
            // DATUM is inactive (white text on purple bg)
            binding.sortDateButton.apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.inactive_button_background)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }
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
        
        // Update theme button text based on current theme
        val prefs = requireContext().getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
        val isDarkTheme = prefs.getBoolean("is_dark_theme", false)
        val themeMenuTitle = if (isDarkTheme) "Light Mode" else "Dark Mode"
        popup.menu.findItem(R.id.menu_theme).title = themeMenuTitle
        
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
                R.id.menu_logout -> {
                    val app = requireActivity().application as ElektropregledApplication
                    app.tokenStorage.clearToken()
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LoginFragment())
                        .commit()
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
