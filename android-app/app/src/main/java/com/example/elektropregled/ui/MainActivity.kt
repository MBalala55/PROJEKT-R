package com.example.elektropregled.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.elektropregled.ElektropregledApplication
import com.example.elektropregled.R
import com.example.elektropregled.data.TokenStorage
import com.example.elektropregled.ui.screen.LoginFragment
import com.example.elektropregled.ui.screen.FacilityListFragment

class MainActivity : AppCompatActivity() {
    
    private lateinit var tokenStorage: TokenStorage
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme before setting content view
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDarkTheme = prefs.getBoolean("is_dark_theme", false)
        if (isDarkTheme) {
            setTheme(R.style.Theme_Elektropregled_Dark)
        } else {
            setTheme(R.style.Theme_Elektropregled)
        }
        
        setContentView(R.layout.activity_main)
        
        tokenStorage = (application as ElektropregledApplication).tokenStorage
        
        // Check if user is logged in
        if (tokenStorage.isTokenValid()) {
            showFacilityList()
        } else {
            showLogin()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Trigger auto-sync when app comes to foreground
        if (tokenStorage.isTokenValid()) {
            (application as ElektropregledApplication).triggerAutoSync()
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
