package com.songjunyi.cardgame

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import com.songjunyi.cardgame.model.CardMatchingGame
import com.songjunyi.cardgame.model.Suit

/** View only: renders model state and reports button events to the controller. */
class GameBoardView(
    private val activity: Activity,
    private val onChoose: (Int) -> Unit
) : GridLayout(activity) {
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private val cardIds = intArrayOf(R.id.card_0, R.id.card_1, R.id.card_2, R.id.card_3,
        R.id.card_4, R.id.card_5, R.id.card_6, R.id.card_7, R.id.card_8, R.id.card_9,
        R.id.card_10, R.id.card_11, R.id.card_12, R.id.card_13, R.id.card_14, R.id.card_15)
    private val columns = if (resources.configuration.screenWidthDp >= 600) 8 else 4
    private val cards: List<Button>
    init {
        id = R.id.card_grid
        columnCount = columns
        rowCount = 16 / columns
        val width = (minOf(resources.configuration.screenWidthDp, 720) - 36 - 30 - columns * 8) / columns
        cards = (0 until 16).map { index ->
            Button(activity).apply {
                id = cardIds[index]
                isAllCaps = false
                textSize = if (columns == 8) 20f else 24f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 0)
                minWidth = 0
                minimumWidth = 0
                minHeight = 0
                minimumHeight = 0
                setOnClickListener { onChoose(index) }
                addView(this, LayoutParams().apply {
                    this.width = dp(width)
                    height = dp((width * 1.30f).toInt())
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                    rowSpec = spec(index / columns, TOP)
                    columnSpec = spec(index % columns)
                })
            }
        }
    }
    fun render(game: CardMatchingGame) {
        val suits = resources.getStringArray(R.array.suits)
        val ranks = resources.getStringArray(R.array.ranks)
        cards.forEachIndexed { index, button ->
            val card = game.cardAtIndex(index)
            val suit = suits[card.suit.ordinal]
            val rank = ranks[card.rank - 1]
            button.text = if (card.isChosen) activity.getString(R.string.card_face, rank, suit)
                else activity.getString(R.string.card_back)
            button.contentDescription = activity.getString(
                if (card.isMatched) R.string.card_matched_accessibility
                else if (card.isChosen) R.string.card_open_accessibility
                else R.string.card_hidden_accessibility, index + 1, suit, rank)
            button.setTextColor(if (!card.isChosen) Color.rgb(154, 181, 232) else
                if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) Color.rgb(193, 47, 84)
                else Color.rgb(20, 37, 63))
            button.background = if (card.isChosen) GradientDrawable().apply {
                setColor(if (card.isMatched) Color.rgb(202, 248, 226) else Color.rgb(246, 248, 255))
                cornerRadius = dp(12).toFloat()
            } else GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(31, 48, 81), Color.rgb(43, 60, 103))).apply {
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.rgb(82, 104, 155))
            }
            button.backgroundTintList = null
            button.isEnabled = !card.isMatched && !game.isFinished
            button.alpha = if (card.isMatched) .82f else 1f
        }
    }
}
