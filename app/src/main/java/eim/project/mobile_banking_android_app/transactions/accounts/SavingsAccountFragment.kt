package eim.project.mobile_banking_android_app.transactions.accounts

import android.content.ContentValues.TAG
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import eim.project.mobile_banking_android_app.databinding.DialogAddSavingsAccountBinding
import eim.project.mobile_banking_android_app.databinding.FragmentSavingsAccountBinding
import eim.project.mobile_banking_android_app.home.DatePickerFragment
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.ln

class SavingsAccountFragment : Fragment() {
    private var _binding: FragmentSavingsAccountBinding? = null
    private lateinit var dialogView: DialogAddSavingsAccountBinding
    private val binding get() = _binding!!
    private lateinit var adapter: SavingsAccountAdapter
    private var context: Context? = null
    private var cardNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cardNumber = it.getString("currentCardNumber")
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavingsAccountBinding.inflate(inflater, container, false)
        val root = binding.root
        binding.recicleView.layoutManager = LinearLayoutManager(requireContext())
        dialogView = DialogAddSavingsAccountBinding.inflate(layoutInflater)
        binding.createAccountButton.setOnClickListener {
            dialogView.depositDate.setOnClickListener {
                showDatePicker()
            }

            dialogView.depositCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    dialogView.depositRate.visibility = View.VISIBLE
                    dialogView.depositRateLabel.visibility = View.VISIBLE
                    dialogView.depositAmount.visibility = View.VISIBLE
                    dialogView.depositDate.visibility = View.VISIBLE
                    dialogView.depositDateLabel.visibility = View.VISIBLE
                } else {
                    dialogView.depositRate.visibility = View.GONE
                    dialogView.depositRateLabel.visibility = View.GONE
                    dialogView.depositAmount.visibility = View.GONE
                    dialogView.depositDate.visibility = View.GONE
                    dialogView.depositDateLabel.visibility = View.GONE
                }
            }

            // Listen for changes in deposit date
            dialogView.depositDate.addTextChangedListener(object : TextWatcher {

                fun getDepositRate(depositTimestamp: Long): String {
                    val nowTimestamp = Date().time
                    val daysDifference = TimeUnit.MILLISECONDS.toDays(depositTimestamp - nowTimestamp)
                    val rateMultiplier = BigDecimal((ln((daysDifference / 30.0) + 1) + 1)).setScale(3, RoundingMode.HALF_UP).toDouble()
                    return (0.9 * rateMultiplier).toString().take(6)
                }
                override fun afterTextChanged(s: Editable?) {
                    val depositDate = dialogView.depositDate.text.toString()
                    val depositTimestamp = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(depositDate)?.time ?: return
                    val depositRate = getDepositRate(depositTimestamp)
                    dialogView.depositRate.text = depositRate
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

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
                    // Get data from UI
                    val accountNameText = dialogView.accountName.text.toString()
                    val accountCurrencyText = dialogView.accountCurrency.text.toString()
                    val depositAmountText = dialogView.depositAmount.text.toString()
                    val depositDateText = dialogView.depositDate.text.toString()
                    val depositRateText = dialogView.depositRate.text.toString()
                    val depositCheckbox = dialogView.depositCheckbox.isChecked

                    // Additional data
                    val iban = "${accountCurrencyText.take(2)}${(1..14).map { (0..9).random() }.joinToString("")}".padEnd(16, '0')
                    val objectRate = if (depositCheckbox) depositRateText.toDouble() else 0.0
                    val objectAmount = if (depositCheckbox) depositAmountText.toDouble() else 0.0
                    val objectDate = if (depositCheckbox) depositDateText else ""

                    // Create account
                    val account = SavingsAccount(
                        cardNumber = cardNumber,
                        currency = accountCurrencyText,
                        name = accountNameText,
                        iban = iban,
                        interest_rate = objectRate,
                        sold = objectAmount,
                        liquidation_date = objectDate,
                        isDeposit = depositCheckbox,
                    )
                    addSavingsAccountToDatabase(account, dialog)
                }
            }
            dialog.show()
        }
        loadAccounts()
        return root
    }

    private fun addSavingsAccountToDatabase(account: SavingsAccount, dialog: AlertDialog) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        val cardsRef = currentUser?.uid?.let {
            FirebaseDatabase.getInstance().reference.child("users").child(it).child("cards")
        }

        cardsRef?.orderByChild("number")?.equalTo(cardNumber)?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val mainAccountSnapshot = dataSnapshot.children.firstOrNull()?.child("savingsAccounts")?.child("0")
                val mainAccountSold = mainAccountSnapshot?.child("sold")?.getValue(Double::class.java) ?: 0.0

                val savingsAccountsRef = mainAccountSnapshot?.ref?.parent
                val accountNumber = mainAccountSnapshot?.key?.toInt()?.plus(1).toString()

                if (account.isDeposit && account.sold > mainAccountSold) {
                    Toast.makeText(context, "Deposit account's sold value is greater than the main account's sold value", Toast.LENGTH_SHORT).show()
                    return
                }

                val newMainAccountSoldValue = mainAccountSold - account.sold
                savingsAccountsRef?.child(accountNumber)?.setValue(account)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        mainAccountSnapshot.child("sold").ref.setValue(newMainAccountSoldValue)
                            .addOnCompleteListener { mainAccountTask ->
                                if (mainAccountTask.isSuccessful) {
                                    Toast.makeText(context, "Savings account added successfully", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                } else {
                                    Toast.makeText(context, "Failed to update main account's sold value", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Failed to add savings account", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Failed to retrieve card data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAccounts() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val database = FirebaseDatabase.getInstance()
        val cardsRef = database.getReference("/users/${currentUser?.uid}/cards")

        val query = cardsRef.orderByChild("number").equalTo(cardNumber)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val savingsAccountList = ArrayList<SavingsAccount>()

                    for (cardSnapshot in dataSnapshot.children) {
                        val savingsAccountsSnapshot = cardSnapshot.child("savingsAccounts")
                        for (accountSnapshot in savingsAccountsSnapshot.children) {
                            accountSnapshot.getValue(SavingsAccount::class.java)?.let { savingsAccountList.add(it) }
                        }
                    }
                    adapter = SavingsAccountAdapter(requireContext(), savingsAccountList)
                    binding.recicleView.adapter = adapter
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "loadAccounts:onCancelled", databaseError.toException())
            }
        })
    }

    private fun showDatePicker() {
        val datePicker = DatePickerFragment { day, month, year -> onDateSelected(day, month, year) }
        datePicker.show(parentFragmentManager, "datePicker")
    }

    private fun onDateSelected(day: Int, month: Int, year: Int) {
        val selectedDate = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(selectedDate.time)
        dialogView.depositDate.text = formattedDate
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
