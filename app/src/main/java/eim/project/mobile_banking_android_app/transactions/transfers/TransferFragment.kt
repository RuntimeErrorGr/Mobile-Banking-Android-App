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


    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransfersBinding.inflate(inflater, container, false)
        val root = binding.root

        // Set the LayoutManager for the RecyclerView
        binding.recicleView.layoutManager = LinearLayoutManager(requireContext())
        // Listener for the income button, reload transfers
        binding.incomeButton.addOnCheckedChangeListener { _, isChecked ->
            isIncomeChecked = isChecked
            loadTransfers()
        }
        // Listener for the outcome button, reload transfers
        binding.outcomeButton.addOnCheckedChangeListener { _, isChecked ->
            isOutcomeChecked = isChecked
            loadTransfers()
        }
        // Load the transfers into the RecyclerView
        loadTransfers()
        return root
    }

    /**
     * This function initializes the activity and gets the card number from the passed arguments.
     * @param savedInstanceState (Bundle): A Bundle object containing previously saved state information, or null if no saved state exists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cardNumber = it.getString("currentCardNumber")
        }
    }

    /**
     * This function loads the transfers of the current user and updates the UI accordingly.
     * The list of transfers can be filtered by income or outcome.
     */
    private fun loadTransfers() {
        val transfersList = ArrayList<Transfer>()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val database = FirebaseDatabase.getInstance()
            val cardsRef = database.getReference("users/${currentUser.uid}/cards")
            // Get the card with the specified card number
            cardsRef.orderByChild("number").equalTo(cardNumber).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val currentCardKey = snapshot.children.first().key
                        val accountsRef = database.getReference("users/${currentUser.uid}/cards/$currentCardKey/savingsAccounts")

                        // Get the savings accounts associated with the current card
                        accountsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                // Iterate through each account and add its transfers to the transfers list
                                for (accountSnapshot in snapshot.children) {
                                    val transfers = accountSnapshot.child("transfers").getValue(object : GenericTypeIndicator<ArrayList<Transfer>>() {})
                                    if (transfers != null) {
                                        transfersList.addAll(transfers)
                                    }
                                }

                                // Filter the transfers list based on the selected filters
                                var filteredList = if (isIncomeChecked && isOutcomeChecked) {
                                    transfersList // show all transfers
                                } else (if (isIncomeChecked) {
                                    transfersList.filter { it.type == "income" }
                                } else if (isOutcomeChecked) {
                                    transfersList.filter { it.type == "outcome" }
                                } else {
                                    ArrayList(emptyList()) // show no transfers
                                }) as ArrayList<Transfer>

                                // Create and set the adapter for the RecyclerView
                                val adapter = context?.let { TransferAdapter(it, filteredList) }!!
                                binding.recicleView.adapter = adapter

                                // Set up the search view to filter the transfers list by source or destination name
                                binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                                    override fun onQueryTextSubmit(query: String?): Boolean {
                                        return false
                                    }

                                    override fun onQueryTextChange(newText: String?): Boolean {
                                        // Filter the transfers list based on the search query
                                        filteredList = if (newText.isNullOrBlank()) {
                                            transfersList
                                        } else {
                                            (transfersList.filter { (it.srcName.contains(newText, true) && it.type == "outcome") ||
                                                    (it.destName.contains(newText, true) && it.type == "income") }
                                                    as ArrayList<Transfer>)
                                        }

                                        // Set the adapter for the RecyclerView with the filtered list
                                        binding.recicleView.adapter = TransferAdapter(requireContext(), filteredList)
                                        return true
                                    }
                                })
                                if (filteredList.isEmpty()) {
                                    binding.recicleView.visibility = View.GONE
                                    binding.noTransfersFound.visibility = View.VISIBLE
                                } else {
                                    binding.recicleView.visibility = View.VISIBLE
                                    binding.noTransfersFound.visibility = View.GONE
                                }
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
