package com.songjunyi.cardgame

import android.content.Intent
import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView

@Suppress("DEPRECATION")
class MainActivity : ScreenActivity() {
    private val fields = mutableMapOf<Int, TextView>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen(R.string.main_title, R.string.main_subtitle, menu = true)
        val info = panel()
        label(info, R.string.profile_title, 18f, white, true)
        for (id in listOf(R.id.player_name, R.id.player_gender, R.id.player_student,
            R.id.player_phone, R.id.player_email, R.id.player_hobbies)) {
            fields[id] = text(info, getString(R.string.unfilled)).apply { this.id = id }
        }
        val score = panel()
        label(score, R.string.best_title, 13f, accent)
        fields[R.id.player_best] = text(score, getString(R.string.number_format, 0), 38f, white, true)
            .apply { id = R.id.player_best }
        button(content, R.string.open_game).apply {
            id = R.id.open_game
            setOnClickListener { startActivity(Intent(this@MainActivity, GameActivity::class.java)) }
        }
        button(content, R.string.edit_profile, false).apply {
            id = R.id.edit_profile
            setOnClickListener {
                startActivityForResult(Intent(this@MainActivity, ProfileActivity::class.java), PROFILE_REQUEST)
            }
        }
        label(content, R.string.profile_note, 12f, muted)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.menu_about) return super.onOptionsItemSelected(item)
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage(getString(R.string.about_description, getString(R.string.default_name),
                getString(R.string.default_student), getString(R.string.app_version)) + getString(R.string.rules_description))
            .setPositiveButton(R.string.dialog_ok, null)
            .show()
        return true
    }
    override fun onResume() { super.onResume(); refreshProfile() }
    private fun refreshProfile() {
        val p = session.profile
        val genders = arrayOf(R.string.unspecified, R.string.male, R.string.female)
        val hobbies = intArrayOf(R.string.hobby_travel, R.string.hobby_reading,
            R.string.hobby_games, R.string.hobby_shopping)
        fun row(id: Int, label: Int, value: String) {
            fields[id]?.text = getString(R.string.field_format, getString(label), value)
        }
        row(R.id.player_name, R.string.name_label, p.name)
        row(R.id.player_gender, R.string.gender_label, getString(genders[p.gender]))
        row(R.id.player_student, R.string.student_label, p.studentId)
        row(R.id.player_phone, R.string.phone_label, p.phone.ifBlank { getString(R.string.unfilled) })
        row(R.id.player_email, R.string.email_label, p.email.ifBlank { getString(R.string.unfilled) })
        row(R.id.player_hobbies, R.string.hobbies_label, p.hobbies.joinToString(getString(R.string.hobby_separator)) {
            getString(hobbies[it])
        }.ifBlank { getString(R.string.no_hobbies) })
        fields[R.id.player_best]?.text = getString(R.string.number_format, p.bestScore)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PROFILE_REQUEST && resultCode == RESULT_OK && data != null) {
            session.updateProfile(session.profile.copy(
                name = requireNotNull(data.getStringExtra("name")),
                studentId = requireNotNull(data.getStringExtra("student")),
                gender = data.getIntExtra("gender", 0),
                phone = data.getStringExtra("phone").orEmpty(),
                email = data.getStringExtra("email").orEmpty(),
                hobbies = data.getIntegerArrayListExtra("hobbies")?.toList() ?: emptyList()))
            refreshProfile()
        }
    }
    companion object { private const val PROFILE_REQUEST = 404 }
}
