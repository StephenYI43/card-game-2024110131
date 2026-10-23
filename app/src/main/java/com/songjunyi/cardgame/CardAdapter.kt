package com.songjunyi.cardgame

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import com.songjunyi.cardgame.model.CardMatchingGame
import com.songjunyi.cardgame.model.Suit

/** View + adapter only. Scores and matching remain entirely in the model. */
class CardAdapter(private val game: CardMatchingGame, private val onChoose: (Int) -> Unit) :
    RecyclerView.Adapter<CardAdapter.CardHolder>() {
    class CardHolder(val button: Button) : RecyclerView.ViewHolder(button)
    private var cards = game.snapshot().cards
    private var finished = game.isFinished
    var cardHeight = 90
    init { setHasStableIds(true) }
    override fun getItemCount() = cards.size
    override fun getItemId(position: Int) = position.toLong()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder {
        val density = parent.resources.displayMetrics.density
        fun dp(n: Int) = (n * density).toInt()
        val button = Button(parent.context).apply {
            isAllCaps = false
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0)
            minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            layoutParams = RecyclerView.LayoutParams(-1, dp(cardHeight)).apply {
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
        }
        val holder = CardHolder(button)
        button.setOnClickListener {
            val index = holder.bindingAdapterPosition
            if (index != RecyclerView.NO_POSITION) onChoose(index)
        }
        return holder
    }
    override fun onBindViewHolder(holder: CardHolder, position: Int) {
        val button = holder.button
        val res = button.resources
        val density = res.displayMetrics.density
        val card = cards[position]
        val suit = res.getStringArray(R.array.suits)[card.suit.ordinal]
        val rank = res.getStringArray(R.array.ranks)[card.rank - 1]
        // Rebind all mutable properties, including a previously matched recycled view.
        button.text = if (card.isChosen) res.getString(R.string.card_face, rank, suit)
            else res.getString(R.string.card_back)
        button.contentDescription = res.getString(
            if (card.isMatched) R.string.card_matched_accessibility
            else if (card.isChosen) R.string.card_open_accessibility
            else R.string.card_hidden_accessibility, position + 1, suit, rank)
        button.setTextColor(if (!card.isChosen) Color.rgb(168, 193, 255) else
            if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) Color.rgb(193, 47, 84)
            else Color.rgb(20, 37, 63))
        button.background = if (card.isChosen) GradientDrawable().apply {
            setColor(if (card.isMatched) Color.rgb(193, 249, 226) else Color.rgb(246, 248, 255))
            cornerRadius = 12 * density
        } else GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(32, 47, 86), Color.rgb(43, 54, 108))).apply {
            cornerRadius = 12 * density
            setStroke((1 * density).toInt(), Color.rgb(88, 110, 183))
        }
        button.backgroundTintList = null
        button.isEnabled = !card.isMatched && !finished
        button.alpha = if (card.isMatched) .8f else 1f
    }
    fun refresh() {
        val old = cards
        val next = game.snapshot().cards
        val wasFinished = finished
        val nowFinished = game.isFinished
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = next.size
            override fun areItemsTheSame(oldPosition: Int, newPosition: Int) = oldPosition == newPosition
            override fun areContentsTheSame(oldPosition: Int, newPosition: Int) =
                old[oldPosition] == next[newPosition] && wasFinished == nowFinished
        }, false)
        cards = next
        finished = nowFinished
        diff.dispatchUpdatesTo(this)
    }
}
