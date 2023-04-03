package eim.project.mobile_banking_android_app.home.home

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import eim.project.mobile_banking_android_app.MainActivity
import eim.project.mobile_banking_android_app.databinding.FragmentHomeBinding
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var firebaseAuth: FirebaseAuth

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
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
            sendCardDetailsToDatabase()
        }

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

    private fun sendCardDetailsToDatabase() {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val cardsRef = database.getReference("users/${currentUser?.uid}/cards")

        val cardNumber = binding.numberEditText.text.toString()
        val nameOnCard = binding.nameEditText.text.toString()
        val expirationDate = binding.dateEditText.text.toString()
        val cvv = binding.cvvEditText.text.toString()

        if (validateCardDetalisInput(cardNumber, nameOnCard, expirationDate, cvv)) {
            val card = Card(cardNumber, nameOnCard, expirationDate, cvv)
            cardsRef.push().setValue(card)
            binding.popupLayout.visibility = View.GONE
            binding.addCreditCardBtn.isEnabled = true
            resetFields()
            Toast.makeText(requireContext(), "Card successfully added!", Toast.LENGTH_SHORT).show()
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

    private fun resetFields() {
        binding.numberEditText.text = null
        binding.nameEditText.text = null
        binding.dateEditText.text = null
        binding.cvvEditText.text = null
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}