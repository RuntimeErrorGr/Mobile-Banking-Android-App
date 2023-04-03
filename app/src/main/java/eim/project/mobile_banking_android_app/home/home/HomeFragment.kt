package eim.project.mobile_banking_android_app.home.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import eim.project.mobile_banking_android_app.MainActivity
import eim.project.mobile_banking_android_app.databinding.FragmentHomeBinding

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
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.popupLayout.visibility = View.GONE

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        binding.logoutBtn.setOnClickListener() {
            firebaseAuth.signOut()
            checkUser()
        }

        binding.addCreditCardBtn.setOnClickListener() {
            binding.popupLayout.visibility = View.VISIBLE
            binding.addCreditCardBtn.visibility = View.GONE
        }

        binding.cancelButton.setOnClickListener() {
            binding.popupLayout.visibility = View.GONE
            binding.addCreditCardBtn.visibility = View.VISIBLE
        }

        binding.saveButton.setOnClickListener() {
            sendCardDetailsToDatabase()
            binding.popupLayout.visibility = View.GONE
            binding.addCreditCardBtn.visibility = View.GONE
        }

        return root
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
        val cardsRef = database.getReference("users/${currentUser?.uid}/card")

        val cardNumber = binding.nameEditText.text.toString()
        val nameOnCard = binding.numberEditText.text.toString()
        val expirationDate = binding.cvcEditText.text.toString()
        val cvc = binding.expirationEditText.text.toString()

        // Validate that all fields are not empty
        if (cardNumber.isEmpty() || nameOnCard.isEmpty() || expirationDate.isEmpty() || cvc.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate the card number has 16 digits
        val cardNumberRegex = Regex("\\d{16}")
        if (!cardNumberRegex.matches(cardNumber)) {
            Toast.makeText(requireContext(), "Invalid card number", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate the cvc has 3 digits
        val cvcRegex = Regex("\\d{3}")
        if (!cvcRegex.matches(cvc)) {
            Toast.makeText(requireContext(), "Invalid cvc", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate the expiration date is in the format "mm/yy"
        val expirationDateRegex = Regex("\\d{2}/\\d{2}")
        if (!expirationDateRegex.matches(expirationDate)) {
            Toast.makeText(requireContext(), "Invalid expiration date", Toast.LENGTH_SHORT).show()
            return
        }

        val card = Card(cardNumber, nameOnCard, expirationDate, cvc)
        cardsRef.push().setValue(card)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}