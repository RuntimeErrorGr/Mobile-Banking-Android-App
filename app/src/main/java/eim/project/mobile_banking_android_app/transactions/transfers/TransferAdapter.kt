package eim.project.mobile_banking_android_app.transactions.transfers

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import eim.project.mobile_banking_android_app.R
import eim.project.mobile_banking_android_app.databinding.ItemTransferBinding
import java.util.*

/**
 * RecyclerView adapter for displaying a list of Transfer objects.
 */
class TransferAdapter : RecyclerView.Adapter<TransferAdapter.TransferHolder> {

    private lateinit var binding: ItemTransferBinding
    private var context: Context
    var transfers: ArrayList<Transfer>

    constructor(context: Context, transfers: ArrayList<Transfer>) : super() {
        this.context = context
        this.transfers = transfers
    }

    /**
     * This is an inner class TransferHolder which is used as a ViewHolder for the RecyclerView in TransferAdapter.
     * It holds references to the views that represent the data for each item in the RecyclerView.
     * The views are initialized in the constructor and assigned to the corresponding variables for
     * easy access in onBindViewHolder method of TransferAdapter.
     * Each view represents a transfer's information:
     * such as its amount, date, currency, destination account's IBAN, and source account's IBAN.
     * These views are used to display transfer information in each item of the RecyclerView.
     * */
    inner class TransferHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val transfer_amount = binding.amountTextview
        val transfer_date = binding.dateTextview
        val transfer_currency = binding.currencyTextview
        val transfer_iban_dest = binding.ibanDestTextview
        val transfer_iban_src = binding.ibanSrcTextview
    }

    /**
    * Creates and returns a new instance of TransferHolder by inflating the item view layout
    * from the provided parent ViewGroup, using the ItemTransferBinding class.
    * @param parent the parent ViewGroup used to inflate the item view layout.
    * @param viewType an integer that represents the type of view. Unused in this implementation.
    * @return a new instance of TransferHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransferHolder {
        binding = ItemTransferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransferHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return transfers.size
    }

    @SuppressLint("SetTextI18n")
    /**
     * Binds the data from the Transfer object at the given position to the corresponding UI elements in the ViewHolder.
     * @param holder The ViewHolder that contains the UI elements to bind data to.
     * @param position The position of the Transfer object in the transfers list to get data from.
     */
    override fun onBindViewHolder(holder: TransferHolder, position: Int) {
        val transfer = transfers[position]
        holder.transfer_date.text = transfer.date
        holder.transfer_currency.text = transfer.currency
        if (transfer.type.lowercase(Locale.ROOT) == "outcome") {
            holder.transfer_amount.setTextColor(ContextCompat.getColor(context, R.color.error_red))
            holder.transfer_currency.setTextColor(ContextCompat.getColor(context, R.color.error_red))
            holder.transfer_amount.text = "-$"+transfer.amount.toString()
        } else {
            holder.transfer_amount.setTextColor(ContextCompat.getColor(context, R.color.error_green))
            holder.transfer_currency.setTextColor(ContextCompat.getColor(context, R.color.error_green))
            holder.transfer_amount.text = "+$"+transfer.amount.toString()
        }
        holder.transfer_iban_dest.text = transfer.destIban
        holder.transfer_iban_src.text = transfer.srcIban
    }


}