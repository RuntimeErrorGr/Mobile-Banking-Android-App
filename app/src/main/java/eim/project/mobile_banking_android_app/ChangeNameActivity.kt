package eim.project.mobile_banking_android_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import eim.project.mobile_banking_android_app.databinding.ActivityChangeNameBinding

class ChangeNameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangeNameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.saveButton.setOnClickListener {
            updateNameInFirebase()
        }
    }

    private fun updateNameInFirebase() {
        val name = binding.nameEditText.text.toString().trim { it <= ' ' }
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val currentUserRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(firebaseUser.uid)
            currentUserRef.child("name").setValue(name)
                .addOnSuccessListener {
                    Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    this.finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error updating name", Toast.LENGTH_SHORT).show()
                }
        }
    }
}