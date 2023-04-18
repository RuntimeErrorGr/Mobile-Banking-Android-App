package eim.project.mobile_banking_android_app.payments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import eim.project.mobile_banking_android_app.databinding.FragmentPaymentsBinding

class PaymentsFragment : Fragment() {

    private var _binding: FragmentPaymentsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectSourceCard()

    }

    private fun selectSourceCard() {
        val cardNumberSpinner: Spinner = binding.cardNumberSpinner
        getCardsFromDatabase {
            val cardNumbersList = it.result
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cardNumbersList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            cardNumberSpinner.adapter = adapter
            cardNumberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    // Get the selected card number
                    val selectedCardNumber = parent.getItemAtPosition(position) as String
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }
        }
    }

    private fun getCardsFromDatabase(listener: OnCompleteListener<List<String>>) {
        val cardNumberList = ArrayList<String>()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val cardsRef = FirebaseDatabase
            .getInstance()
            .reference
            .child("users")
            .child(currentUser!!.uid)
            .child("cards")

        cardsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (cardSnapshot in snapshot.children) {
                    val cardNumber = cardSnapshot.child("number").getValue(String::class.java)
                    if (cardNumber != null) {
                        cardNumberList.add(cardNumber.chunked(4).joinToString(" "))
                    }
                }
                listener.onComplete(Tasks.forResult(cardNumberList))
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}