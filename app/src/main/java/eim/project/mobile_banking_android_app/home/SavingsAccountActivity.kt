package eim.project.mobile_banking_android_app.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eim.project.mobile_banking_android_app.databinding.ActivitySavingsAccountsBinding

class SavingsAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySavingsAccountsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavingsAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }


}