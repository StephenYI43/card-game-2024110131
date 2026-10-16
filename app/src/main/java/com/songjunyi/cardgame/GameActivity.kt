package com.songjunyi.cardgame

import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import com.songjunyi.cardgame.model.*

/** Controller: applies user actions to the model and renders the View. */
class GameActivity : ScreenActivity() {
    private var game = CardMatchingGame()
    private var feedback = R.string.first_prompt
    private lateinit var board: GameBoardView
    private lateinit var score: TextView
    private lateinit var best: TextView
    private lateinit var moves: TextView
    private lateinit var matched: TextView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { saved ->
            val codes = requireNotNull(saved.getIntArray("cards"))
            val chosen = requireNotNull(saved.getBooleanArray("chosen"))
            val matched = requireNotNull(saved.getBooleanArray("matched"))
            game = CardMatchingGame.restore(GameSnapshot(codes.mapIndexed { index, code ->
                Card(Suit.entries[code / 13], code % 13 + 1, chosen[index], matched[index])
            }, saved.getInt("score"), saved.getInt("moves")))
            feedback = saved.getInt("feedback", R.string.first_prompt)
            session.recordScore(saved.getInt("best"))
        }
        screen(R.string.game_title, R.string.game_subtitle)
        val summary = panel()
        summary.orientation = LinearLayout.HORIZONTAL
        val scoreColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val detailColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        summary.addView(scoreColumn, LinearLayout.LayoutParams(0, -2, 1f))
        summary.addView(detailColumn, LinearLayout.LayoutParams(0, -2, 1f))
        label(scoreColumn, R.string.score_label, 12f, accent)
        score = text(scoreColumn, getString(R.string.number_format, 0), 32f, white, true)
            .apply { id = R.id.game_score }
        best = label(detailColumn, R.string.best_title, 12f, muted).apply { id = R.id.game_best }
        moves = text(detailColumn, getString(R.string.moves_format, 0), 12f, muted).apply { id = R.id.game_moves }
        matched = text(detailColumn, getString(R.string.matched_format, 0, 16), 12f, accent).apply { id = R.id.game_matched }
        val boardPanel = panel()
        board = GameBoardView(this) { index ->
            val result = game.chooseCardAtIndex(index)
            feedback = when (result) {
                Choice.SELECTED -> R.string.selected_prompt
                Choice.UNSELECTED -> R.string.unselected_prompt
                Choice.MATCHED_RANK -> R.string.rank_match_prompt
                Choice.MATCHED_SUIT -> R.string.suit_match_prompt
                Choice.MISMATCH -> R.string.mismatch_prompt
                Choice.IGNORED -> feedback
            }
            session.recordScore(game.score)
            render()
        }
        boardPanel.addView(board)
        status = text(boardPanel, getString(R.string.first_prompt), 13f, accent).apply { id = R.id.game_feedback }
        button(content, R.string.restart_game).apply {
            id = R.id.restart_game
            setOnClickListener { game = CardMatchingGame(); feedback = R.string.new_round_prompt; render() }
        }
        button(content, R.string.exit_game, false).apply {
            id = R.id.exit_game
            setOnClickListener { finish() }
        }
        val rules = panel()
        label(rules, R.string.rules_title, 14f, white, true)
        label(rules, R.string.rules_description, 12f, muted)
        render()
    }
    private fun render() {
        score.text = getString(R.string.number_format, game.score)
        best.text = getString(R.string.best_format, session.profile.bestScore)
        moves.text = getString(R.string.moves_format, game.moves)
        matched.text = getString(R.string.matched_format, game.matchedCount, game.count)
        status.text = if (game.isFinished) getString(
            if (game.matchedCount == game.count) R.string.won_prompt else R.string.ended_prompt, game.score)
            else getString(feedback)
        board.render(game)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        val snapshot = game.snapshot()
        outState.putIntArray("cards", snapshot.cards.map { it.suit.ordinal * 13 + it.rank - 1 }.toIntArray())
        outState.putBooleanArray("chosen", snapshot.cards.map { it.isChosen }.toBooleanArray())
        outState.putBooleanArray("matched", snapshot.cards.map { it.isMatched }.toBooleanArray())
        outState.putInt("score", snapshot.score)
        outState.putInt("moves", snapshot.moves)
        outState.putInt("feedback", feedback)
        outState.putInt("best", session.profile.bestScore)
        super.onSaveInstanceState(outState)
    }
}
