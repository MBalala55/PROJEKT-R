package com.example.elektropregled.ui.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.elektropregled.ElektropregledApplication
import com.example.elektropregled.R
import com.example.elektropregled.databinding.FragmentLoginBinding
import com.example.elektropregled.ui.viewmodel.LoginViewModel
import com.example.elektropregled.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import android.util.Log

class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LoginViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as ElektropregledApplication)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.loginButton.setOnClickListener {
            val username = binding.usernameEdit.text?.toString() ?: ""
            val password = binding.passwordEdit.text?.toString() ?: ""
            viewModel.login(username, password)
        }
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                try {
                    binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.loginButton.isEnabled = !state.isLoading
                    
                    if (state.errorMessage != null) {
                        binding.errorText.text = state.errorMessage
                        binding.errorText.visibility = View.VISIBLE
                        Log.e("LoginFragment", "Error: ${state.errorMessage}")
                    } else {
                        binding.errorText.visibility = View.GONE
                    }
                    
                    if (state.isSuccess) {
                        Log.d("LoginFragment", "Login successful, navigating...")
                        // Reset success state to prevent multiple navigations
                        viewModel.clearSuccess()
                        // Navigate to facility list
                        if (isAdded && !requireActivity().isFinishing) {
                            try {
                                requireActivity().supportFragmentManager.beginTransaction()
                                    .replace(R.id.fragment_container, FacilityListFragment())
                                    .commitAllowingStateLoss()
                                Log.d("LoginFragment", "Navigation successful")
                            } catch (e: Exception) {
                                Log.e("LoginFragment", "Navigation error", e)
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginFragment", "Error in state collection", e)
                    e.printStackTrace()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
