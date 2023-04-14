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
            val phoneNumber = firebaseUser.phoneNumber
            lifecycleScope.launch(Dispatchers.IO) {
                addContactToPhone(this@ChangeNameActivity, name, phoneNumber!!)
            }
            currentUserRef.child("name").setValue(name)
                .addOnSuccessListener {
                    Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@ChangeNameActivity, DashboardActivity::class.java))
                    this.finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error updating name", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private suspend fun addContactToPhone(context: Context, name: String, phoneNumber: String): Boolean {
        val permission = WRITE_CONTACTS
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(permission), 100)
            return false
        }

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
                val contentResolver: ContentResolver = context.contentResolver
                contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
                true
            } catch (e: Exception) {
                false
            }
        }
    }



}