package eim.project.mobile_banking_android_app.payments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import eim.project.mobile_banking_android_app.databinding.FragmentPaymentsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        selectDestinationUser()
        binding.paymentButton.setOnClickListener {
            makePayment()
        }
    }

    private fun selectSourceCard() {
        val cardNumberSpinner: Spinner = binding.cardNumberSpinner
        binding.amountInput.setText("")
        getCardsFromDatabase {
            val cardNumbersList = it.result
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cardNumbersList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            cardNumberSpinner.adapter = adapter
            adapter.insert("No card selected", 0)
            cardNumberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedCardNumber = parent.getItemAtPosition(position) as String
                    selectSourceAccount(selectedCardNumber.replace(" ", ""))
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
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

    private fun getAccountsFromDatabase(selectedCardNumber: String, listener: OnCompleteListener<List<Pair<String, String>>>) {
        val accountsList = ArrayList<Pair<String, String>>()
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
                    if (cardNumber != null && cardNumber == selectedCardNumber) {
                        val accountsRef = cardSnapshot.child("savingsAccounts")
                        for (accountSnapshot in accountsRef.children) {
                            val accountDeposit = accountSnapshot.child("deposit").getValue(Boolean::class.java)
                            if (accountDeposit != null && accountDeposit)
                                continue
                            val accountName = accountSnapshot.child("name").getValue(String::class.java)
                            val accountIban = accountSnapshot.child("iban").getValue(String::class.java)
                            if (accountName != null && accountIban != null) {
                                accountsList.add(Pair(accountName, accountIban))
                            }
                        }
                        break
                    }
                }
                listener.onComplete(Tasks.forResult(accountsList))
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun selectSourceAccount(selectedCardNumber: String) {
        val accountLabel = binding.accountLabel
        val accountSpinner = binding.accountSpinner
        binding.amountInput.setText("")
        if (selectedCardNumber == "Nocardselected") {
            accountLabel.visibility = View.GONE // hide the second label
            accountSpinner.visibility = View.GONE // hide the second spinner
            return
        } else {
            accountLabel.visibility = View.VISIBLE // show the second label
            accountSpinner.visibility = View.VISIBLE // show the second spinner
        }
        getAccountsFromDatabase(selectedCardNumber) {
            val accountsList = it.result
            val accountsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, accountsList)
            accountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            accountSpinner.adapter = accountsAdapter
            accountsAdapter.insert(Pair("No account selected", "No IBAN"), 0)
            accountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    // Get the selected account number
                    val selectedAccountPair = (parent.getItemAtPosition(position) as Pair<*, *>)
                    selectMoney(selectedCardNumber, selectedAccountPair.second.toString())
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }
        }
    }

    private fun selectMoney(selectedCardNumber: String, selectedAccountNumber: String) {
        val moneyQuestion = binding.availableMoneyQuestion
        val amountLayout = binding.amountLayout
        val amountInput = binding.amountInput
        amountInput.setText("")
        val maxAvailableMoney = binding.maxAvailableMoney
        val currency = binding.currency

        if (selectedAccountNumber == "No IBAN" || selectedCardNumber == "Nocardselected") {
            moneyQuestion.visibility = View.GONE // hide the second label
            amountLayout.visibility = View.GONE // hide the second spinner
            return
        } else {
            moneyQuestion.visibility = View.VISIBLE // show the second label
            amountLayout.visibility = View.VISIBLE // show the second spinner
        }

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
                    val accountsRef = cardSnapshot.child("savingsAccounts")
                    for (accountSnapshot in accountsRef.children) {
                        val accountIban = accountSnapshot.child("iban").getValue(String::class.java)
                        if (accountIban != null && accountIban == selectedAccountNumber) {
                            val accountBalance = accountSnapshot.child("sold").getValue(Double::class.java)
                            val accountCurrency = accountSnapshot.child("currency").getValue(String::class.java)
                            if (accountBalance != null) {
                                maxAvailableMoney.text = "/" + accountBalance.toString()
                                amountInput.filters = arrayOf<InputFilter>(InputFilterMinMax(0.0, accountBalance))
                                currency.text = accountCurrency
                            }
                            break
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun selectDestinationUser() {
        val amountInput = binding.amountInput
        amountInput.setText("")
        val destinationUserLabel = binding.destinationUserLabel
        val destinationUserSpinner = binding.destinationUserSpinner
        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (amountInput.text.toString().isEmpty()) {
                    destinationUserLabel.visibility = View.GONE
                    destinationUserSpinner.visibility = View.GONE
                } else {
                    destinationUserLabel.visibility = View.VISIBLE
                    destinationUserSpinner.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Start a coroutine to get the users from the agenda and set up the spinner adapter
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val usersList = getUsersFromAgenda(requireContext())
                withContext(Dispatchers.Main) {
                    val usersAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, usersList)
                    usersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    destinationUserSpinner.adapter = usersAdapter
                    usersAdapter.insert("No destination selected", 0)
                    destinationUserSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val selectedUser = parent.getItemAtPosition(position) as String
                            selectDestinationAccount(selectedUser)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
                }
            } catch (_: Exception) {}
        }
    }


    @SuppressLint("Range")
    private fun getUsersFromAgenda(context: Context): List<String> {
        val permission = Manifest.permission.READ_CONTACTS
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(permission), 100)
            return emptyList()
        }

        val contactNames = ArrayList<String>()
        val projection = arrayOf(
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )
        val selection = null
        val selectionArgs = null
        val sortOrder = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        cursor?.use {
            while (it.moveToNext()) {
                val name =
                    it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY))
                contactNames.add(name)
            }
        }
        return contactNames
    }

    private fun selectDestinationAccount(selectedUser: String) {
        val destinationAccountSpinner = binding.destinationAccountSpinner
        if (selectedUser == "No destination selected") {
            binding.destinationAccountLabel.visibility = View.GONE
            binding.destinationAccountSpinner.visibility = View.GONE
            return
        } else {
            binding.destinationAccountLabel.visibility = View.VISIBLE
            binding.destinationAccountSpinner.visibility = View.VISIBLE
        }
        getAccountsByUserFromDatabase(selectedUser
        ) { task ->
            if (task.isSuccessful) {
                val accountsList = task.result
                if (accountsList != null) {
                    val accountsAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        accountsList
                    )
                    accountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    destinationAccountSpinner.adapter = accountsAdapter
                    accountsAdapter.insert("No Account Selected", 0)
                    destinationAccountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            // Get the selected account iban
                            val selectedIban =
                                (parent.getItemAtPosition(position) as String)
                            if (selectedIban != "No Account Selected") {
                                binding.paymentButton.visibility = View.VISIBLE
                            } else {
                                binding.paymentButton.visibility = View.GONE
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // Do nothing
                        }
                    }
                }
            }
        }
    }

    private fun getAccountsByUserFromDatabase(userName: String, listener: OnCompleteListener<List<String>>) {
        val userRef = FirebaseDatabase.
                        getInstance().
                        reference.
                        child("users").
                        orderByChild("name").
                        equalTo(userName)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val accountsList = ArrayList<String>()
                for (userSnapshot in dataSnapshot.children) {
                    for (cardSnapshot in userSnapshot.child("cards").children) {
                        for (savingsAccountSnapshot in cardSnapshot.child("savingsAccounts").children) {
                            val iban = savingsAccountSnapshot.child("iban").getValue(String::class.java)
                            val deposit = savingsAccountSnapshot.child("deposit").getValue(Boolean::class.java)
                            if (deposit == true)
                                continue
                            if (iban != null) {
                                accountsList.add(iban)
                            }
                        }
                    }
                }
                listener.onComplete(Tasks.forResult(accountsList))
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onComplete(Tasks.forException(databaseError.toException()))
            }
        })
    }

    private fun makePayment() {
        //TODO: Make payment
    }



    class InputFilterMinMax(d: Double, accountBalance: Double) : InputFilter {
        private var min: Double = d
        private var max: Double = accountBalance

        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
            try {
                val input = (dest.subSequence(0, dstart).toString() + source.subSequence(start, end) + dest.subSequence(dend, dest.length)).toDouble()
                if (isInRange(min, max, input)) return null
            } catch (_: NumberFormatException) {
            }
            return ""
        }

        private fun isInRange(a: Double, b: Double, c: Double): Boolean {
            return if (b > a) c in a..b else c in b..a

    }
}


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}