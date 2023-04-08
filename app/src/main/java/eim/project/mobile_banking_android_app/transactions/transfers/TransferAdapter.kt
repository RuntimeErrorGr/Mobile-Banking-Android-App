package eim.project.mobile_banking_android_app.transactions.transfers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eim.project.mobile_banking_android_app.databinding.ItemTransferBinding

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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransferHolder {
        binding = ItemTransferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransferHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return transfers.size
    }

    override fun onBindViewHolder(holder: TransferHolder, position: Int) {
        val account = transfers[position]
        holder.transfer_amount.text = "$"+account.amount.toString()
        holder.transfer_date.text = account.date
        holder.transfer_currency.text = account.currency
        holder.transfer_iban_dest.text = account.destIban
        holder.transfer_iban_src.text = account.srcIban
    }
}