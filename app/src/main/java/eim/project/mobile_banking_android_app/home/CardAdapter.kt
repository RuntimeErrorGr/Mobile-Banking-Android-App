package eim.project.mobile_banking_android_app.home

import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eim.project.mobile_banking_android_app.databinding.ItemCardBinding
import java.util.*

class CardAdapter : RecyclerView.Adapter<CardAdapter.CardHolder> {

    private lateinit var binding: ItemCardBinding
    private var context: Context
    var cards: ArrayList<Card>


    constructor(context: Context, cards: ArrayList<Card>) : super() {
        this.context = context
        this.cards = cards
    }


    inner class CardHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progress_bar = binding.progressBar
        val card_number = binding.cardNumber
        val card_holder_name = binding.cardHolderName
        val card_expiration_date = binding.expiryDate
        val card_expirtaion_date_label = binding.expiryDateLabel

        fun bind(card: Card) {
            card_number.text = maskCardNumber(card.number)
            card_holder_name.text = maskName(card.nameOnCard)
            card_expiration_date.text = maskExpirationDate()
            card_expirtaion_date_label.visibility = View.VISIBLE
        }

        init {
            itemView.setOnClickListener {
                val card = cards[adapterPosition]
                if (card.masked) {
                    // Show real data
                    card_number.text = card.number.chunked(4).joinToString(" ")
                    card_holder_name.text = card.nameOnCard.uppercase(Locale.ROOT)
                    card_expiration_date.text = card.expirationDate
                    cards[adapterPosition] = card.copy(masked = false)
                } else {
                    // Mask the data
                    card_number.text = maskCardNumber(card.number)
                    card_holder_name.text = maskName(card.nameOnCard)
                    card_expiration_date.text = maskExpirationDate()
                    // Update the card with the masked data
                    cards[adapterPosition] = card.copy(masked = true)
                }
                card.masked = !card.masked
            }
        }

        fun maskCardNumber(cardNumber: String?): String {
            if (cardNumber.isNullOrEmpty()) {
                return ""
            }
            return cardNumber.substring(cardNumber.length - 4).padStart(cardNumber.length, '*').chunked(4).joinToString(" ")
        }

        fun maskName(name: String?): String {
            if (name.isNullOrEmpty()) {
                return ""
            }
            return name.replace(Regex("[A-Za-z]"), "*")
        }

        fun maskExpirationDate(): String {
            return "--/--"
        }
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
        // Hide text views and show progress bar
        holder.progress_bar.visibility = View.VISIBLE
        holder.card_number.visibility = View.INVISIBLE
        holder.card_holder_name.visibility = View.INVISIBLE
        holder.card_expiration_date.visibility = View.INVISIBLE
        holder.card_expirtaion_date_label.visibility = View.INVISIBLE

        // Get data
        val card = cards[position]
        val cardNumber = if (card.masked) holder.maskCardNumber(card.number) else card.number.chunked(4).joinToString(" ")
        val cardHolderName = if (card.masked) holder.maskName(card.nameOnCard) else card.nameOnCard.uppercase(Locale.ROOT)
        val cardExpirationDate = if (card.masked) holder.maskExpirationDate() else card.expirationDate

        //Set data
        holder.card_number.text = cardNumber
        holder.card_holder_name.text = cardHolderName
        holder.card_expiration_date.text = cardExpirationDate


        object : CountDownTimer(850, 850) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                holder.progress_bar.visibility = View.INVISIBLE
                holder.card_number.visibility = View.VISIBLE
                holder.card_holder_name.visibility = View.VISIBLE
                holder.card_expiration_date.visibility = View.VISIBLE
                holder.card_expirtaion_date_label.visibility = View.VISIBLE
            }
        }.start()
    }
}