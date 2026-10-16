package com.songjunyi.cardgame

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.*

class ProfileActivity : ScreenActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen(R.string.edit_title, R.string.edit_subtitle, true)
        val p = session.profile
        val form = panel()
        val name = field(form, R.string.name_label, R.id.edit_name, p.name)
        val student = field(form, R.string.student_label, R.id.edit_student, p.studentId)
            .apply { inputType = InputType.TYPE_CLASS_NUMBER }
        val phone = field(form, R.string.phone_label, R.id.edit_phone, p.phone)
            .apply { inputType = InputType.TYPE_CLASS_PHONE }
        val email = field(form, R.string.email_label, R.id.edit_email, p.email)
            .apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        label(form, R.string.gender_label, 12f, muted)
        val group = RadioGroup(this).apply {
            id = R.id.gender_group
            orientation = RadioGroup.HORIZONTAL
        }
        val genderIds = intArrayOf(R.id.gender_unknown, R.id.gender_male, R.id.gender_female)
        val genderLabels = intArrayOf(R.string.unspecified, R.string.male, R.string.female)
        for (i in genderIds.indices) group.addView(RadioButton(this).apply {
            id = genderIds[i]
            setText(genderLabels[i])
            setTextColor(white)
            textSize = 14f
        })
        form.addView(group)
        group.check(genderIds[p.gender])
        label(form, R.string.hobbies_label, 12f, muted)
        val hobbyLabels = intArrayOf(R.string.hobby_travel, R.string.hobby_reading,
            R.string.hobby_games, R.string.hobby_shopping)
        val hobbyIds = intArrayOf(R.id.hobby_0, R.id.hobby_1, R.id.hobby_2, R.id.hobby_3)
        val checks = hobbyIds.indices.map { i -> CheckBox(this).apply {
            id = hobbyIds[i]
            setText(hobbyLabels[i])
            setTextColor(white)
            isChecked = i in p.hobbies
        } }
        val hobbyRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        checks.forEach { hobbyRow.addView(it, LinearLayout.LayoutParams(0, -2, 1f)) }
        form.addView(hobbyRow)
        button(content, R.string.save_profile).apply {
            id = R.id.save_profile
            setOnClickListener {
                val n = name.text.toString().trim()
                val s = student.text.toString().trim()
                val ph = phone.text.toString().trim()
                val em = email.text.toString().trim()
                when {
                    n.isEmpty() -> { name.error = getString(R.string.name_error); name.requestFocus() }
                    s.isEmpty() || !s.all { it.isDigit() } ->
                        { student.error = getString(R.string.student_error); student.requestFocus() }
                    ph.isNotEmpty() && !Patterns.PHONE.matcher(ph).matches() ->
                        { phone.error = getString(R.string.phone_error); phone.requestFocus() }
                    em.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(em).matches() ->
                        { email.error = getString(R.string.email_error); email.requestFocus() }
                    else -> {
                        setResult(RESULT_OK, Intent().putExtra("name", n).putExtra("student", s)
                            .putExtra("phone", ph).putExtra("email", em)
                            .putExtra("gender", genderIds.indexOf(group.checkedRadioButtonId))
                            .putIntegerArrayListExtra("hobbies", ArrayList(checks.indices.filter { checks[it].isChecked })))
                        finish()
                    }
                }
            }
        }
        button(content, R.string.cancel_profile, false).apply {
            id = R.id.cancel_profile
            setOnClickListener { setResult(RESULT_CANCELED); finish() }
        }
        label(content, R.string.profile_note, 12f, muted)
    }
}
