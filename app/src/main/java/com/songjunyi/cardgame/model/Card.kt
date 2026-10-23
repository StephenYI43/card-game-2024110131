package com.songjunyi.cardgame.model

enum class Suit { HEARTS, DIAMONDS, SPADES, CLUBS }

/** Lab 5: a three-card rank match wins before a three-card suit match. */
data class Card(
    val suit: Suit,
    val rank: Int,
    var isChosen: Boolean = false,
    var isMatched: Boolean = false
) {
    init { require(rank in 1..13) }
    fun match(otherCards: Array<Card>): Int {
        if (otherCards.size != 2) return 0
        return when {
            otherCards.all { it.rank == rank } -> 6
            otherCards.all { it.suit == suit } -> 3
            else -> 0
        }
    }
}
