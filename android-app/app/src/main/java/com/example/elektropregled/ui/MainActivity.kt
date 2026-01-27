package com.example.elektropregled.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.elektropregled.ElektropregledApplication
import com.example.elektropregled.R
import com.example.elektropregled.data.TokenStorage
import com.example.elektropregled.ui.screen.LoginFragment
import com.example.elektropregled.ui.screen.FacilityListFragment
import com.example.elektropregled.ui.viewmodel.ViewModelFactory

class MainActivity : AppCompatActivity() {
    
    private lateinit var tokenStorage: TokenStorage
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tokenStorage = (application as ElektropregledApplication).tokenStorage
        
        // Check if user is logged in
        if (tokenStorage.isTokenValid()) {
            showFacilityList()
        } else {
            showLogin()
        }
    }
    
    private fun showLogin() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }
    
    private fun showFacilityList() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FacilityListFragment())
            .commit()
    }
}
