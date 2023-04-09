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

class TransferAdapter : RecyclerView.Adapter<TransferAdapter.TransferHolder> {

    private lateinit var binding: ItemTransferBinding
    private var context: Context
    var transfers: ArrayList<Transfer>

    constructor(context: Context, transfers: ArrayList<Transfer>) : super() {
        this.context = context
        this.transfers = transfers
    }
    inner class TransferHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val transfer_amount = binding.amountTextview
        val transfer_date = binding.dateTextview
        val transfer_currency = binding.currencyTextview
        val transfer_iban_dest = binding.ibanDestTextview
        val transfer_iban_src = binding.ibanSrcTextview
        val transfer_type = binding.typeTextview
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransferHolder {
        binding = ItemTransferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransferHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return transfers.size
    }

    @SuppressLint("SetTextI18n")
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
        holder.transfer_type.text = transfer.type
    }
}