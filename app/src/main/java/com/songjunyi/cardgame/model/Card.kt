package com.songjunyi.cardgame.model

enum class Suit { HEARTS, DIAMONDS, SPADES, CLUBS }

/** Based on the course Card: same rank wins before same suit. No Android/UI dependency. */
data class Card(
    val suit: Suit,
    val rank: Int,
    var isChosen: Boolean = false,
    var isMatched: Boolean = false
) {
    init { require(rank in 1..13) }
    fun match(otherCards: Array<Card>): Int {
        if (otherCards.size != 1) return 0
        val other = otherCards[0]
        return when {
            other.rank == rank -> 4
            other.suit == suit -> 1
            else -> 0
        }
    }
}
