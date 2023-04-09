package eim.project.mobile_banking_android_app

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import eim.project.mobile_banking_android_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var mCallbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null
    private var nVerificationId: String? = null
    private lateinit var firebaseAuth: FirebaseAuth

    private val TAG = "MAIN_TAG"

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.phone.visibility = View.VISIBLE
        binding.otp.visibility = View.GONE

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {

                progressDialog.dismiss()
                nVerificationId = verificationId
                forceResendingToken = token
                binding.phone.visibility = View.GONE
                binding.otp.visibility = View.VISIBLE
                Toast.makeText(this@MainActivity, "Verification code sent...", Toast.LENGTH_SHORT)
                    .show()
                binding.codeSentDescriptionTv.text = "Please enter the code we sent to ${binding.phonEt.text.toString().trim()}"
            }
        }

        binding.phoneContinueBtn.setOnClickListener {
            val phone = binding.phonEt.text.toString().trim()
            if (phone.isEmpty() || phone.length < 10) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
            } else {
                startPhoneNumberVerification(phone)
            }
        }

        binding.resendOtpTv.setOnClickListener {
            val phone = binding.phonEt.text.toString().trim()
            if (phone.isEmpty() || phone.length < 10) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
            } else {
                resendVerificationCode(phone, forceResendingToken)
            }
        }

        binding.otpContinueBtn.setOnClickListener {
            val code = binding.otpEt.text.toString().trim()
            if (code.isEmpty() || code.length < 6) {
                Toast.makeText(this, "Please enter verification code", Toast.LENGTH_SHORT).show()
            } else {
                verifyPhoneNumberWithCode(nVerificationId!!, code)
            }
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        progressDialog.setMessage("Verifying phone number...")
        progressDialog.show()
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallbacks!!)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendVerificationCode(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        progressDialog.setMessage("Resending code...")
        progressDialog.show()
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallbacks!!)
            .setForceResendingToken(token!!)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
        progressDialog.setMessage("Verifying code...")
        progressDialog.show()
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        Log.d(TAG, "verifyPhoneNumberWithCode: $credential")
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                progressDialog.dismiss()
                val firebaseUser = authResult.user
                val phone = firebaseUser!!.phoneNumber
                Toast.makeText(this, "Logged in as $phone", Toast.LENGTH_SHORT).show()
                // user is logged in
                val phoneNumberRef = Firebase.database.reference.
                child("users").
                child(firebaseUser.uid).
                child("phoneNumber")
                phoneNumberRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            phoneNumberRef.setValue(phone)
                            startActivity(Intent(this@MainActivity, ChangeNameActivity::class.java))
                        } else {
                            startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                        }
                        this@MainActivity.finish()
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.d(ContentValues.TAG, "onCancelled: $error")
                    }
                })
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
    }
}