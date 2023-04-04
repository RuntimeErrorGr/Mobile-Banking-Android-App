package eim.project.mobile_banking_android_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import eim.project.mobile_banking_android_app.databinding.ActivityDashboardBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var checkForNewCardsJob: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_dashboard)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_transactions, R.id.navigation_payments
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        checkForNewCards()
    }

    private fun checkForNewCards() {
        var hasCards = false
        val currentUser = FirebaseAuth.getInstance().currentUser
        checkForNewCardsJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(2000) // Wait for 5 seconds before checking again

                val snapshot = Firebase.database.reference
                    .child("users")
                    .child(currentUser!!.uid)
                    .child("cards")
                    .get()
                    .await()

                if (snapshot.exists() && !hasCards) {
                    // The user has new cards, notify the main thread
                    withContext(Dispatchers.Main) {
                        binding.textViewDashboard.text = "New cards added!"
                    }
                    hasCards = true
                } else if (!snapshot.exists()) {
                    // The user has no cards, notify the main thread
                    withContext(Dispatchers.Main) {
                        binding.textViewDashboard.text = "No cards added yet!"
                    }
                    hasCards = false
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the coroutine to avoid leaks
        checkForNewCardsJob.cancel()
    }


}