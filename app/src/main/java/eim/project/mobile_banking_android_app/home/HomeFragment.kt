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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import eim.project.mobile_banking_android_app.MainActivity
import eim.project.mobile_banking_android_app.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var firebaseAuth: FirebaseAuth

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

        binding.popupLayout.visibility = View.GONE

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        binding.logoutBtn.setOnClickListener() {
            firebaseAuth.signOut()
            checkUser()
        }

        binding.dateEditText.setOnClickListener {
            showDatePicker()
        }

        binding.addCreditCardBtn.setOnClickListener() {
            binding.popupLayout.visibility = View.VISIBLE
            binding.addCreditCardBtn.isEnabled = false
        }

        binding.cancelButton.setOnClickListener() {
            binding.popupLayout.visibility = View.GONE
            binding.addCreditCardBtn.isEnabled = true
        }

        binding.saveButton.setOnClickListener() {
            lifecycleScope.launch {
                // Get the reference card with data from the UI
                val card = getCardDetailsFromUI()
                // Add card in the database
                withContext(Dispatchers.IO) {
                    addCardToDatabase(card)
                }
            }
        }

        loadCardsList()
        return root
    }

    private fun showDatePicker() {
        val datePicker = DatePickerFragment { day, month, year -> onDateSelected(day, month, year) }
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

    private fun getCardDetailsFromUI(): Card? {
        val cardNumber = binding.numberEditText.text.toString()
        val nameOnCard = binding.nameEditText.text.toString()
        val expirationDate = binding.dateEditText.text.toString()
        val cvv = binding.cvvEditText.text.toString()
        var card: Card? = null

        if (validateCardDetalisInput(cardNumber, nameOnCard, expirationDate, cvv)) {
            card = Card(cardNumber, nameOnCard, expirationDate, cvv)
            binding.popupLayout.visibility = View.GONE
            binding.addCreditCardBtn.isEnabled = true
            resetCardFormFields()
        }
        return card
    }

    private suspend fun addCardToDatabase(card: Card?): Boolean = withContext(Dispatchers.IO) {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val usersRef = database.getReference("users").child(currentUser?.uid ?: "")
        val cardsQuery = usersRef.child("cards").orderByChild("number").equalTo(card?.number ?: "")
        val snapshot = cardsQuery.get().await()

        if (card == null) {
            return@withContext false
        }

        if (!snapshot.exists()) {
            card.user = currentUser?.uid ?: ""
            usersRef.child("phoneNumber").setValue(currentUser?.phoneNumber)
            val newCardRef = usersRef.child("cards").push()
            newCardRef.setValue(card)
            Log.d("HomeFragment", "New card added: ${card.nameOnCard}")
            return@withContext true
        } else {
            Log.d("HomeFragment", "Card already exists: ${card.nameOnCard}")
            return@withContext false
        }
    }

    private fun validateCardDetalisInput(
        cardNumber: String,
        nameOnCard: String,
        expirationDate: String,
        cvv: String
    ): Boolean {
        // Validate that all fields are not empty
        if (cardNumber.isEmpty() || nameOnCard.isEmpty() || expirationDate.isEmpty() || cvv.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }

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

    private fun onDateSelected(day: Int, month: Int, year: Int) {
        val selectedDate = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }
        val formattedDate = SimpleDateFormat("MM/yy", Locale.US).format(selectedDate.time)
        binding.dateEditText.text = formattedDate
    }

    private fun resetCardFormFields() {
        binding.numberEditText.text = null
        binding.nameEditText.text = null
        binding.dateEditText.text = null
        binding.cvvEditText.text = null
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

    val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
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
            val deletedCard = adapter.cards[position]

            // Show dialog box
            val builder = context?.let { AlertDialog.Builder(it) }
            builder!!.setMessage("Are you sure you want to delete this card?")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    // Delete card from RecyclerView
                    adapter.cards.removeAt(position)
                    adapter.notifyItemRemoved(position)

                    // Delete card from Firebase
                    val database = FirebaseDatabase.getInstance()
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val cardsRef = database.getReference("users").child(currentUser?.uid ?: "").child("cards")
                    val query = cardsRef.orderByChild("number").equalTo(deletedCard.number)
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
        }

    })


}