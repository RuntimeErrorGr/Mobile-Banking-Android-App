package eim.project.mobile_banking_android_app.transactions.accounts

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eim.project.mobile_banking_android_app.databinding.ItemAccountBinding

/**
 * Adapter class for the RecyclerView that holds the accounts.
 * It is used to bind the data to the views in each item.
 * */
class SavingsAccountAdapter : RecyclerView.Adapter<SavingsAccountAdapter.SavingsAccountHolder> {

    private lateinit var binding: ItemAccountBinding
    private var context: Context
    var accounts: ArrayList<SavingsAccount>

    constructor(context: Context, accounts: ArrayList<SavingsAccount>) : super() {
        this.context = context
        this.accounts = accounts
    }

    /**
     * Inner class that holds the views for each account item in the RecyclerView.
     * */
    inner class SavingsAccountHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val account_ballance = binding.balanceTextview
        val account_iban = binding.ibanTextview
        val account_currency = binding.currencyTextview
        val account_type = binding.typeTextview
        val account_name = binding.titleTextview
        val account_rate = binding.rateTextView
        val accout_date = binding.dateTextView
        val account_date_label = binding.dateLabelTextView
        val account_rate_label = binding.rateLabelTextView

    }

    /**
     * Creates and returns a new instance of SavingsAccountHolder by inflating the item view layout
     * */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingsAccountHolder {
        binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SavingsAccountHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return accounts.size
    }

    /**
     * Binds the data to the views in each item.
     * */
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SavingsAccountHolder, position: Int) {
        val account = accounts[position]
        val balanceString = account.sold.toString()
        val decimalIndex = balanceString.indexOf(".") + 1
        val decimalLength = balanceString.length - decimalIndex
        holder.account_ballance.text = if (decimalLength > 5) {
            balanceString.substring(0, decimalIndex + 5)
        } else {
            balanceString
        }
        holder.account_iban.text = account.iban
        holder.account_currency.text = account.currency
        holder.account_type.text = if (account.isDeposit) "Deposit" else if (account.isMain) "" else "Savings"
        if (account.isDeposit) {
            if (account.name == "New Account") {
                account.name = "New Deposit"
            }
            holder.account_rate.text = account.interest_rate.toString() + "%"
            holder.accout_date.text = account.liquidation_date
            holder.accout_date.visibility = View.VISIBLE
            holder.account_rate.visibility = View.VISIBLE
            holder.account_date_label.visibility = View.VISIBLE
            holder.account_rate_label.visibility = View.VISIBLE
        }
        holder.account_name.text = account.name
    }


}