package eim.project.mobile_banking_android_app

import android.Manifest.permission.WRITE_CONTACTS
import android.app.Activity
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import eim.project.mobile_banking_android_app.databinding.ActivityChangeNameBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * An activity to change the user's name and update it in Firebase.
 * It also adds the user's name and phone number as a contact in the user's device.

Attributes:
- binding: ActivityChangeNameBinding - binding for the layout of this activity

Methods:
- onCreate(savedInstanceState: Bundle?): Unit
Initializes the activity, sets the content view and click listener for the save button. Calls `updateNameInFirebase()` when the button is clicked.
- updateNameInFirebase(): Unit
Updates the user's name in Firebase and adds the user's name and phone number as a contact in the user's device.
- addContactToPhone(context: Context, name: String, phoneNumber: String): Boolean
Adds the user's name and phone number as a contact in the user's device. Returns true if successful, false otherwise.
 */
class ChangeNameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangeNameBinding

    /**
     * Override function called when the activity is first created or recreated after being destroyed.
    It sets the content view to the inflated layout, initializes the binding object,
    and sets a click listener on the save button to call the updateNameInFirebase() function.

     * param savedInstanceState: the bundle of saved instance state
     * return: None
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.saveButton.setOnClickListener {
            updateNameInFirebase()
        }
    }

    /**
     * Updates the user's name in Firebase and adds a new contact to the phone's contact list
     * with the new name and user's phone number.
     */
    private fun updateNameInFirebase() {
        val name = binding.nameEditText.text.toString().trim()
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        // If a user is currently signed in
        if (firebaseUser != null) {
            // Get a reference to the current user's data in Firebase Realtime Database
            val currentUserRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(firebaseUser.uid)
            val phoneNumber = firebaseUser.phoneNumber

            // Launch a coroutine on the IO dispatcher to add a new contact to the phone's contact list
            lifecycleScope.launch(Dispatchers.IO) {
                addContactToPhone(this@ChangeNameActivity, name, phoneNumber!!)
            }

            // Update the user's name in Firebase Realtime Database
            currentUserRef.child("name").setValue(name)
                .addOnSuccessListener {
                    Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show()
                    // Navigate back to the dashboard activity
                    startActivity(Intent(this@ChangeNameActivity, DashboardActivity::class.java))
                    this.finish()
                }
                .addOnFailureListener {
                    // Show an error message
                    Toast.makeText(this, "Error updating name", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Adds a contact with the given name and phone number to the phone's contacts.
     *
     * @param context The context to use.
     * @param name The name of the contact to add.
     * @param phoneNumber The phone number of the contact to add.
     * @return True if the contact was successfully added, false otherwise.
     */
    private suspend fun addContactToPhone(context: Context, name: String, phoneNumber: String): Boolean {
        // Check if we have permission to write contacts
        val permission = WRITE_CONTACTS
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            // If we don't have permission, request it
            ActivityCompat.requestPermissions(context as Activity, arrayOf(permission), 100)
            return false
        }

        // Create the operations to insert the contact
        val operations = ArrayList<ContentProviderOperation>()
        operations.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build())

        operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            .build())

        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            .build())

        return withContext(Dispatchers.IO) {
            try {
                // Apply the batch of operations to insert the contact
                val contentResolver: ContentResolver = context.contentResolver
                contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
                true
            } catch (e: Exception) {
                false
            }
        }
    }



}