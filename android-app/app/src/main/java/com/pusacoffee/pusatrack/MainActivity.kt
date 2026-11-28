package com.pusacoffee.pusatrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.pusacoffee.pusatrack.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val rootTabDestinations = setOf(
        R.id.homeFragment,
        R.id.inventoryFragment,
        R.id.cashierFragment,
        R.id.ordersFragment,
        R.id.historyFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupNavigation()
        setupBottomNavigation()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.navHostFragment.updatePadding(top = systemBars.top)
            binding.bottomNavWrapper.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isRootTab = destination.id in rootTabDestinations
            binding.bottomNavWrapper.isVisible = isRootTab
            if (isRootTab) {
                updateSelectedTabUi(destination.id)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.tabHome.setOnClickListener { navigateToTab(R.id.homeFragment) }
        binding.tabInventory.setOnClickListener { navigateToTab(R.id.inventoryFragment) }
        binding.tabCashierFab.setOnClickListener { navigateToTab(R.id.cashierFragment) }
        binding.tabCashierLabelArea.setOnClickListener { navigateToTab(R.id.cashierFragment) }
        binding.tabOrders.setOnClickListener { navigateToTab(R.id.ordersFragment) }
        binding.tabHistory.setOnClickListener { navigateToTab(R.id.historyFragment) }
    }

    fun navigateToTab(destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return

        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(R.id.homeFragment, inclusive = false, saveState = true)
            .build()

        navController.navigate(destinationId, null, navOptions)
    }

    private fun updateSelectedTabUi(activeId: Int) {
        val activeColor = ContextCompat.getColor(this, R.color.primary_forest)
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive)

        val homeActive = activeId == R.id.homeFragment
        binding.ivTabHome.setColorFilter(if (homeActive) activeColor else inactiveColor)
        binding.tvTabHome.setTextColor(if (homeActive) activeColor else inactiveColor)

        val inventoryActive = activeId == R.id.inventoryFragment
        binding.ivTabInventory.setColorFilter(if (inventoryActive) activeColor else inactiveColor)
        binding.tvTabInventory.setTextColor(if (inventoryActive) activeColor else inactiveColor)

        val cashierActive = activeId == R.id.cashierFragment
        binding.tvTabCashier.setTextColor(if (cashierActive) activeColor else inactiveColor)

        val ordersActive = activeId == R.id.ordersFragment
        binding.ivTabOrders.setColorFilter(if (ordersActive) activeColor else inactiveColor)
        binding.tvTabOrders.setTextColor(if (ordersActive) activeColor else inactiveColor)

        val historyActive = activeId == R.id.historyFragment
        binding.ivTabHistory.setColorFilter(if (historyActive) activeColor else inactiveColor)
        binding.tvTabHistory.setTextColor(if (historyActive) activeColor else inactiveColor)
    }
}
