package eim.project.mobile_banking_android_app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import eim.project.mobile_banking_android_app.databinding.ActivityCardDetailsBinding
import eim.project.mobile_banking_android_app.databinding.DialogChangePinBinding
import eim.project.mobile_banking_android_app.home.Card
import eim.project.mobile_banking_android_app.transactions.accounts.SavingsAccountFragment
import eim.project.mobile_banking_android_app.transactions.transfers.TransferFragment
import java.util.*

class CardDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCardDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras ?: return
        val cardNumber = extras.getString("number", "")
        val expiryDate = extras.getString("expiryDate", "")
        val cardHolderName = extras.getString("name", "")
        val cardCvv = extras.getString("cvv", "")

        // Access the views inside the CardView and set their text
        binding.creditCard.cardNumber.text = cardNumber.chunked(4).joinToString(" ")
        binding.creditCard.expiryDate.text = expiryDate
        binding.creditCard.cardHolderName.text = cardHolderName.uppercase(Locale.ROOT)
        binding.creditCard.cvv.text = cardCvv
        binding.creditCard.progressBar.visibility = View.GONE

        binding.cardContainer.setOnLongClickListener {
            val dialogView = DialogChangePinBinding.inflate(layoutInflater)

            val builder = AlertDialog.Builder(this)
            builder.setView(dialogView.root)
            builder.setPositiveButton("Change", null)
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val currentPin = dialogView.currentPinEdittext.text.toString()
                    val newPin = dialogView.newPinEdittext.text.toString()
                    updateCardPinCodeDatabase(currentPin, newPin, cardNumber, dialog)
                }
            }
            dialog.show()
            true
        }

        val bundle = Bundle().apply {
            putString("currentCardNumber", cardNumber)
        }
        val savingsAccountFragment = SavingsAccountFragment()
        savingsAccountFragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.savings_accounts_fragment, savingsAccountFragment)
            .commit()
        val transfersFragment = TransferFragment()
        transfersFragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.transfers_fragment, transfersFragment)
            .commit()

    }

    private fun updateCardPinCodeDatabase(currentPin: String, newPin: String, currentCardNumber: String, dialog: AlertDialog) {
        if (currentPin == newPin) {
            Toast.makeText(applicationContext, "New PIN must be different!", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentPin.length < 4 || newPin.length < 4) {
            Toast.makeText(applicationContext, "PIN must be at least 4 digits!", Toast.LENGTH_SHORT).show()
            return
        }
        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val cardsRef = FirebaseDatabase.getInstance().getReference("users/$currentUserId/cards")
        val query = cardsRef.orderByChild("number").equalTo(currentCardNumber)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var successfulUpdate = false
                for (cardSnapshot in dataSnapshot.children) {
                    val card = cardSnapshot.getValue(Card::class.java)
                    if (card?.pin == currentPin) {
                        // update the pin code of the card
                        val cardRef = cardsRef.child(cardSnapshot.key!!)
                        cardRef.child("pin").setValue(newPin)
                        Toast.makeText(applicationContext, "PIN updated!", Toast.LENGTH_SHORT).show()
                        successfulUpdate = true
                    } else {
                        Toast.makeText(applicationContext, "Incorrect PIN!", Toast.LENGTH_SHORT).show()
                    }
                }
                if (successfulUpdate) {
                    dialog.dismiss()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    // In your Dashboard activity
    override fun onBackPressed() {
        super.onBackPressed()
        // Set the transition animation
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

}