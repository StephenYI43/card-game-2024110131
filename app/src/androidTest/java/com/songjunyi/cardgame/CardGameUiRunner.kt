package com.songjunyi.cardgame

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.*
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
            "saveAndCancelProfile" to ::saveAndCancelProfile,
            "invalidProfileInput" to ::invalidProfileInput,
            "firstFlipAndTurnBack" to ::firstFlipAndTurnBack,
            "rankMatchRestartAndBest" to ::rankMatchRestartAndBest,
            "mismatchFeedback" to ::mismatchFeedback,
            "rotationRestoresGame" to ::rotationRestoresGame
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
        screenshot("02-profile-editor.png")
        tap(editor, R.id.save_profile)
        awaitCondition("Name updated after Activity result") {
            read(main, R.id.player_name).contains(fixture(TestR.string.fixture_name))
        }
        verify(read(main, R.id.player_phone).contains(fixture(TestR.string.fixture_phone)), "Phone updated")
        verify(read(main, R.id.player_email).contains(fixture(TestR.string.fixture_email)), "Email updated")
        screenshot("03-profile-saved.png")
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
        screenshot("10-validation.png")
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
    private val ids = intArrayOf(R.id.card_0, R.id.card_1, R.id.card_2, R.id.card_3,
        R.id.card_4, R.id.card_5, R.id.card_6, R.id.card_7, R.id.card_8, R.id.card_9,
        R.id.card_10, R.id.card_11, R.id.card_12, R.id.card_13, R.id.card_14, R.id.card_15)
    private fun firstFlipAndTurnBack() {
        val (_, game) = game()
        tap(game, ids[0])
        verify(read(game, R.id.game_score).toInt() == -1, "First flip costs 1")
        verify(read(game, ids[0]) != targetContext.getString(R.string.card_back), "Face displayed")
        screenshot("04-card-first.png")
        tap(game, ids[0])
        verify(read(game, R.id.game_score).toInt() == -1, "Turning back is free")
        verify(read(game, ids[0]) == targetContext.getString(R.string.card_back), "Face hidden")
    }
    private fun rankMatchRestartAndBest() {
        val (main, game) = game()
        val model = model(game)
        val pair = (0 until model.count).flatMap { a -> (a+1 until model.count).map { b -> a to b } }
            .first { (a,b) -> model.cardAtIndex(a).rank == model.cardAtIndex(b).rank }
        tap(game, ids[pair.first]); tap(game, ids[pair.second])
        verify(read(game, R.id.game_score).toInt() == 14, "Rank match net score 14")
        verify(!game.findViewById<Button>(ids[pair.first]).isEnabled, "Matched card disabled")
        screenshot("05-card-matched.png")
        tap(game, R.id.restart_game)
        verify(read(game, R.id.game_score).toInt() == 0, "New round score reset")
        verify(read(game, R.id.game_matched) == targetContext.getString(R.string.matched_format, 0, 16), "New round unmatched")
        verify((0 until 16).all { read(game, ids[it]) == targetContext.getString(R.string.card_back) }, "All cards hidden")
        screenshot("06-card-restart.png")
        tap(game, R.id.exit_game)
        awaitCondition("Best appears on resumed main page") { read(main, R.id.player_best).toInt() == 14 }
        screenshot("09-main-best.png")
    }
    private fun mismatchFeedback() {
        val (_, game) = game()
        val model = model(game)
        val pair = (0 until model.count).flatMap { a -> (a+1 until model.count).map { b -> a to b } }
            .first { (a,b) -> model.cardAtIndex(a).match(arrayOf(model.cardAtIndex(b))) == 0 }
        tap(game, ids[pair.first]); tap(game, ids[pair.second])
        verify(read(game, R.id.game_score).toInt() == -4, "Mismatch net score -4")
        verify(read(game, ids[pair.first]) == targetContext.getString(R.string.card_back), "Previous card hides")
        verify(read(game, R.id.game_feedback) == targetContext.getString(R.string.mismatch_prompt), "Feedback follows model")
        screenshot("07-card-mismatch.png")
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
        tap(original, ids[0])
        val face = read(original, ids[0])
        var game = rotate(original, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        verify(read(game, R.id.game_score).toInt() == -1, "Score survives landscape")
        verify(read(game, ids[0]) == face, "Card state survives landscape")
        runOnMainSync {
            val grid = game.findViewById<android.view.View>(R.id.card_grid)
            grid.requestRectangleOnScreen(android.graphics.Rect(0, 0, grid.width, grid.height), true)
        }
        screenshot("08-game-landscape.png")
        game = rotate(game, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        verify(read(game, R.id.game_score).toInt() == -1 && read(game, ids[0]) == face, "Portrait restores state")
    }
}
