package com.songjunyi.cardgame.model

import java.util.Random

data class GameSnapshot(val cards: List<Card>, val score: Int, val moves: Int)
enum class Choice { SELECTED, UNSELECTED, MATCHED_RANK, MATCHED_SUIT, MISMATCH, IGNORED }

/** Course rules retained: match multiplier 4, mismatch -2, new selection -1. */
class CardMatchingGame private constructor(
    private val cards: MutableList<Card>,
    initialScore: Int,
    initialMoves: Int
) {
    constructor(count: Int = 16, random: Random = Random()) : this(deal(count, random), 0, 0)
    internal constructor(initial: List<Card>) : this(initial.map { it.copy() }.toMutableList(), 0, 0)
    var score = initialScore
        private set
    var moves = initialMoves
        private set
    val count get() = cards.size
    val matchedCount get() = cards.count { it.isMatched }
    val hasAvailableMatch get() = cards.indices.any { first ->
        !cards[first].isMatched && (first + 1 until count).any { second ->
            !cards[second].isMatched && cards[first].match(arrayOf(cards[second])) > 0
        }
    }
    val isFinished get() = matchedCount == count || !hasAvailableMatch
    fun cardAtIndex(index: Int): Card = cards[index]
    fun chooseCardAtIndex(index: Int): Choice {
        val card = cardAtIndex(index)
        if (card.isMatched) return Choice.IGNORED
        if (card.isChosen) {
            card.isChosen = false
            return Choice.UNSELECTED
        }
        val other = cards.firstOrNull { it.isChosen && !it.isMatched }
        var choice = Choice.SELECTED
        if (other != null) {
            val matchScore = card.match(arrayOf(other))
            if (matchScore > 0) {
                score += matchScore * MATCH_BONUS
                other.isMatched = true
                card.isMatched = true
                choice = if (matchScore == 4) Choice.MATCHED_RANK else Choice.MATCHED_SUIT
            } else {
                score -= MISMATCH_PENALTY
                other.isChosen = false
                choice = Choice.MISMATCH
            }
        }
        score -= COST_TO_CHOOSE
        moves++
        card.isChosen = true
        return choice
    }
    fun snapshot() = GameSnapshot(cards.map { it.copy() }, score, moves)
    companion object {
        const val MISMATCH_PENALTY = 2
        const val MATCH_BONUS = 4
        const val COST_TO_CHOOSE = 1
        private fun deal(count: Int, random: Random): MutableList<Card> {
            require(count in 2..52)
            val deck = Deck(random)
            return MutableList(count) { requireNotNull(deck.drawRandomCard()) }
        }
        fun restore(state: GameSnapshot): CardMatchingGame {
            require(state.cards.size in 2..52 && state.moves >= 0)
            require(state.cards.count { it.isChosen && !it.isMatched } <= 1)
            require(state.cards.all { !it.isMatched || it.isChosen })
            require(state.cards.map { it.suit to it.rank }.distinct().size == state.cards.size)
            return CardMatchingGame(state.cards.map { it.copy() }.toMutableList(), state.score, state.moves)
        }
    }
}
