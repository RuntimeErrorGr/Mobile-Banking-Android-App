package eim.project.mobile_banking_android_app.home

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import eim.project.mobile_banking_android_app.CardDetailsActivity
import eim.project.mobile_banking_android_app.MainActivity
import eim.project.mobile_banking_android_app.databinding.DialogAddCardBinding
import eim.project.mobile_banking_android_app.databinding.FragmentHomeBinding
import eim.project.mobile_banking_android_app.transactions.accounts.SavingsAccount
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var dialogView: DialogAddCardBinding
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var adapter: CardAdapter
    private var context: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.recicleView.layoutManager = LinearLayoutManager(requireContext())
        dialogView = DialogAddCardBinding.inflate(layoutInflater)
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        binding.logoutBtn.setOnClickListener() {
            firebaseAuth.signOut()
            checkUser()
        }

        binding.addCreditCardBtn.setOnClickListener {

            dialogView.dateEditText.setOnClickListener {
                showDatePicker()
            }

            dialogView.root.parent?.let { (it as ViewGroup).removeAllViews() }
            val builder = AlertDialog.Builder(requireContext())
            builder.setView(dialogView.root)
            builder.setPositiveButton("Add", null)
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val cardNumber = dialogView.numberEditText.text.toString()
                    val nameOnCard = dialogView.nameEditText.text.toString()
                    val expirationDate = dialogView.dateEditText.text.toString()
                    val cvv = dialogView.cvvEditText.text.toString()
                    if (validateCardDetalisInput(cardNumber, nameOnCard, expirationDate, cvv)) {
                        val card = Card(cardNumber, nameOnCard, expirationDate, cvv)
                        val iban = "RO${(1..14).map { (0..9).random() }.joinToString("")}".padEnd(16, '0')
                        card.savingsAccounts.add(SavingsAccount(
                            name="Main account",
                            iban=iban,
                            isMain = true,
                            isDeposit = false,
                            cardNumber = card.number
                        ))
                        addCardToDatabase(card, dialog)
                    }
                }
            }
            dialog.show()
        }

        loadCardsList()
        return root
    }

    private fun showDatePicker() {
        val datePicker = DatePickerFragment { _, month, year -> onDateSelected(month, year) }
        datePicker.show(parentFragmentManager, "datePicker")
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            // user not logged in, go to main activity
            activity?.startActivity(Intent(activity, MainActivity::class.java))
            activity?.finish()
        } else {
            // TODO: user is logged in, get user info
        }
    }

    private fun addCardToDatabase(card: Card, dialog: AlertDialog) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val cardsRef = FirebaseDatabase.getInstance().getReference("users/${currentUser?.uid}/cards")
        val query = cardsRef.orderByChild("number").equalTo(card.number)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var successfulUpdate = false
                if (dataSnapshot.exists()) {
                    // a card with this number already exists in the database
                    Toast.makeText(requireContext(), "A card with this number already exists", Toast.LENGTH_SHORT).show()
                } else {
                    // add the card to the database
                    val cardId = cardsRef.push().key
                    if (cardId != null) {
                        cardsRef.child(cardId).setValue(card)
                        Toast.makeText(requireContext(), "Card successfully registered", Toast.LENGTH_SHORT).show()
                        successfulUpdate = true
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


    private fun validateCardDetalisInput(
        cardNumber: String,
        nameOnCard: String,
        expirationDate: String,
        cvv: String
    ): Boolean {
        // Validate that all fields are not empty
        if (cardNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in cardNumber", Toast.LENGTH_SHORT).show()
            return false
        }
        if (nameOnCard.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in nameOnCard", Toast.LENGTH_SHORT).show()
            return false
        }
        if (expirationDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in expirationDate", Toast.LENGTH_SHORT).show()
            return false
        }
        if (cvv.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in cvv", Toast.LENGTH_SHORT).show()
            return false
        }
//        if (cardNumber.isEmpty() || nameOnCard.isEmpty() || expirationDate.isEmpty() || cvv.isEmpty()) {
//            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
//            return false
//        }

        // Validate the card number has 16 digits
        val cardNumberRegex = Regex("\\d{16}")
        if (!cardNumberRegex.matches(cardNumber)) {
            Toast.makeText(requireContext(), "Invalid card number", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate the cvc has 3 digits
        val cvcRegex = Regex("\\d{3}")
        if (!cvcRegex.matches(cvv)) {
            Toast.makeText(requireContext(), "Invalid cvc", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate the expiration date is in the format "mm/yy"
        val expirationDateRegex = Regex("\\d{2}/\\d{2}")
        if (!expirationDateRegex.matches(expirationDate)) {
            Toast.makeText(requireContext(), "Invalid expiration date", Toast.LENGTH_SHORT).show()
            return false
        }
        val dateFormat = SimpleDateFormat("MM/yy", Locale.US)
        val date = dateFormat.parse(expirationDate)
        val calendar = Calendar.getInstance()

        val inputMonth = date?.let {
            val cal = Calendar.getInstance()
            cal.time = it
            cal.get(Calendar.MONTH)
        }
        val inputYear = date?.let {
            val cal = Calendar.getInstance()
            cal.time = it
            cal.get(Calendar.YEAR)
        }
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        if (inputMonth != null && inputYear != null) {
            if ((inputYear == currentYear && inputMonth <= currentMonth) || inputYear < currentYear) {
                Toast.makeText(requireContext(), "Card already expired!", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun onDateSelected(month: Int, year: Int) {
        val selectedDate = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }
        val formattedDate = SimpleDateFormat("MM/yy", Locale.US).format(selectedDate.time)
        dialogView.dateEditText.text = formattedDate
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadCardsList() {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val cardsRef = database.getReference("users").child(currentUser?.uid ?: "").child("cards")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cardsList = snapshot.children.mapNotNull { it.getValue(Card::class.java) }
                adapter = CardAdapter(requireContext(), cardsList as ArrayList<Card>)
                binding.recicleView.adapter = adapter
                itemTouchHelper.attachToRecyclerView(binding.recicleView)
                if (cardsList.isEmpty()) {
                    binding.recicleView.visibility = View.GONE
                    binding.noCardsTextView.visibility = View.VISIBLE
                } else {
                    binding.recicleView.visibility = View.VISIBLE
                    binding.noCardsTextView.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Failed to observe cards: ${error.message}")
            }
        }
        cardsRef.addValueEventListener(listener)
    }


    val itemTouchHelper = ItemTouchHelper(object :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            // Do nothing, since we're not interested in moving items in the list
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val card = adapter.cards[position]

            if (direction == ItemTouchHelper.LEFT) {
                // Show dialog box to confirm card deletion
                val builder = context?.let { AlertDialog.Builder(it) }
                builder!!.setMessage("Are you sure you want to close this card?")
                    .setTitle("Close card")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
                        // Delete card from RecyclerView
                        adapter.cards.removeAt(position)
                        adapter.notifyItemRemoved(position)

                        // Delete card from Firebase
                        val database = FirebaseDatabase.getInstance()
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val cardsRef = database.getReference("users").child(currentUser?.uid ?: "")
                            .child("cards")
                        val query = cardsRef.orderByChild("number").equalTo(card.number)
                        query.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    for (cardSnapshot in snapshot.children) {
                                        cardSnapshot.ref.removeValue()
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("HomeFragment", "Failed to delete card: ${error.message}")
                            }
                        })
                    }
                    .setNegativeButton("No") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                val alert = builder.create()
                alert.show()
            } else if (direction == ItemTouchHelper.RIGHT) {
                val intent = Intent(context, CardDetailsActivity::class.java)
                val bundle = Bundle().apply {
                    putString("number", card.number)
                    putString("name", card.nameOnCard)
                    putString("expiryDate", card.expirationDate)
                    putString("cvv", card.cvv)
                }
                intent.putExtras(bundle)
                context?.startActivity(intent)
            }
        }
    })

    override fun onResume() {
        super.onResume()
        loadCardsList()
    }
}