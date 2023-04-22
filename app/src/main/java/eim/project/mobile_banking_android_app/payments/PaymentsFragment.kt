package eim.project.mobile_banking_android_app.payments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.JsonParser
import eim.project.mobile_banking_android_app.R
import eim.project.mobile_banking_android_app.databinding.FragmentPaymentsBinding
import eim.project.mobile_banking_android_app.databinding.ItemTransferBinding
import eim.project.mobile_banking_android_app.transactions.accounts.SavingsAccount
import eim.project.mobile_banking_android_app.transactions.transfers.Transfer
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.CountDownLatch


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
        binding.maxAvailableMoney.setOnClickListener {
            val maxAvailableMoney = binding.maxAvailableMoney.text.toString().replace("/", "").toDouble()
            binding.amountInput.text = Editable.Factory.getInstance().newEditable(maxAvailableMoney.toString())
        }
        binding.paymentButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage("Are you sure you want to make the payment?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, _ ->
                    makePayment()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
            val dialog = builder.create()
            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            dialog.show()
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

        cardsRef.addValueEventListener(object : ValueEventListener {
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
        val sourceAccountPairs = binding.accountSpinner.selectedItem.toString()
        val (_, accountNumber) = sourceAccountPairs.substring(1, sourceAccountPairs.length - 1).split(", ")
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
                            if (iban == accountNumber)
                                continue
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
        val sourceCardNumber = binding.cardNumberSpinner.selectedItem.toString().replace(" ", "")
        val sourceAccountPairs = binding.accountSpinner.selectedItem.toString()
        val sourceAccountCurrency = binding.currency.text.toString()
        val (sourceAccountName, sourceAccountIban) = sourceAccountPairs.substring(1, sourceAccountPairs.length - 1).split(", ")
        val amountInput = binding.amountInput.text.toString()
        val destinationUserName = binding.destinationUserSpinner.selectedItem.toString()
        val destinationAccountIban = binding.destinationAccountSpinner.selectedItem.toString()

        getSavingsAccountDetailsByIban(destinationAccountIban) { accountDetails ->
            if (accountDetails != null) {
                val (destinationAccountName, destinationAccountCurrency) = accountDetails
                if (destinationAccountCurrency != sourceAccountCurrency) {
                    convertAmountAndTransfer(
                        sourceCardNumber,
                        sourceAccountIban,
                        sourceAccountName,
                        sourceAccountCurrency,
                        destinationUserName,
                        destinationAccountIban,
                        destinationAccountName,
                        destinationAccountCurrency,
                        amountInput.toDouble()
                    )
                } else {
                    transfer(
                        sourceCardNumber,
                        sourceAccountIban,
                        sourceAccountName,
                        sourceAccountCurrency,
                        destinationUserName,
                        destinationAccountIban,
                        destinationAccountName,
                        amountInput.toDouble()
                    )
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun transfer(
        sourceCardNumber: String,
        sourceAccountIban: String,
        sourceAccountName: String,
        sourceAccountCurrency: String,
        destinationUserName: String,
        destinationAccountIban: String,
        destinationAccountName: String,
        amount: Double
    ) {
        val transfer = Transfer(
            destIban = destinationAccountIban,
            srcIban = sourceAccountIban,
            amount = amount,
            currency = sourceAccountCurrency,
            description = "Rent payment",
            date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            type = "outcome",
            srcName = sourceAccountName,
            destName = destinationAccountName)

        updateSourceAccount(sourceCardNumber, sourceAccountIban, amount, transfer)
        updateDestinationAccount(destinationUserName, destinationAccountIban, amount, transfer)
        showItemTransferPopout(
            requireContext(),
            amount,
            sourceAccountCurrency,
            destinationAccountIban,
            sourceAccountIban)
    }

    @SuppressLint("NewApi")
    private fun convertAmountAndTransfer(
        sourceCardNumber: String,
        sourceAccountIban: String,
        sourceAccountName: String,
        sourceAccountCurrency: String,
        destinationUserName: String,
        destinationAccountIban: String,
        destinationAccountName: String,
        destinationAccountCurrency: String,
        amount: Double
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            convertCurrency(
                requireActivity(),
                requireContext(),
                sourceAccountIban = sourceAccountIban,
                destinationAccountIban = destinationAccountIban,
                sourceCurrency = sourceAccountCurrency,
                destinationCurrency = destinationAccountCurrency,
                amount = amount,
                onSuccess = { convertedAmount, executeTransfer ->
                    if (!executeTransfer) {
                        return@convertCurrency
                    }
                    val roundedAmount = BigDecimal(convertedAmount).
                        setScale(2, RoundingMode.HALF_EVEN).
                        toDouble()
                    val transfer = Transfer(
                        destIban = destinationAccountIban,
                        srcIban = sourceAccountIban,
                        amount = amount,
                        currency = sourceAccountCurrency,
                        description = "Rent payment",
                        date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        type = "outcome",
                        srcName = sourceAccountName,
                        destName = destinationAccountName
                    )
                    val destTransfer = transfer.copy()
                    destTransfer.amount = roundedAmount
                    destTransfer.currency = destinationAccountCurrency
                    updateSourceAccount(sourceCardNumber, sourceAccountIban, amount, transfer)
                    updateDestinationAccount(destinationUserName, destinationAccountIban, roundedAmount, destTransfer)
                },
                onFailure = {}
            )
        }
    }





    companion object {
        @SuppressLint("NewApi")
        fun showItemTransferPopout(
            context: Context,
            convertedAmount: Double,
            sourceAccountCurrency: String,
            destinationAccountIban: String,
            sourceAccountIban: String
        ) {
            val popoutDialog = Dialog(context)
            val binding = ItemTransferBinding.inflate(LayoutInflater.from(context))
            popoutDialog.setContentView(binding.root)
            popoutDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            binding.amountTextview.text = String.format("%.2f", convertedAmount)
            binding.currencyTextview.text = sourceAccountCurrency
            binding.dateTextview.text = java.time.LocalDate.now().
            format(java.time.format.DateTimeFormatter.
            ofPattern("dd/MM/yyyy"))
            binding.ibanDestTextview.text = destinationAccountIban
            binding.ibanSrcTextview.text = sourceAccountIban

            popoutDialog.show()

            Handler().postDelayed({ popoutDialog.dismiss() }, 2000)

        }

        @SuppressLint("NewApi")
        fun convertCurrency(
            activity: FragmentActivity,
            context: Context,
            sourceAccountIban: String,
            destinationAccountIban: String,
            sourceCurrency: String,
            destinationCurrency: String,
            amount: Double,
            onSuccess: (Double, Boolean) -> Unit,
            onFailure: () -> Unit
        ) {
            val client = OkHttpClient()
            var executeTransfer = true
            val url = "https://api.exchangerate.host/convert?from=$sourceCurrency&to=$destinationCurrency&amount=$amount"
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val jsonObject = JsonParser.parseString(json).asJsonObject
                    val convertedAmount = jsonObject.get("result").asDouble
                    val exchangeRate = jsonObject.get("info").asJsonObject.get("rate").asDouble
                    val latch = CountDownLatch(1)
                    activity.runOnUiThread {
                        AlertDialog.Builder(context)
                            .setTitle("Different currencies")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("The destination account has a different currency than the source account. If you proceed with the transfer, both accounts will be updated to reflect the currency conversion." +
                                    "\n\n $amount $sourceCurrency = ${String.format("%.2f", convertedAmount)} $destinationCurrency" +
                                    "\n\n (1$sourceCurrency = $exchangeRate$destinationCurrency)")
                            .setPositiveButton("Continue") { dialog, _ ->
                                dialog.dismiss()
                                showItemTransferPopout(
                                    context,
                                    amount,
                                    sourceCurrency,
                                    destinationAccountIban,
                                    sourceAccountIban)
                                latch.countDown()
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                                executeTransfer = false
                                latch.countDown()
                            }
                            .show()
                    }
                    latch.await()
                    onSuccess(convertedAmount, executeTransfer)
                } else {
                    onFailure()
                }
            }
        }
    }



    private fun updateSourceAccount(
        sourceCardNumber: String,
        sourceAccountIban: String,
        amount: Double,
        transfer: Transfer
    ) {
        val rootRef = FirebaseDatabase.getInstance().reference
        val currentUser = FirebaseAuth.getInstance().currentUser

        rootRef.child("users").child(currentUser!!.uid).child("cards")
            .orderByChild("number")
            .equalTo(sourceCardNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (cardSnapshot in snapshot.children) {
                            val accountSnapshot =
                                cardSnapshot.child("savingsAccounts").children.find {
                                    it.child("iban").value == sourceAccountIban
                                }
                            if (accountSnapshot != null) {
                                val currentBalance = when (val sold = accountSnapshot.child("sold").value) {
                                    is Long -> sold.toDouble()
                                    is Double -> sold
                                    else -> throw IllegalArgumentException("Unexpected value type for sold")
                                }
                                val newBalance = currentBalance - amount
                                accountSnapshot.ref.child("sold").setValue(newBalance)
                                updateTransfers(accountSnapshot, transfer)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        TAG,
                        "Failed to update source account balance: ${error.message}"
                    )
                }
            })
    }


    private fun updateTransfers(accountSnapshot: DataSnapshot, transfer: Transfer) {
        val transfersRef = accountSnapshot.ref.child("transfers")
        transfersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val nextIndex = snapshot.childrenCount.toInt()
                    transfersRef.child(nextIndex.toString()).setValue(transfer)
                } else {
                    // Transfers node doesn't exist, create it with the first transfer
                    transfersRef.child("0").setValue(transfer)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    TAG,
                    "Failed to update transfers for source account: ${error.message}"
                )
            }
        })
    }

    private fun updateDestinationAccount(destinationUserName: String,
                                         destinationAccountNumber: String,
                                         amount: Double,
                                         transfer: Transfer) {
        val destTransfer = transfer.copy()
        destTransfer.type = "income"
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val query = usersRef.orderByChild("name").equalTo(destinationUserName)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    for (cardSnapshot in userSnapshot.child("cards").children) {
                        for (accountSnapshot in cardSnapshot.child("savingsAccounts").children) {
                            val account = accountSnapshot.getValue(SavingsAccount::class.java)
                            if (account != null && account.iban == destinationAccountNumber) {
                                val newSold = account.sold + amount
                                accountSnapshot.ref.child("sold").setValue(newSold)
                                updateTransfers(accountSnapshot, destTransfer)
                                return
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "updateDestinationAccountBalance:onCancelled", error.toException())
            }
        })
    }

    private fun getSavingsAccountDetailsByIban(iban: String, callback: (Pair<String, String>?) -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().reference
        databaseRef.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var accountDetails: Pair<String, String>? = null
                for (userSnapshot in snapshot.children) {
                    val cardsSnapshot = userSnapshot.child("cards")
                    for (cardSnapshot in cardsSnapshot.children) {
                        val savingsAccountsSnapshot = cardSnapshot.child("savingsAccounts")
                        for (accountSnapshot in savingsAccountsSnapshot.children) {
                            val ibanValue = accountSnapshot.child("iban").getValue(String::class.java)
                            if (ibanValue == iban) {
                                val accountName = accountSnapshot.child("name").getValue(String::class.java)
                                val currency = accountSnapshot.child("currency").getValue(String::class.java)
                                accountDetails = Pair(accountName!!, currency!!)
                                break
                            }
                        }
                        if (accountDetails != null) {
                            break
                        }
                    }
                    if (accountDetails != null) {
                        break
                    }
                }
                callback(accountDetails)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
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