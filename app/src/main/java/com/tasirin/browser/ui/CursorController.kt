package com.tasirin.browser.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import kotlin.math.max
import kotlin.math.min

/**
 * Controller cursor on-screen untuk mode navigasi tanpa sentuh.
 * Mengontrol SELURUH layar (toolbar + WebView + bookmarks).
 *
 * Cursor & D-pad ditempel ke rootContainer (android.R.id.content) supaya
 * tidak butuh permission SYSTEM_ALERT_WINDOW.
 *
 * Alur klik: cursor posisi → rootContainer.dispatchTouchEvent → propagate
 * ke view apapun di posisi tersebut (toolbar buttons, WebView, bookmark list, dll).
 */
class CursorController(
    private val context: Context,
    private val rootContainer: ViewGroup
) {
    private var isActive = false
    private var cursorX = 0f
    private var cursorY = 0f
    private val step = 40f
    private val longStep = 160f

    private var cursorView: ImageView? = null
    private var dpadView: View? = null

    val isCursorMode: Boolean get() = isActive

    fun toggle(): Boolean {
        if (isActive) disable() else enable()
        return isActive
    }

    fun enable() {
        if (isActive) return
        isActive = true
        val (maxX, maxY) = overlaySize()
        cursorX = max(0f, min(maxX, maxX / 2f))
        cursorY = max(0f, min(maxY, maxY / 2f))
        showCursor()
        showDpad()
    }

    fun disable() {
        if (!isActive) return
        isActive = false
        hideCursor()
        hideDpad()
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isActive) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { move(0f, -step); return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { move(0f, step); return true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { move(-step, 0f); return true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { move(step, 0f); return true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { clickAtCursor(); return true }
            KeyEvent.KEYCODE_PAGE_UP -> { move(0f, -longStep); return true }
            KeyEvent.KEYCODE_PAGE_DOWN -> { move(0f, longStep); return true }
        }
        return false
    }

    /* ---------- cursor overlay ---------- */

    @SuppressLint("ClickableViewAccessibility")
    private fun showCursor() {
        val density = context.resources.displayMetrics.density
        val iv = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_mylocation)
            setColorFilter(Color.parseColor("#FF4081"))
            alpha = 0.9f
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            isClickable = false
            isFocusable = false
        }
        val size = (48 * density).toInt()
        val params = FrameLayout.LayoutParams(size, size).apply {
            leftMargin = cursorX.toInt()
            topMargin = cursorY.toInt()
        }
        iv.layoutParams = params
        iv.elevation = 10f * density
        rootContainer.addView(iv)
        cursorView = iv
    }

    private fun hideCursor() {
        cursorView?.let { v ->
            (v.parent as? ViewGroup)?.removeView(v)
        }
        cursorView = null
    }

    /* ---------- D-pad overlay ---------- */

    @SuppressLint("ClickableViewAccessibility")
    private fun showDpad() {
        val density = context.resources.displayMetrics.density
        val btnSize = (44 * density).toInt()
        val pad = (4 * density).toInt()

        fun makeBtn(label: String, onClick: () -> Unit): View {
            return android.widget.TextView(context).apply {
                text = label
                textSize = 18f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#66000000"))
                gravity = Gravity.CENTER
                isClickable = true
                layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                    marginStart = pad; marginEnd = pad
                    topMargin = pad; bottomMargin = pad
                }
                setOnClickListener { onClick() }
            }
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Color.parseColor("#33000000"))
            setOnTouchListener { _, _ -> true } // tutup touch biar tidak tembus ke toolbar/WebView
        }

        container.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(makeBtn("▲") { move(0f, -step) })
        })
        container.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(makeBtn("◀") { move(-step, 0f) })
            addView(makeBtn("●") { clickAtCursor() })
            addView(makeBtn("▶") { move(step, 0f) })
        })
        container.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(makeBtn("▼") { move(0f, step) })
        })

        val size = (180 * density).toInt()
        val params = FrameLayout.LayoutParams(size, size).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            marginEnd = (16 * density).toInt()
            bottomMargin = (100 * density).toInt()
        }
        container.layoutParams = params
        container.elevation = 10f * density
        rootContainer.addView(container)
        dpadView = container
    }

    private fun hideDpad() {
        dpadView?.let { v ->
            (v.parent as? ViewGroup)?.removeView(v)
        }
        dpadView = null
    }

    /* ---------- helper ---------- */

    private fun overlaySize(): Pair<Float, Float> {
        val w = rootContainer.width
        val h = rootContainer.height
        if (w > 0 && h > 0) return w.toFloat() to h.toFloat()
        val dm = context.resources.displayMetrics
        return dm.widthPixels.toFloat() to dm.heightPixels.toFloat()
    }

    private fun move(dx: Float, dy: Float) {
        val (maxX, maxY) = overlaySize()
        cursorX = max(0f, min(maxX, cursorX + dx))
        cursorY = max(0f, min(maxY, cursorY + dy))
        updateCursorPosition()
    }

    private fun updateCursorPosition() {
        cursorView?.let { v ->
            val p = v.layoutParams as? FrameLayout.LayoutParams ?: return
            p.leftMargin = cursorX.toInt()
            p.topMargin = cursorY.toInt()
            v.layoutParams = p
        }
    }

    private fun clickAtCursor() {
        val downTime = System.currentTimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
        rootContainer.dispatchTouchEvent(down)
        down.recycle()

        val up = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, cursorX, cursorY, 0)
        rootContainer.dispatchTouchEvent(up)
        up.recycle()
    }
}
