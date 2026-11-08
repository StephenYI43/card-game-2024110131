@file:Suppress("DEPRECATION")
package com.songjunyi.cardgame

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.graphics.Color
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.songjunyi.cardgame.model.*

private const val CONTAINER = 0x6001
private val accent = Color.rgb(165, 180, 252)
private fun Context.dp(n: Int) = (n * resources.displayMetrics.density).toInt()
private fun Context.column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
private fun LinearLayout.line(value: CharSequence, size: Float = 16f) = TextView(context).apply {
    text = value
    textSize = size
    setTextColor(if (size >= 22) Color.WHITE else accent)
    setPadding(0, context.dp(6), 0, context.dp(6))
    this@line.addView(this)
}
private fun LinearLayout.button(res: Int, action: () -> Unit) = Button(context).apply {
    setText(res)
    isAllCaps = false
    setOnClickListener { action() }
    this@button.addView(this)
}

class FragmentHostActivity : Activity() {
    private lateinit var store: ArchiveStore
    lateinit var profile: PlayerProfile
    lateinit var pair: CardMatchingGame
    lateinit var triple: CardMatchingGame
    private var tab = 0
    private lateinit var tabs: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ArchiveStore(this)
        val saved = store.read()
        profile = saved?.profile ?: PlayerProfile(getString(R.string.default_name), getString(R.string.default_student))
        pair = saved?.pair?.let(CardMatchingGame::restore) ?: CardMatchingGame(16, groupSize = 2)
        triple = saved?.triple?.let(CardMatchingGame::restore) ?: CardMatchingGame(24)
        tab = savedInstanceState?.getInt("tab") ?: 0
        val root = column().apply { setBackgroundColor(Color.rgb(9, 15, 31)) }
        root.setOnApplyWindowInsetsListener { v, inset ->
            v.setPadding(dp(16), inset.systemWindowInsetTop + dp(8), dp(16), inset.systemWindowInsetBottom + dp(8))
            inset
        }
        root.line(getString(R.string.app_name), 24f)
        root.line(getString(R.string.lab6_caption), 13f)
        val bar = LinearLayout(this)
        tabs = listOf(R.string.lab6_profile, R.string.lab6_pair, R.string.lab6_triple).mapIndexed { i, label ->
            Button(this).apply {
                setText(label); isAllCaps = false
                setOnClickListener { switchTo(i) }
                bar.addView(this, LinearLayout.LayoutParams(0, dp(52), 1f))
            }
        }
        root.addView(bar)
        root.addView(FrameLayout(this).apply { id = CONTAINER }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        if (savedInstanceState == null) switchTo(tab) else markTab()
    }
    private fun markTab() = tabs.forEachIndexed { i, button ->
        button.setTextColor(if (i == tab) accent else Color.WHITE)
        button.isSelected = i == tab
    }
    private fun switchTo(next: Int) {
        if (android.os.Build.VERSION.SDK_INT >= 26 && fragmentManager.isStateSaved) return
        tab = next
        val fragment = when (next) { 1 -> PairFragment(); 2 -> TripleFragment(); else -> PlayerFragment() }
        fragmentManager.beginTransaction().replace(CONTAINER, fragment).commit()
        markTab()
    }
    fun persist(): Boolean {
        val written = store.write(Archive(profile, pair.snapshot(), triple.snapshot()))
        if (!written) Toast.makeText(this, R.string.lab6_save_error, Toast.LENGTH_SHORT).show()
        return written
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("tab", tab)
        super.onSaveInstanceState(outState)
    }
}

class PlayerFragment : Fragment() {
    override fun onCreateView(i: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        val host = activity as FragmentHostActivity
        val form = host.column()
        form.line(getString(R.string.lab6_profile_title), 24f)
        form.line(getString(R.string.lab6_profile_hint))
        fun field(label: Int, value: String, id: Int, input: Int): EditText {
            form.line(getString(label))
            return EditText(host).apply {
                this.id = id
                setSingleLine()
                inputType = input
                setText(value)
                form.addView(this)
            }
        }
        val name = field(R.string.name_label, host.profile.name, 0x6031, 1)
        val student = field(R.string.student_label, host.profile.studentId, 0x6032, 2)
        val phone = field(R.string.phone_label, host.profile.phone, 0x6033, 3)
        val email = field(R.string.email_label, host.profile.email, 0x6034, 33)
        form.button(R.string.save_profile) {
            val n = name.text.toString().trim()
            val sid = student.text.toString().trim()
            if (n.isEmpty()) name.error = getString(R.string.lab6_required)
            else if (sid.isEmpty() || sid.any { !it.isDigit() }) student.error = getString(R.string.lab6_required)
            else {
                host.profile = host.profile.copy(name = n, studentId = sid, phone = phone.text.toString().trim(),
                    email = email.text.toString().trim())
                if (host.persist()) Toast.makeText(host, R.string.lab6_saved, Toast.LENGTH_SHORT).show()
            }
        }
        form.line(getString(R.string.lab6_best, host.profile.bestScore))
        return ScrollView(host).apply { addView(form) }
    }
}

abstract class MatchFragment(private val size: Int) : Fragment() {
    override fun onCreateView(i: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        val host = activity as FragmentHostActivity
        val game = if (size == 2) host.pair else host.triple
        val root = host.column()
        root.line(getString(if (size == 2) R.string.lab6_pair else R.string.lab6_triple), 24f)
        root.line(getString(if (size == 2) R.string.lab6_pair_rules else R.string.lab6_triple_rules), 13f)
        val stats = root.line("")
        val feedback = root.line("", 13f)
        lateinit var adapter: CardAdapter
        fun render(choice: Choice? = null) {
            stats.text = getString(R.string.lab6_stats, game.score, game.moves, game.matchedCount, game.count)
            feedback.text = getString(when {
                game.isFinished -> R.string.lab6_finished
                choice == Choice.MATCHED_RANK || choice == Choice.MATCHED_SUIT -> R.string.lab6_matched
                choice == Choice.MISMATCH -> R.string.lab6_mismatch
                else -> R.string.lab6_select
            })
            adapter.refresh()
        }
        adapter = CardAdapter(game) { index ->
            val result = game.chooseCardAtIndex(index)
            if (game.score > host.profile.bestScore) host.profile = host.profile.copy(bestScore = game.score)
            host.persist()
            render(result)
        }.apply { cardHeight = 76 }
        val recycler = RecyclerView(host).apply {
            layoutManager = GridLayoutManager(host, if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 8 else 4)
            this.adapter = adapter
            itemAnimator = null
        }
        root.addView(recycler, LinearLayout.LayoutParams(-1, 0, 1f))
        root.button(R.string.restart_game) { game.reset(); host.persist(); recycler.scrollToPosition(0); render() }
        render()
        return root
    }
}
class PairFragment : MatchFragment(2)
class TripleFragment : MatchFragment(3)
