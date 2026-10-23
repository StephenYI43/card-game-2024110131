package com.songjunyi.cardgame.model

import com.songjunyi.cardgame.BoardLayout
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class GameModelTest {
    private fun c(suit: Suit, rank: Int) = Card(suit, rank)
    private fun rankTriple() = listOf(c(Suit.HEARTS, 7), c(Suit.CLUBS, 7), c(Suit.SPADES, 7))
    @Test fun threeEqualRanksScoreSix() {
        assertEquals(6, rankTriple()[0].match(rankTriple().drop(1).toTypedArray()))
    }
    @Test fun threeEqualSuitsScoreThree() {
        assertEquals(3, c(Suit.HEARTS, 1).match(arrayOf(c(Suit.HEARTS, 2), c(Suit.HEARTS, 3))))
    }
    @Test fun rankRuleHasPriorityWhenBothConditionsHold() {
        val card = c(Suit.HEARTS, 7)
        assertEquals(6, card.match(arrayOf(card.copy(), card.copy())))
    }
    @Test fun wrongCardinalityCannotMatch() {
        val card = c(Suit.HEARTS, 1)
        assertEquals(0, card.match(emptyArray()))
        assertEquals(0, card.match(arrayOf(card)))
        assertEquals(0, card.match(arrayOf(card, card, card)))
    }
    @Test fun partiallyEqualTripleCannotMatch() {
        assertEquals(0, c(Suit.HEARTS, 1).match(arrayOf(c(Suit.CLUBS, 1), c(Suit.SPADES, 2))))
    }
    @Test fun deckDraws52UniqueCardsWithoutReplacement() {
        val deck = Deck(Random(42))
        val cards = (1..52).map { requireNotNull(deck.drawRandomCard()) }
        assertEquals(52, cards.map { it.suit to it.rank }.distinct().size)
        assertEquals(0, deck.remainingCount)
        assertNull(deck.drawRandomCard())
    }
    @Test fun invalidCountsAreRejected() {
        for (count in listOf(0, 2, 25, 54)) {
            assertThrows(IllegalArgumentException::class.java) { CardMatchingGame(count) }
        }
    }
    @Test fun firstTwoChoicesWaitForThird() {
        val game = CardMatchingGame(rankTriple())
        assertEquals(Choice.SELECTED, game.chooseCardAtIndex(0))
        assertEquals(Choice.SELECTED, game.chooseCardAtIndex(1))
        assertEquals(-2, game.score)
        assertEquals(0, game.matchedCount)
        assertTrue(game.cardAtIndex(0).isChosen && game.cardAtIndex(1).isChosen)
    }
    @Test fun turningBackIsFreeAndDoesNotCountAsNewFlip() {
        val game = CardMatchingGame(rankTriple())
        game.chooseCardAtIndex(0)
        assertEquals(Choice.UNSELECTED, game.chooseCardAtIndex(0))
        assertEquals(-1, game.score)
        assertEquals(1, game.moves)
        assertFalse(game.cardAtIndex(0).isChosen)
    }
    @Test fun rankTripleScoresTwentyOneAndDisablesMatchedCards() {
        val game = CardMatchingGame(rankTriple())
        game.chooseCardAtIndex(0); game.chooseCardAtIndex(1)
        assertEquals(Choice.MATCHED_RANK, game.chooseCardAtIndex(2))
        assertEquals(21, game.score)
        assertEquals(3, game.matchedCount)
        assertTrue(game.isFinished)
        assertEquals(Choice.IGNORED, game.chooseCardAtIndex(0))
        assertEquals(21, game.score)
    }
    @Test fun suitTripleScoresNine() {
        val game = CardMatchingGame(listOf(c(Suit.HEARTS, 1), c(Suit.HEARTS, 2), c(Suit.HEARTS, 3)))
        game.chooseCardAtIndex(0); game.chooseCardAtIndex(1)
        assertEquals(Choice.MATCHED_SUIT, game.chooseCardAtIndex(2))
        assertEquals(9, game.score)
    }
    @Test fun mismatchHidesPreviousTwoAndKeepsNewest() {
        val game = CardMatchingGame(rankTriple() + listOf(c(Suit.HEARTS, 1), c(Suit.CLUBS, 2), c(Suit.SPADES, 3)))
        game.chooseCardAtIndex(3); game.chooseCardAtIndex(4)
        assertEquals(Choice.MISMATCH, game.chooseCardAtIndex(5))
        assertEquals(-5, game.score)
        assertFalse(game.cardAtIndex(3).isChosen || game.cardAtIndex(4).isChosen)
        assertTrue(game.cardAtIndex(5).isChosen)
        assertEquals(0, game.matchedCount)
    }
    @Test fun unselectingOneOfTwoDoesNotPrematurelyMatch() {
        val game = CardMatchingGame(rankTriple())
        game.chooseCardAtIndex(0); game.chooseCardAtIndex(1); game.chooseCardAtIndex(0)
        assertEquals(Choice.SELECTED, game.chooseCardAtIndex(2))
        assertEquals(0, game.matchedCount)
        assertEquals(-3, game.score)
    }
    @Test fun resetClearsTheSameModelInstance() {
        val game = CardMatchingGame(rankTriple())
        (0..2).forEach { game.chooseCardAtIndex(it) }
        game.reset(24, Random(1))
        assertEquals(24, game.count)
        assertEquals(0, game.score)
        assertEquals(0, game.moves)
        assertEquals(0, game.matchedCount)
        assertTrue((0 until game.count).none { game.cardAtIndex(it).isChosen })
    }
    @Test fun resetSupportsLandscape36CardsAndUniqueDeck() {
        val game = CardMatchingGame(24, Random(1))
        game.reset(36, Random(2))
        assertEquals(36, game.count)
        assertEquals(36, game.snapshot().cards.map { it.suit to it.rank }.distinct().size)
    }
    @Test fun invalidResetLeavesExistingGameUntouched() {
        val game = CardMatchingGame(24, Random(1))
        game.chooseCardAtIndex(0)
        val before = game.snapshot()
        assertThrows(IllegalArgumentException::class.java) { game.reset(25) }
        assertEquals(before, game.snapshot())
    }
    @Test fun snapshotPreservesTwoChosenCardsAndIsIndependent() {
        val original = CardMatchingGame(24, Random(3))
        original.chooseCardAtIndex(0); original.chooseCardAtIndex(1)
        val restored = CardMatchingGame.restore(original.snapshot())
        assertEquals(original.snapshot(), restored.snapshot())
        restored.chooseCardAtIndex(0)
        assertTrue(original.cardAtIndex(0).isChosen)
    }
    @Test fun snapshotPreservesMatchedTripleAndScore() {
        val game = CardMatchingGame(rankTriple())
        (0..2).forEach { game.chooseCardAtIndex(it) }
        val restored = CardMatchingGame.restore(game.snapshot())
        assertEquals(21, restored.score)
        assertEquals(3, restored.matchedCount)
        assertTrue(restored.isFinished)
    }
    @Test fun snapshotRejectsThreePendingChoices() {
        assertThrows(IllegalArgumentException::class.java) {
            CardMatchingGame.restore(GameSnapshot(rankTriple().map { it.copy(isChosen = true) }, -3, 3))
        }
    }
    @Test fun snapshotRejectsDuplicateCardsAndPartialMatchedSet() {
        assertThrows(IllegalArgumentException::class.java) {
            CardMatchingGame.restore(GameSnapshot(List(3) { c(Suit.HEARTS, 1) }, 0, 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CardMatchingGame.restore(GameSnapshot(rankTriple().mapIndexed { i, card ->
                card.copy(isChosen = i == 0, isMatched = i == 0)
            }, 0, 0))
        }
    }
    @Test fun terminalStateStopsUnmatchableRemainingCards() {
        val game = CardMatchingGame(listOf(c(Suit.HEARTS, 1), c(Suit.CLUBS, 2), c(Suit.SPADES, 3)))
        assertFalse(game.hasAvailableMatch)
        assertTrue(game.isFinished)
        assertEquals(Choice.IGNORED, game.chooseCardAtIndex(0))
    }
    @Test fun bestScoreNeverDropsAndProfileEditRetainsIt() {
        val session = PlayerSession(PlayerProfile("fixture", "2024110131"))
        session.recordScore(21); session.recordScore(-5)
        session.updateProfile(session.profile.copy(name = "edited", bestScore = 0))
        assertEquals(21, session.profile.bestScore)
        assertEquals("edited", session.profile.name)
    }
    @Test fun orientationPolicyUses24Or36AndFourOrNineColumns() {
        assertEquals(24, BoardLayout.newRoundCount(false))
        assertEquals(36, BoardLayout.newRoundCount(true))
        assertEquals(4, BoardLayout.columns(false))
        assertEquals(9, BoardLayout.columns(true))
    }
}
