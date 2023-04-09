package eim.project.mobile_banking_android_app.transactions.transfers

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
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
    private lateinit var filteredList: ArrayList<Transfer>
    private var cardNumber: String? = null
    private var isIncomeChecked = true
    private var isOutcomeChecked = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransfersBinding.inflate(inflater, container, false)
        val root = binding.root
        binding.recicleView.layoutManager = LinearLayoutManager(requireContext())

        binding.incomeButton.addOnCheckedChangeListener { _, isChecked ->
            isIncomeChecked = isChecked
            loadTransfers()
        }

        binding.outcomeButton.addOnCheckedChangeListener { _, isChecked ->
            isOutcomeChecked = isChecked
            loadTransfers()
        }

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
                                    val transfers = accountSnapshot.child("transfers").
                                        getValue(object : GenericTypeIndicator<ArrayList<Transfer>>() {})
                                    if (transfers != null) {
                                        transfersList.addAll(transfers)
                                    }
                                }
                                filteredList = if (isIncomeChecked && isOutcomeChecked) {
                                    transfersList // show all transfers
                                } else (if (isIncomeChecked) {
                                    transfersList.filter { it.type == "income" }
                                } else if (isOutcomeChecked) {
                                    transfersList.filter { it.type == "outcome" }
                                } else {
                                    ArrayList(emptyList()) // show no transfers
                                }) as ArrayList<Transfer>

                                adapter = TransferAdapter(requireContext(), filteredList)
                                binding.recicleView.adapter = adapter

                                binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                                    override fun onQueryTextSubmit(query: String?): Boolean {
                                        return false
                                    }

                                    override fun onQueryTextChange(newText: String?): Boolean {
                                        filteredList = if (newText.isNullOrBlank()) {
                                            transfersList
                                        } else {
                                            transfersList.filter { it.srcName.contains(newText, true) ||
                                                    it.destName.contains(newText, true) } as ArrayList<Transfer>
                                        }
                                        adapter = TransferAdapter(requireContext(), filteredList)
                                        binding.recicleView.adapter = adapter
                                        if (filteredList.isEmpty()) {
                                            binding.recicleView.visibility = View.GONE
                                            binding.noTransfersFound.visibility = View.VISIBLE
                                        } else {
                                            binding.recicleView.visibility = View.VISIBLE
                                            binding.noTransfersFound.visibility = View.GONE
                                        }
                                        return true
                                    }
                                })
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
