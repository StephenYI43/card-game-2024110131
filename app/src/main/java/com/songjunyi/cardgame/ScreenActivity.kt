package com.songjunyi.cardgame

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.widget.*

/** Shared View construction; controllers still own navigation and model updates. */
abstract class ScreenActivity : Activity() {
    protected val accent = Color.rgb(165, 180, 252)
    protected val white = Color.rgb(243, 247, 255)
    protected val muted = Color.rgb(154, 174, 203)
    protected lateinit var content: LinearLayout
    protected val session get() = (application as GameApplication).session

    protected fun screen(title: Int, subtitle: Int, back: Boolean = false, menu: Boolean = false) {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(9, 15, 31), Color.rgb(22, 30, 56), Color.rgb(10, 20, 35)))
        }
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(22))
        }
        outer.addView(content, LinearLayout.LayoutParams(dp(minOf(resources.configuration.screenWidthDp, 720)), -2))
        scroll.addView(outer)
        setContentView(scroll)
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        if (menu) {
            val toolbar = Toolbar(this).apply {
                setTitleTextColor(white)
                popupTheme = android.R.style.Theme_Material
            }
            content.addView(toolbar, LinearLayout.LayoutParams(-1, dp(48)))
            setActionBar(toolbar)
            actionBar?.setTitle(R.string.app_name)
        }
        if (back) button(content, R.string.back, false).setOnClickListener { finish() }
        label(content, R.string.eyebrow, 11f, accent, true).letterSpacing = .13f
        label(content, R.string.app_name, 12f, muted)
        label(content, title, 28f, white, true)
        label(content, subtitle, 13f, muted)
    }
    protected fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(15), dp(15), dp(15))
        background = shape(Color.rgb(18, 31, 54), 22f, Color.rgb(43, 60, 88))
        content.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(15) })
    }
    protected fun label(parent: LinearLayout, resource: Int, size: Float = 15f,
                        color: Int = white, bold: Boolean = false) =
        text(parent, getString(resource), size, color, bold)
    protected fun text(parent: LinearLayout, value: CharSequence, size: Float = 15f,
                       color: Int = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setLineSpacing(dp(2).toFloat(), 1f)
            parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        }
    protected fun button(parent: LinearLayout, resource: Int, primary: Boolean = true): Button =
        Button(this).apply {
            setText(resource)
            isAllCaps = false
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            minHeight = dp(48)
            setTextColor(if (primary) Color.rgb(12, 20, 45) else accent)
            background = shape(if (primary) accent else Color.rgb(29, 45, 70), 14f)
            backgroundTintList = null
            parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(11) })
        }
    protected fun field(parent: LinearLayout, title: Int, idValue: Int, value: String): EditText {
        label(parent, title, 12f, muted)
        return EditText(this).apply {
            id = idValue
            setText(value)
            setHint(title)
            setTextColor(white)
            setHintTextColor(muted)
            textSize = 15f
            isSingleLine = true
            setPadding(dp(13), dp(10), dp(13), dp(10))
            background = shape(Color.rgb(9, 20, 37), 12f, Color.rgb(55, 76, 108))
            parent.addView(this, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(7) })
        }
    }
    protected fun shape(color: Int, radius: Float, border: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius.toInt()).toFloat()
            if (border != null) setStroke(dp(1), border)
        }
    protected fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
