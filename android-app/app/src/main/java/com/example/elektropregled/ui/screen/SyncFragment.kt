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
import com.example.elektropregled.databinding.FragmentSyncBinding
import com.example.elektropregled.data.repository.SyncResult
import com.example.elektropregled.ui.viewmodel.SyncViewModel
import com.example.elektropregled.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class SyncFragment : Fragment() {
    
    private var _binding: FragmentSyncBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SyncViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as ElektropregledApplication)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSyncBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.syncButton.setOnClickListener {
            viewModel.syncNow()
        }
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.pendingCount.text = getString(R.string.sync_pending, state.pendingCount)
                binding.syncedCount.text = getString(R.string.sync_synced, state.syncedCount)
                binding.failedCount.text = getString(R.string.sync_failed, state.failedCount)
                
                binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.syncButton.isEnabled = !state.isLoading && state.pendingCount > 0
                
                if (state.errorMessage != null) {
                    binding.errorText.text = state.errorMessage
                    binding.errorText.visibility = View.VISIBLE
                    binding.successText.visibility = View.GONE
                } else {
                    binding.errorText.visibility = View.GONE
                }
                
                if (state.lastSyncResult is SyncResult.Success) {
                    val result = state.lastSyncResult
                    binding.successText.text = getString(
                        R.string.sync_success
                    ) + " (${result.synced} sinkronizirano, ${result.failed} neuspješno)"
                    binding.successText.visibility = View.VISIBLE
                } else {
                    binding.successText.visibility = View.GONE
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
