package com.songjunyi.cardgame

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import com.songjunyi.cardgame.model.CardMatchingGame
import com.songjunyi.cardgame.test.R as TestR
import java.io.File

/** Native Android instrumentation runner, executed by connectedDebugAndroidTest. */
class CardGameUiRunner : Instrumentation() {
    private val active = mutableListOf<Activity>()
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val cases = listOf(
            "defaultProfile" to ::defaultProfile,
            "menuOpensAboutDialog" to ::menuOpensAboutDialog,
            "saveAndCancelProfile" to ::saveAndCancelProfile,
            "invalidProfileInput" to ::invalidProfileInput,
            "board24AndRecycledState" to ::board24AndRecycledState,
            "firstTwoAndTurnBack" to ::firstTwoAndTurnBack,
            "tripleMatchResetAndBest" to ::tripleMatchResetAndBest,
            "mismatchFeedback" to ::mismatchFeedback,
            "rotationRestoresGame" to ::rotationRestoresGame,
            "landscapeNewRound36" to ::landscapeNewRound36
        )
        var failures = 0
        cases.forEachIndexed { index, (name, test) ->
            val status = Bundle().apply {
                putString("class", CardGameUiRunner::class.java.name)
                putString("test", name)
                putInt("numtests", cases.size)
                putInt("current", index + 1)
            }
            sendStatus(1, status)
            try {
                test()
                sendStatus(0, status)
            } catch (error: Throwable) {
                failures++
                status.putString("stack", android.util.Log.getStackTraceString(error))
                sendStatus(-2, status)
            } finally {
                runOnMainSync {
                    active.asReversed().forEach { if (!it.isFinishing && !it.isDestroyed) it.finish() }
                    active.clear()
                }
                waitForIdleSync()
            }
        }
        finish(if (failures == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
            putString("stream", if (failures == 0) "OK ("+cases.size+" tests)" else "FAILURES: "+failures)
        })
    }
    private fun verify(condition: Boolean, message: String) {
        if (!condition) throw AssertionError(message)
    }
    private fun main(): MainActivity {
        val result = startActivitySync(Intent(targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        active.add(result)
        waitForIdleSync()
        return result
    }
    private fun <T : Activity> open(from: Activity, id: Int, cls: Class<T>): T {
        val monitor = addMonitor(cls.name, null, false)
        tap(from, id)
        val result = waitForMonitorWithTimeout(monitor, 5000)
        removeMonitor(monitor)
        verify(result != null, "Activity not launched: "+cls.name)
        active.add(result)
        waitForIdleSync()
        return cls.cast(result)!!
    }
    private fun tap(activity: Activity, id: Int) {
        runOnMainSync {
            val view = activity.findViewById<android.view.View>(id)
            verify(view != null && view.isEnabled, "Button unavailable: "+id)
            view.requestRectangleOnScreen(android.graphics.Rect(0, 0, view.width, view.height), true)
            view.performClick()
        }
        waitForIdleSync()
    }
    private fun read(activity: Activity, id: Int): String {
        var value = ""
        runOnMainSync { value = activity.findViewById<TextView>(id).text.toString() }
        return value
    }
    private fun screenshot(name: String) {
        waitForIdleSync()
        Thread.sleep(1100)
        val bitmap = requireNotNull(uiAutomation.takeScreenshot())
        val root = File(targetContext.getExternalFilesDir(null), "ui-test-evidence").apply { mkdirs() }
        File(root, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun fixture(id: Int) = context.getString(id)
    private fun awaitCondition(message: String, condition: () -> Boolean) {
        val deadline = android.os.SystemClock.uptimeMillis() + 5000
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            waitForIdleSync()
            if (condition()) return
            Thread.sleep(50)
        }
        verify(condition(), message)
    }
    private fun defaultProfile() {
        val main = main()
        verify(read(main, R.id.player_name).contains(targetContext.getString(R.string.default_name)), "Default name")
        verify(read(main, R.id.player_student).contains(targetContext.getString(R.string.default_student)), "Student id")
        verify(read(main, R.id.player_gender).contains(targetContext.getString(R.string.unspecified)), "Gender unset")
        verify(read(main, R.id.player_best) == targetContext.getString(R.string.number_format, 0), "Best starts at zero")
        screenshot("01-default-main.png")
    }
    private fun menuOpensAboutDialog() {
        val main = main()
        runOnMainSync { main.openOptionsMenu() }
        screenshot("02-main-menu.png")
        val items = uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText(
            targetContext.getString(R.string.menu_about))
        verify(items.isNotEmpty(), "About overflow item visible")
        var clicked = false
        for (node in items) {
            var item: android.view.accessibility.AccessibilityNodeInfo? = node
            while (item != null && !item.isClickable) item = item.parent
            if (item != null) {
                clicked = item.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) break
            }
        }
        verify(clicked, "About overflow item clicked")
        waitForIdleSync()
        screenshot("03-about-dialog.png")
        val nodes = uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText(
            targetContext.getString(R.string.default_student))
        verify(nodes.isNotEmpty(), "About dialog shows student identity")
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
    }
    private fun saveAndCancelProfile() {
        val main = main()
        var editor = open(main, R.id.edit_profile, ProfileActivity::class.java)
        runOnMainSync {
            editor.findViewById<EditText>(R.id.edit_name).setText(fixture(TestR.string.fixture_name))
            editor.findViewById<EditText>(R.id.edit_phone).setText(fixture(TestR.string.fixture_phone))
            editor.findViewById<EditText>(R.id.edit_email).setText(fixture(TestR.string.fixture_email))
            editor.findViewById<RadioButton>(R.id.gender_male).isChecked = true
            editor.findViewById<CheckBox>(R.id.hobby_1).isChecked = true
            editor.findViewById<CheckBox>(R.id.hobby_2).isChecked = true
        }
        screenshot("04-profile-editor.png")
        tap(editor, R.id.save_profile)
        awaitCondition("Name updated after Activity result") {
            read(main, R.id.player_name).contains(fixture(TestR.string.fixture_name))
        }
        verify(read(main, R.id.player_phone).contains(fixture(TestR.string.fixture_phone)), "Phone updated")
        verify(read(main, R.id.player_email).contains(fixture(TestR.string.fixture_email)), "Email updated")
        screenshot("05-profile-saved.png")
        editor = open(main, R.id.edit_profile, ProfileActivity::class.java)
        runOnMainSync { editor.findViewById<EditText>(R.id.edit_name).setText(targetContext.getString(R.string.default_name)) }
        tap(editor, R.id.cancel_profile)
        verify(read(main, R.id.player_name).contains(fixture(TestR.string.fixture_name)), "Cancel preserves profile")
    }
    private fun invalidProfileInput() {
        val main = main()
        val editor = open(main, R.id.edit_profile, ProfileActivity::class.java)
        runOnMainSync { editor.findViewById<EditText>(R.id.edit_name).setText(fixture(TestR.string.fixture_empty)) }
        tap(editor, R.id.save_profile)
        var error: CharSequence? = null
        runOnMainSync { error = editor.findViewById<EditText>(R.id.edit_name).error }
        verify(error != null && !editor.isFinishing, "Blank name is rejected")
        runOnMainSync {
            editor.findViewById<EditText>(R.id.edit_name).setText(fixture(TestR.string.fixture_name))
            editor.findViewById<EditText>(R.id.edit_email).setText(fixture(TestR.string.fixture_bad_email))
        }
        tap(editor, R.id.save_profile)
        runOnMainSync { error = editor.findViewById<EditText>(R.id.edit_email).error }
        verify(error != null && !editor.isFinishing, "Invalid email is rejected")
        screenshot("15-validation.png")
        tap(editor, R.id.cancel_profile)
    }
    private fun game(): Pair<MainActivity, GameActivity> {
        val main = main()
        return main to open(main, R.id.open_game, GameActivity::class.java)
    }
    private fun model(game: GameActivity): CardMatchingGame {
        val field = GameActivity::class.java.getDeclaredField("game").apply { isAccessible = true }
        return field.get(game) as CardMatchingGame
    }
    private fun board(game: GameActivity) = game.findViewById<RecyclerView>(R.id.card_grid)
    private fun cardButton(game: GameActivity, position: Int): Button {
        runOnMainSync { board(game).scrollToPosition(position) }
        awaitCondition("RecyclerView holder bound: "+position) {
            var ready = false
            runOnMainSync { ready = board(game).findViewHolderForAdapterPosition(position) != null }
            ready
        }
        var result: Button? = null
        runOnMainSync { result = board(game).findViewHolderForAdapterPosition(position)!!.itemView as Button }
        return requireNotNull(result)
    }
    private fun flip(game: GameActivity, position: Int) {
        val button = cardButton(game, position)
        runOnMainSync { verify(button.isEnabled, "Card enabled"); button.performClick() }
        waitForIdleSync()
    }
    private fun cardText(game: GameActivity, position: Int): String {
        val button = cardButton(game, position)
        var result = ""
        runOnMainSync { result = button.text.toString() }
        return result
    }
    private fun top(game: GameActivity) {
        runOnMainSync { board(game).scrollToPosition(0) }
        waitForIdleSync()
    }
    private fun board24AndRecycledState() {
        val (_, game) = game()
        verify(board(game).adapter is CardAdapter, "Real RecyclerView adapter")
        verify(board(game).adapter!!.itemCount == 24, "Portrait has 24 cards")
        verify((board(game).layoutManager as GridLayoutManager).spanCount == 4, "Four columns")
        screenshot("06-board-24.png")
        flip(game, 0)
        val face = cardText(game, 0)
        verify(cardText(game, 23) == targetContext.getString(R.string.card_back), "Recycled last card stays hidden")
        verify(cardButton(game, 23).isEnabled, "Recycled hidden card enabled")
        screenshot("07-recycled-bottom.png")
        verify(cardText(game, 0) == face, "Rebound first card keeps face")
    }
    private fun firstTwoAndTurnBack() {
        val (_, game) = game()
        flip(game, 0)
        verify(read(game, R.id.game_score).toInt() == -1, "First flip costs 1")
        flip(game, 1)
        verify(read(game, R.id.game_score).toInt() == -2, "Two cards wait for third")
        verify(model(game).matchedCount == 0, "No premature pair match")
        screenshot("08-two-chosen.png")
        flip(game, 0)
        verify(read(game, R.id.game_score).toInt() == -2, "Turning back is free")
        verify(cardText(game, 0) == targetContext.getString(R.string.card_back), "Face hidden")
    }
    private fun tripleMatchResetAndBest() {
        val (main, game) = game()
        val before = model(game)
        // Among the first nine cards at least one suit occurs three times.
        val triple = (0 until 9).groupBy { before.cardAtIndex(it).suit }.values.first { it.size >= 3 }.take(3)
        triple.forEach { flip(game, it) }
        verify(read(game, R.id.game_score).toInt() == 9, "Suit triple net score 9")
        verify(model(game).matchedCount == 3, "Three cards matched")
        verify(!cardButton(game, triple[0]).isEnabled, "Matched card disabled")
        top(game)
        screenshot("09-triple-matched.png")
        tap(game, R.id.restart_game)
        verify(before === model(game), "Reset reuses same model for adapter")
        verify(read(game, R.id.game_score).toInt() == 0, "New round score reset")
        verify(read(game, R.id.game_matched) == targetContext.getString(R.string.matched_format, 0, 24), "New round unmatched")
        verify((0 until 24).all { cardText(game, it) == targetContext.getString(R.string.card_back) }, "All 24 cards rebound hidden")
        top(game)
        screenshot("10-card-reset.png")
        tap(game, R.id.exit_game)
        awaitCondition("Best appears on resumed main page") { read(main, R.id.player_best).toInt() == 9 }
        screenshot("11-main-best.png")
    }
    private fun mismatchFeedback() {
        val (_, game) = game()
        val model = model(game)
        val triple = (0 until model.count).flatMap { a ->
            (a+1 until model.count).flatMap { b -> (b+1 until model.count).map { c -> listOf(a,b,c) } }
        }.first { (a,b,c) -> model.cardAtIndex(a).match(arrayOf(model.cardAtIndex(b),model.cardAtIndex(c))) == 0 }
        triple.forEach { flip(game, it) }
        verify(read(game, R.id.game_score).toInt() == -5, "Mismatch net score -5")
        verify(cardText(game, triple[0]) == targetContext.getString(R.string.card_back), "First card hides")
        verify(cardText(game, triple[1]) == targetContext.getString(R.string.card_back), "Second card hides")
        verify(model.cardAtIndex(triple[2]).isChosen, "Newest card stays chosen")
        verify(read(game, R.id.game_feedback) == targetContext.getString(R.string.mismatch_prompt), "Feedback follows model")
        top(game)
        screenshot("12-card-mismatch.png")
    }
    private fun rotate(game: GameActivity, orientation: Int): GameActivity {
        val monitor = addMonitor(GameActivity::class.java.name, null, false)
        runOnMainSync { game.requestedOrientation = orientation }
        val recreated = waitForMonitorWithTimeout(monitor, 5000) as? GameActivity
        removeMonitor(monitor)
        verify(recreated != null, "Rotation recreates Activity")
        active.add(requireNotNull(recreated))
        waitForIdleSync()
        return recreated
    }
    private fun rotationRestoresGame() {
        val (_, original) = game()
        flip(original, 0); flip(original, 1)
        val before = model(original).snapshot()
        var game = rotate(original, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        verify(read(game, R.id.game_score).toInt() == -2, "Score survives landscape")
        verify(model(game).snapshot() == before, "Whole 24 card round survives rotation")
        verify((board(game).layoutManager as GridLayoutManager).spanCount == 9, "Landscape nine columns")
        screenshot("13-rotation-preserved.png")
        game = rotate(game, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        verify(model(game).snapshot() == before, "Portrait restores all state")
    }
    private fun landscapeNewRound36() {
        val (_, original) = game()
        var game = rotate(original, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        tap(game, R.id.restart_game)
        verify(board(game).adapter!!.itemCount == 36, "Landscape reset has 36 cards")
        verify(read(game, R.id.game_score).toInt() == 0, "Landscape reset score zero")
        cardButton(game, 35)
        top(game)
        screenshot("14-landscape-36.png")
        game = rotate(game, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        verify(board(game).adapter!!.itemCount == 36, "Rotate retains existing 36 card round")
        tap(game, R.id.restart_game)
        verify(board(game).adapter!!.itemCount == 24, "Portrait next round returns to 24")
    }
}
