package com.example.elektropregled.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.elektropregled.ElektropregledApplication
import com.example.elektropregled.data.repository.*

class ViewModelFactory(private val app: ElektropregledApplication) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(app.tokenStorage) as T
            }
            modelClass.isAssignableFrom(FacilityListViewModel::class.java) -> {
                FacilityListViewModel(app.postrojenjeRepository) as T
            }
            modelClass.isAssignableFrom(FieldListViewModel::class.java) -> {
                FieldListViewModel(app.postrojenjeRepository, app.pregledRepository) as T
            }
            modelClass.isAssignableFrom(ChecklistViewModel::class.java) -> {
                ChecklistViewModel(
                    app.checklistRepository,
                    app.pregledRepository
                ) as T
            }
            modelClass.isAssignableFrom(SyncViewModel::class.java) -> {
                SyncViewModel(app.pregledRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
