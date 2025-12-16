package com.caloracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.caloracker.R
import com.caloracker.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Main activity of the Caloracker app.
 * Hosts navigation with bottom navigation bar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up navigation
        setupNavigation()
    }

    /**
     * Set up navigation controller and bottom navigation.
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up bottom navigation with nav controller
        val bottomNav: BottomNavigationView = binding.bottomNavigation
        bottomNav.setupWithNavController(navController)

        // Handle bottom nav visibility on different destinations
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.foodConfirmationFragment -> {
                    // Hide bottom nav on food confirmation screen
                    bottomNav.visibility = android.view.View.GONE
                }
                else -> {
                    // Show bottom nav on other screens
                    bottomNav.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    /**
     * Handle back button press.
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
