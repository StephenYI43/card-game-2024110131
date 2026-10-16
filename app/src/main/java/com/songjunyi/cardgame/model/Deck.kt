package com.songjunyi.cardgame.model

import java.util.Random

class Deck(private val random: Random = Random()) {
    private val cards = Suit.entries.flatMap { suit ->
        (1..13).map { rank -> Card(suit, rank) }
    }.toMutableList()
    val remainingCount get() = cards.size
    fun drawRandomCard(): Card? =
        if (cards.isEmpty()) null else cards.removeAt(random.nextInt(cards.size))
}
