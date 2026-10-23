package com.songjunyi.cardgame.model

import java.util.Random

data class GameSnapshot(val cards: List<Card>, val score: Int, val moves: Int)
enum class Choice { SELECTED, UNSELECTED, MATCHED_RANK, MATCHED_SUIT, MISMATCH, IGNORED }

/** MVC model: three cards per match, selection -1, mismatch -2, bonus multiplier 4. */
class CardMatchingGame private constructor(
    private val cards: MutableList<Card>,
    initialScore: Int,
    initialMoves: Int
) {
    constructor(count: Int = 24, random: Random = Random()) : this(deal(count, random), 0, 0)
    internal constructor(initial: List<Card>) : this(initial.map { it.copy() }.toMutableList(), 0, 0)
    var score = initialScore
        private set
    var moves = initialMoves
        private set
    val count get() = cards.size
    val matchedCount get() = cards.count { it.isMatched }
    val hasAvailableMatch get(): Boolean {
        val available = cards.filterNot { it.isMatched }
        return available.groupingBy { it.rank }.eachCount().values.any { it >= 3 } ||
            available.groupingBy { it.suit }.eachCount().values.any { it >= 3 }
    }
    val isFinished get() = matchedCount == count || !hasAvailableMatch
    fun cardAtIndex(index: Int): Card = cards[index]
    fun chooseCardAtIndex(index: Int): Choice {
        val card = cardAtIndex(index)
        if (card.isMatched || isFinished) return Choice.IGNORED
        if (card.isChosen) {
            card.isChosen = false
            return Choice.UNSELECTED
        }
        val others = cards.filter { it.isChosen && !it.isMatched }
        var choice = Choice.SELECTED
        if (others.size == 2) {
            val matchScore = card.match(others.toTypedArray())
            if (matchScore > 0) {
                score += matchScore * MATCH_BONUS
                others.forEach { it.isMatched = true }
                card.isMatched = true
                choice = if (matchScore == 6) Choice.MATCHED_RANK else Choice.MATCHED_SUIT
            } else {
                score -= MISMATCH_PENALTY
                others.forEach { it.isChosen = false }
                choice = Choice.MISMATCH
            }
        }
        score -= COST_TO_CHOOSE
        moves++
        card.isChosen = true
        return choice
    }
    fun reset(count: Int = this.count, random: Random = Random()) {
        val freshCards = deal(count, random)
        cards.clear()
        cards.addAll(freshCards)
        score = 0
        moves = 0
    }
    fun snapshot() = GameSnapshot(cards.map { it.copy() }, score, moves)
    companion object {
        const val MISMATCH_PENALTY = 2
        const val MATCH_BONUS = 4
        const val COST_TO_CHOOSE = 1
        private fun deal(count: Int, random: Random): MutableList<Card> {
            require(count in 3..52 && count % 3 == 0)
            val deck = Deck(random)
            return MutableList(count) { requireNotNull(deck.drawRandomCard()) }
        }
        fun restore(state: GameSnapshot): CardMatchingGame {
            require(state.cards.size in 3..52 && state.cards.size % 3 == 0 && state.moves >= 0)
            require(state.cards.count { it.isChosen && !it.isMatched } <= 2)
            require(state.cards.count { it.isMatched } % 3 == 0)
            require(state.cards.all { !it.isMatched || it.isChosen })
            require(state.cards.map { it.suit to it.rank }.distinct().size == state.cards.size)
            return CardMatchingGame(state.cards.map { it.copy() }.toMutableList(), state.score, state.moves)
        }
    }
}
