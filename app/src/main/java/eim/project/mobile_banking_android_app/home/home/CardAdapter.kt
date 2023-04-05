package eim.project.mobile_banking_android_app.home.home

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eim.project.mobile_banking_android_app.databinding.ItemCardBinding

class CardAdapter : RecyclerView.Adapter<CardAdapter.CardHolder> {

    private lateinit var binding: ItemCardBinding
    private var context: Context
    private var cards: ArrayList<Card>

    constructor(context: Context, cards: ArrayList<Card>) : super() {
        this.context = context
        this.cards = cards
    }


    inner class CardHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progress_bar = binding.progressBar
        val card_number = binding.cardNumber
        val card_holder_name = binding.cardHolderName
        val card_expiration_date = binding.expiryDate
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder {
        /*
        * Bind
        * Inflate
        * */
        binding = ItemCardBinding.inflate(LayoutInflater.from(context), parent, false)
        return CardHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    override fun onBindViewHolder(holder: CardHolder, position: Int) {
        /*
        * Get data
        * Set data
        * handle click
        * */
        holder.progress_bar.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            holder.progress_bar.visibility = View.GONE
        }, 1500)

        // Get data
        val card = cards[position]
        val cardNumber = card.number
        val cardHolderName = card.nameOnCard
        val cardExpirationDate = card.expirationDate

        //Set data
        holder.card_number.text = cardNumber.chunked(4).joinToString(" ")
        holder.card_holder_name.text = cardHolderName
        holder.card_expiration_date.text = cardExpirationDate
    }
}