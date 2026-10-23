package com.songjunyi.cardgame

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.songjunyi.cardgame.model.*

/** Controller owns the model, adapter callbacks, reset and state restoration. */
class GameActivity : ScreenActivity() {
    private lateinit var game: CardMatchingGame
    private var feedback = R.string.first_prompt
    private lateinit var adapter: CardAdapter
    private lateinit var board: RecyclerView
    private lateinit var score: TextView
    private lateinit var best: TextView
    private lateinit var moves: TextView
    private lateinit var matched: TextView
    private lateinit var status: TextView
    private lateinit var subtitle: TextView
    private val landscape get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = if (savedInstanceState == null) CardMatchingGame(BoardLayout.newRoundCount(landscape)) else {
            val codes = requireNotNull(savedInstanceState.getIntArray("cards"))
            val chosen = requireNotNull(savedInstanceState.getBooleanArray("chosen"))
            val matchedFlags = requireNotNull(savedInstanceState.getBooleanArray("matched"))
            feedback = savedInstanceState.getInt("feedback", R.string.first_prompt)
            session.recordScore(savedInstanceState.getInt("best"))
            CardMatchingGame.restore(GameSnapshot(codes.mapIndexed { i, code ->
                Card(Suit.entries[code / 13], code % 13 + 1, chosen[i], matchedFlags[i])
            }, savedInstanceState.getInt("score"), savedInstanceState.getInt("moves")))
        }
        // RecyclerView is the scrolling surface, never nested in a ScrollView.
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setBackgroundColor(Color.rgb(9, 15, 31))
        }
        setContentView(content)
        content.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                view.setPadding(dp(16) + bars.left, dp(10) + bars.top, dp(16) + bars.right, dp(10) + bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(dp(16) + insets.systemWindowInsetLeft, dp(10) + insets.systemWindowInsetTop,
                    dp(16) + insets.systemWindowInsetRight, dp(10) + insets.systemWindowInsetBottom)
            }
            insets
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        content.addView(header)
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        label(titles, R.string.eyebrow, 10f, accent, true)
        label(titles, R.string.game_title, if (landscape) 19f else 25f, white, true)
        subtitle = label(titles, R.string.game_subtitle, 11f, muted).apply { id = R.id.board_description }
        button(header, R.string.exit_game, false).apply {
            id = R.id.exit_game
            layoutParams = LinearLayout.LayoutParams(dp(80), dp(48))
            setOnClickListener { finish() }
        }
        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = shape(Color.rgb(20, 33, 58), 18f, Color.rgb(47, 65, 101))
        }
        content.addView(summary, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        score = text(summary, getString(R.string.number_format, 0), 32f, white, true).apply {
            id = R.id.game_score
            contentDescription = getString(R.string.score_label)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val detail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        summary.addView(detail, LinearLayout.LayoutParams(0, -2, 1f))
        best = label(detail, R.string.best_title, 11f, accent).apply { id = R.id.game_best }
        moves = text(detail, getString(R.string.moves_format, 0), 11f, muted).apply { id = R.id.game_moves }
        matched = text(detail, getString(R.string.matched_format, 0, game.count), 11f, muted).apply { id = R.id.game_matched }
        button(summary, R.string.restart_game).apply {
            id = R.id.restart_game
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(48))
            setOnClickListener {
                game.reset(BoardLayout.newRoundCount(landscape))
                feedback = R.string.new_round_prompt
                render()
                board.scrollToPosition(0)
            }
        }
        adapter = CardAdapter(game) { index ->
            feedback = when (game.chooseCardAtIndex(index)) {
                Choice.SELECTED -> R.string.selected_prompt
                Choice.UNSELECTED -> R.string.unselected_prompt
                Choice.MATCHED_RANK -> R.string.rank_match_prompt
                Choice.MATCHED_SUIT -> R.string.suit_match_prompt
                Choice.MISMATCH -> R.string.mismatch_prompt
                Choice.IGNORED -> feedback
            }
            session.recordScore(game.score)
            render()
        }.apply { cardHeight = if (landscape) 70 else 86 }
        board = RecyclerView(this).apply {
            id = R.id.card_grid
            layoutManager = GridLayoutManager(this@GameActivity, BoardLayout.columns(landscape))
            adapter = this@GameActivity.adapter
            itemAnimator = null
            clipToPadding = false
            setPadding(0, dp(8), 0, dp(8))
        }
        content.addView(board, LinearLayout.LayoutParams(-1, 0, 1f))
        status = text(content, getString(R.string.first_prompt), 12f, accent).apply {
            id = R.id.game_feedback
            minHeight = dp(36)
        }
        if (!landscape) label(content, R.string.compact_rules, 11f, muted)
        render()
    }
    private fun render() {
        score.text = getString(R.string.number_format, game.score)
        best.text = getString(R.string.best_format, session.profile.bestScore)
        moves.text = getString(R.string.moves_format, game.moves)
        matched.text = getString(R.string.matched_format, game.matchedCount, game.count)
        subtitle.text = getString(R.string.board_description, game.count, BoardLayout.columns(landscape))
        status.text = if (game.isFinished) getString(
            if (game.matchedCount == game.count) R.string.won_prompt else R.string.ended_prompt, game.score)
            else getString(feedback)
        adapter.refresh()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        val state = game.snapshot()
        outState.putIntArray("cards", state.cards.map { it.suit.ordinal * 13 + it.rank - 1 }.toIntArray())
        outState.putBooleanArray("chosen", state.cards.map { it.isChosen }.toBooleanArray())
        outState.putBooleanArray("matched", state.cards.map { it.isMatched }.toBooleanArray())
        outState.putInt("score", state.score)
        outState.putInt("moves", state.moves)
        outState.putInt("feedback", feedback)
        outState.putInt("best", session.profile.bestScore)
        super.onSaveInstanceState(outState)
    }
}
