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

/**
The `MainActivity` class is responsible for handling the login process in the mobile banking application.
It extends `AppCompatActivity` and manages the UI components, user input validation, and authentication logic using Firebase Authentication.

Methods:
    onCreate(savedInstanceState: Bundle?): Sets up the layout and UI components for the login screen.
    onStart(): Adds a listener to Firebase Authentication to check if the user is already logged in.
    startPhoneNumberVerification(phoneNumber: String): Sends a verification code to the user's phone number for authentication.
    resendVerificationCode(phoneNumber: String, token: PhoneAuthProvider.ForceResendingToken?): Sends a new verification code to the user's
    phone number in case the first code was not received or expired.
    verifyPhoneNumberWithCode(verificationId: String, code: String): Authenticates the user with the verification code received on their phone number.
    signInWithPhoneAuthCredential(credential: PhoneAuthCredential): Authenticates the user with the Firebase Authentication credentials received after
    successful verification of the verification code.
*/
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var mCallbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null
    private var nVerificationId: String? = null
    private lateinit var firebaseAuth: FirebaseAuth

    private val TAG = "MAIN_TAG"

    private lateinit var progressDialog: ProgressDialog


    /**
     * Called when the activity is starting. This is where most initialization
     * should go: calling setContentView(int) to inflate the activity's UI, using
     * findViewById to programmatically interact with widgets in the UI.
     *
     * @param savedInstanceState a Bundle containing the activity's previously
     * saved state, or null if the activity has no previously saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the activity_main layout and set it as the content view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show the phone number input field and hide the OTP input field
        binding.phone.visibility = View.VISIBLE
        binding.otp.visibility = View.GONE

        // Initialize Firebase authentication instance
        firebaseAuth = FirebaseAuth.getInstance()

        // Create a progress dialog to show when phone number verification is in progress
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        // Initialize a PhoneAuthProvider callback to listen for verification events
        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                // When verification is completed successfully, sign in the user with the received credential
                signInWithPhoneAuthCredential(phoneAuthCredential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // When verification fails, dismiss the progress dialog and show a toast message with the error message
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // When the verification code is sent, dismiss the progress dialog and update UI to show the OTP input field
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

        // Set a click listener for the phone number continue button
        binding.phoneContinueBtn.setOnClickListener {
            // When clicked, get the entered phone number and validate it. If it's valid, start phone number verification
            val phone = binding.phonEt.text.toString().trim()
            if (phone.isEmpty() || phone.length < 10) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
            } else {
                startPhoneNumberVerification(phone)
            }
        }

        // Set a click listener for the resend OTP button
        binding.resendOtpTv.setOnClickListener {
            // When clicked, get the entered phone number and validate it. If it's valid, resend the verification code
            val phone = binding.phonEt.text.toString().trim()
            if (phone.isEmpty() || phone.length < 10) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
            } else {
                resendVerificationCode(phone, forceResendingToken)
            }
        }

        // Set a click listener for the OTP continue button
        binding.otpContinueBtn.setOnClickListener {
            // When clicked, get the entered verification code and validate it. If it's valid, verify the phone number with the code
            val code = binding.otpEt.text.toString().trim()
            if (code.isEmpty() || code.length < 6) {
                Toast.makeText(this, "Please enter verification code", Toast.LENGTH_SHORT).show()
            } else {
                verifyPhoneNumberWithCode(nVerificationId!!, code)
            }
        }
    }


    /**
     * Starts the specified activity and sets the transition animation to slide in from the right
     * and slide out to the left.
     *
     * @param intent The intent to start the activity.
     */
    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)

        // Set the transition animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }


    /**
     * Initiates phone number verification with Firebase Authentication.
     *
     * @param phoneNumber The phone number to verify.
     */
    private fun startPhoneNumberVerification(phoneNumber: String) {
        // Show a progress dialog with a message to the user.
        progressDialog.setMessage("Verifying phone number...")
        progressDialog.show()

        // Set the phone number verification options using the Firebase Authentication API.
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallbacks!!)
            .build()

        // Initiate the phone number verification using the Firebase Authentication API.
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    /**
     * Resends the verification code to the specified phone number using the provided token.
     *
     * @param phoneNumber The phone number to send the verification code to.
     * @param token The token for resending the verification code (can be null if it's the first time).
     */
    private fun resendVerificationCode(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        // Show a progress dialog to indicate that the code is being resent
        progressDialog.setMessage("Resending code...")
        progressDialog.show()

        // Build the PhoneAuthOptions for resending the code
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)  // Set the phone number to send the code to
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)  // Set the timeout for code verification
            .setActivity(this)  // Set the current activity
            .setCallbacks(mCallbacks!!)  // Set the callback for code verification
            .setForceResendingToken(token!!)  // Set the token for resending the code
            .build()

        // Start the verification process with the new options
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    /**
     * Verify the phone number with the provided verification ID and verification code.
     *
     * Displays a progress dialog while verifying the phone number.
     *
     * @param verificationId the verification ID received via SMS
     * @param code the verification code entered by the user
     */
    private fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
        // Show progress dialog while verifying phone number
        progressDialog.setMessage("Verifying code...")
        progressDialog.show()

        // Create PhoneAuthCredential object with verification ID and code
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        Log.d(TAG, "verifyPhoneNumberWithCode: $credential")
        // Sign in with the PhoneAuthCredential
        signInWithPhoneAuthCredential(credential)
    }


    /**
     * Sign in with the provided phone authentication credential.
     * If authentication succeeds, check if the user exists in the Firebase database.
     * If the user does not exist, add the user's phone number to the database and start the
     * "ChangeNameActivity". If the user already exists, start the "DashboardActivity".
     *
     * @param credential Phone authentication credential.
     */
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        // Show progress dialog while signing in
        progressDialog.setMessage("Signing in...")
        progressDialog.show()

        // Authenticate with Firebase using the provided phone authentication credential
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                // If authentication is successful
                progressDialog.dismiss()

                // Get the Firebase user and phone number
                val firebaseUser = authResult.user
                val phone = firebaseUser!!.phoneNumber
                Toast.makeText(this, "Logged in as $phone", Toast.LENGTH_SHORT).show()

                // Check if the user exists in the Firebase database
                val phoneNumberRef = Firebase.database.reference
                    .child("users")
                    .child(firebaseUser.uid)
                    .child("phoneNumber")
                phoneNumberRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        // If the user does not exist in the database
                        if (!dataSnapshot.exists()) {
                            // Add the user's phone number to the database
                            phoneNumberRef.setValue(phone)
                            // Start the "ChangeNameActivity"
                            startActivity(Intent(this@MainActivity, ChangeNameActivity::class.java))
                        } else {
                            // If the user already exists in the database, start the "DashboardActivity"
                            startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                        }
                        // Close the current activity
                        this@MainActivity.finish()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d(ContentValues.TAG, "onCancelled: $error")
                    }
                })
            }
            .addOnFailureListener { e ->
                // If authentication fails, dismiss the progress dialog and show an error message
                progressDialog.dismiss()
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
    }

}