package com.songjunyi.cardgame.model

import java.util.Random

data class GameSnapshot(val cards: List<Card>, val score: Int, val moves: Int, val groupSize: Int = 3) : java.io.Serializable {
    private companion object { const val serialVersionUID = 1L }
}
enum class Choice { SELECTED, UNSELECTED, MATCHED_RANK, MATCHED_SUIT, MISMATCH, IGNORED }

/** MVC model: three cards per match, selection -1, mismatch -2, bonus multiplier 4. */
class CardMatchingGame private constructor(
    private val cards: MutableList<Card>,
    initialScore: Int,
    initialMoves: Int,
    val groupSize: Int
) {
    constructor(count: Int = 24, random: Random = Random(), groupSize: Int = 3) :
        this(deal(count, random, groupSize), 0, 0, groupSize)
    internal constructor(initial: List<Card>, groupSize: Int = 3) :
        this(initial.map { it.copy() }.toMutableList(), 0, 0, groupSize)
    var score = initialScore
        private set
    var moves = initialMoves
        private set
    val count get() = cards.size
    val matchedCount get() = cards.count { it.isMatched }
    val hasAvailableMatch get(): Boolean {
        val available = cards.filterNot { it.isMatched }
        return available.groupingBy { it.rank }.eachCount().values.any { it >= groupSize } ||
            available.groupingBy { it.suit }.eachCount().values.any { it >= groupSize }
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
        if (others.size == groupSize - 1) {
            val sameRank = others.all { it.rank == card.rank }
            val matchScore = if (groupSize == 3) card.match(others.toTypedArray()) else when {
                sameRank -> 4
                others.all { it.suit == card.suit } -> 1
                else -> 0
            }
            if (matchScore > 0) {
                score += matchScore * MATCH_BONUS
                others.forEach { it.isMatched = true }
                card.isMatched = true
                choice = if (sameRank) Choice.MATCHED_RANK else Choice.MATCHED_SUIT
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
        val freshCards = deal(count, random, groupSize)
        cards.clear()
        cards.addAll(freshCards)
        score = 0
        moves = 0
    }
    fun snapshot() = GameSnapshot(cards.map { it.copy() }, score, moves, groupSize)
    companion object {
        const val MISMATCH_PENALTY = 2
        const val MATCH_BONUS = 4
        const val COST_TO_CHOOSE = 1
        private fun deal(count: Int, random: Random, groupSize: Int): MutableList<Card> {
            require(groupSize in 2..3 && count in groupSize..52 && count % groupSize == 0)
            val deck = Deck(random)
            return MutableList(count) { requireNotNull(deck.drawRandomCard()) }
        }
        fun restore(state: GameSnapshot): CardMatchingGame {
            val size = state.groupSize
            require(size in 2..3 && state.cards.size in size..52 && state.cards.size % size == 0 && state.moves >= 0)
            require(state.cards.count { it.isChosen && !it.isMatched } < size)
            require(state.cards.count { it.isMatched } % size == 0)
            require(state.cards.all { !it.isMatched || it.isChosen })
            require(state.cards.map { it.suit to it.rank }.distinct().size == state.cards.size)
            return CardMatchingGame(state.cards.map { it.copy() }.toMutableList(), state.score, state.moves, size)
        }
    }
}
