package com.songjunyi.cardgame.model

import com.songjunyi.cardgame.Archive
import java.io.*
import org.junit.Assert.*
import org.junit.Test

class ArchiveTest {
    @Test fun pairResolvesOnSecondCard() {
        val game = CardMatchingGame(listOf(Card(Suit.HEARTS, 3), Card(Suit.CLUBS, 3)), 2)
        assertEquals(Choice.SELECTED, game.chooseCardAtIndex(0))
        assertEquals(Choice.MATCHED_RANK, game.chooseCardAtIndex(1))
        assertEquals(14, game.score)
        assertTrue(game.isFinished)
    }
    @Test fun roundTripPreservesProfileAndBothBoards() {
        val pair = CardMatchingGame(16, groupSize = 2)
        val triple = CardMatchingGame()
        pair.chooseCardAtIndex(0)
        triple.chooseCardAtIndex(1)
        triple.chooseCardAtIndex(2)
        val original = Archive(PlayerProfile("测试玩家", "2024110131", phone = "10086"), pair.snapshot(), triple.snapshot())
        val data = ByteArrayOutputStream()
        ObjectOutputStream(data).use { it.writeObject(original) }
        val restored = ObjectInputStream(ByteArrayInputStream(data.toByteArray())).use { it.readObject() as Archive }
        assertEquals(original, restored)
        assertEquals(2, CardMatchingGame.restore(restored.pair).groupSize)
        assertEquals(-2, CardMatchingGame.restore(restored.triple).score)
    }
}
