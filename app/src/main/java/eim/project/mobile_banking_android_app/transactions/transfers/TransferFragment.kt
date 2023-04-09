package eim.project.mobile_banking_android_app.transactions.transfers

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import eim.project.mobile_banking_android_app.databinding.FragmentTransfersBinding

class TransferFragment : Fragment() {

    private var _binding: FragmentTransfersBinding? = null
    private val binding get() = _binding!!
    private var context: Context? = null
    private lateinit var adapter: TransferAdapter
    private var cardNumber: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransfersBinding.inflate(inflater, container, false)
        val root = binding.root
        binding.recicleView.layoutManager = LinearLayoutManager(requireContext())
        loadTransfers()
        return root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cardNumber = it.getString("currentCardNumber")
        }
    }

    private fun loadTransfers() {
        val transfersList = ArrayList<Transfer>()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val database = FirebaseDatabase.getInstance()
            val cardsRef = database.getReference("users/${currentUser.uid}/cards")
            val currentCardNumber = cardNumber
            cardsRef.orderByChild("number").equalTo(currentCardNumber).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val currentCardKey = snapshot.children.first().key
                        val accountsRef = database.getReference("users/${currentUser.uid}/cards/$currentCardKey/savingsAccounts")
                        accountsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (accountSnapshot in snapshot.children) {
                                    val transfers = accountSnapshot.child("transfers").getValue(object : GenericTypeIndicator<ArrayList<Transfer>>() {})
                                    if (transfers != null) {
                                        transfersList.addAll(transfers)
                                    }
                                }
                                adapter = TransferAdapter(requireContext(), transfersList)
                                binding.recicleView.adapter = adapter
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(context, "Error loading transfers", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error loading transfers", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    override fun onResume() {
        super.onResume()
        loadTransfers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
