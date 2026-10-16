package com.songjunyi.cardgame.model

import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class GameModelTest {
    private fun card(suit: Suit, rank: Int) = Card(suit, rank)
    @Test fun rankMatchHasPriority() {
        assertEquals(4, card(Suit.HEARTS, 1).match(arrayOf(card(Suit.SPADES, 1))))
        assertEquals(4, card(Suit.HEARTS, 1).match(arrayOf(card(Suit.HEARTS, 1))))
    }
    @Test fun sameSuitMatchesForOne() {
        assertEquals(1, card(Suit.HEARTS, 1).match(arrayOf(card(Suit.HEARTS, 2))))
    }
    @Test fun unrelatedAndMultipleCardsDoNotMatch() {
        val c = card(Suit.HEARTS, 1)
        assertEquals(0, c.match(arrayOf(card(Suit.CLUBS, 2))))
        assertEquals(0, c.match(emptyArray()))
        assertEquals(0, c.match(arrayOf(c, c)))
    }
    @Test fun deckDraws52UniqueCardsWithoutReplacement() {
        val deck = Deck(Random(42))
        val drawn = (1..52).map { requireNotNull(deck.drawRandomCard()) }
        assertEquals(52, drawn.map { it.suit to it.rank }.distinct().size)
        assertEquals(0, deck.remainingCount)
        assertNull(deck.drawRandomCard())
    }
    @Test(expected = IllegalArgumentException::class) fun invalidCountRejected() {
        CardMatchingGame(53)
    }
    @Test fun firstFlipCostsOneAndTurningBackIsFree() {
        val game = CardMatchingGame(16, Random(1))
        assertEquals(Choice.SELECTED, game.chooseCardAtIndex(0))
        assertEquals(-1, game.score)
        assertTrue(game.cardAtIndex(0).isChosen)
        assertEquals(Choice.UNSELECTED, game.chooseCardAtIndex(0))
        assertEquals(-1, game.score)
        assertEquals(1, game.moves)
    }
    @Test fun rankPairScoresFourteenAndCannotBeChosenAgain() {
        val game = CardMatchingGame(listOf(card(Suit.HEARTS, 1), card(Suit.CLUBS, 1)))
        game.chooseCardAtIndex(0)
        assertEquals(Choice.MATCHED_RANK, game.chooseCardAtIndex(1))
        assertEquals(14, game.score)
        assertEquals(2, game.matchedCount)
        assertTrue(game.isFinished)
        assertEquals(Choice.IGNORED, game.chooseCardAtIndex(0))
        assertEquals(14, game.score)
        assertEquals(2, game.moves)
    }
    @Test fun suitPairScoresTwo() {
        val game = CardMatchingGame(listOf(card(Suit.HEARTS, 1), card(Suit.HEARTS, 2)))
        game.chooseCardAtIndex(0)
        assertEquals(Choice.MATCHED_SUIT, game.chooseCardAtIndex(1))
        assertEquals(2, game.score)
    }
    @Test fun mismatchTurnsPreviousDownAndLeavesCurrentChosen() {
        val game = CardMatchingGame(listOf(card(Suit.HEARTS, 1), card(Suit.CLUBS, 2)))
        game.chooseCardAtIndex(0)
        assertEquals(Choice.MISMATCH, game.chooseCardAtIndex(1))
        assertEquals(-4, game.score)
        assertFalse(game.cardAtIndex(0).isChosen)
        assertTrue(game.cardAtIndex(1).isChosen)
        assertEquals(0, game.matchedCount)
    }
    @Test fun snapshotRestoresAndIsIndependent() {
        val original = CardMatchingGame(16, Random(2))
        original.chooseCardAtIndex(0)
        val restored = CardMatchingGame.restore(original.snapshot())
        assertEquals(original.score, restored.score)
        assertEquals(original.moves, restored.moves)
        assertTrue(restored.cardAtIndex(0).isChosen)
        restored.chooseCardAtIndex(0)
        assertTrue(original.cardAtIndex(0).isChosen)
    }
    @Test(expected = IllegalArgumentException::class) fun invalidSnapshotRejected() {
        CardMatchingGame.restore(GameSnapshot(listOf(
            card(Suit.HEARTS, 1).copy(isChosen = true),
            card(Suit.CLUBS, 2).copy(isChosen = true)), 0, 0))
    }
    @Test fun terminalStateRecognizesNoFurtherMatch() {
        val game = CardMatchingGame(listOf(card(Suit.HEARTS, 1), card(Suit.CLUBS, 2)))
        assertFalse(game.hasAvailableMatch)
        assertTrue(game.isFinished)
    }
    @Test fun newGameResetsScoreMovesAndFlags() {
        val game = CardMatchingGame(16, Random(6))
        game.chooseCardAtIndex(0)
        val fresh = CardMatchingGame(16, Random(7))
        assertEquals(0, fresh.score)
        assertEquals(0, fresh.moves)
        assertEquals(0, fresh.matchedCount)
        assertTrue((0 until fresh.count).none { fresh.cardAtIndex(it).isChosen })
    }
    @Test fun bestScoreNeverDropsAndProfileEditRetainsIt() {
        val session = PlayerSession(PlayerProfile("fixture", "2024110131"))
        session.recordScore(14)
        session.recordScore(-4)
        session.updateProfile(session.profile.copy(name = "edited", bestScore = 0))
        assertEquals(14, session.profile.bestScore)
        assertEquals("edited", session.profile.name)
        assertEquals(0, PlayerSession(PlayerProfile("fixture", "2024110131")).profile.bestScore)
    }
}
