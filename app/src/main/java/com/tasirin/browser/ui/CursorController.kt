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
 * Pemicu panah (D-pad / tombol on-screen) menggerakkan pointer;
 * tombol tengah / tap pada tombol aksi melakukan klik di posisi pointer.
 *
 * Overlay ditempel langsung ke parent WebView (bukan WindowManager) supaya
 * tidak butuh permission SYSTEM_ALERT_WINDOW dan tidak crash di versi Android
 * mana pun (TYPE_APPLICATION_OVERLAY hanya tersedia API 26+).
 */
class CursorController(
    private val context: Context,
    private val webView: android.webkit.WebView
) {
    private var isActive = false
    private var cursorX = 0f
    private var cursorY = 0f
    private val step = 40f
    private val longStep = 160f

    // Overlay cursor
    private var cursorView: ImageView? = null
    // On-screen D-pad overlay
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

    @SuppressLint("ClickableViewAccessibility")
    private fun showCursor() {
        val iv = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_mylocation)
            setColorFilter(Color.parseColor("#FF4081"))
            alpha = 0.9f
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        val size = (48 * context.resources.displayMetrics.density).toInt()
        addCursorToView(iv, size)
    }

    private fun addCursorToView(iv: ImageView, size: Int) {
        val parent = webView.parent as? ViewGroup ?: return
        val params = FrameLayout.LayoutParams(size, size).apply {
            leftMargin = cursorX.toInt()
            topMargin = cursorY.toInt()
        }
        iv.layoutParams = params
        parent.addView(iv)
        cursorView = iv
    }

    private fun hideCursor() {
        cursorView?.let { v ->
            (v.parent as? ViewGroup)?.removeView(v)
        }
        cursorView = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showDpad() {
        val density = context.resources.displayMetrics.density
        val btnSize = (44 * density).toInt()
        val pad = (4 * density).toInt()

        fun makeBtn(label: String, onClick: () -> Unit): View {
            val btn = android.widget.TextView(context).apply {
                text = label
                textSize = 18f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#66000000"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                    marginStart = pad; marginEnd = pad
                    topMargin = pad; bottomMargin = pad
                }
                setOnClickListener { onClick() }
            }
            return btn
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Color.parseColor("#33000000"))
        }

        val rowUp = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        rowUp.addView(makeBtn("▲") { move(0f, -step) })
        container.addView(rowUp)

        val rowMid = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        rowMid.addView(makeBtn("◀") { move(-step, 0f) })
        rowMid.addView(makeBtn("●") { clickAtCursor() })
        rowMid.addView(makeBtn("▶") { move(step, 0f) })
        container.addView(rowMid)

        val rowDown = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        rowDown.addView(makeBtn("▼") { move(0f, step) })
        container.addView(rowDown)

        val size = (180 * density).toInt()
        addDpadToView(container)
    }

    private fun addDpadToView(container: View) {
        val parent = webView.parent as? ViewGroup ?: return
        val density = context.resources.displayMetrics.density
        val params = FrameLayout.LayoutParams(
            (180 * density).toInt(),
            (180 * density).toInt(),
            Gravity.BOTTOM or Gravity.END
        ).apply {
            marginEnd = (16 * density).toInt()
            bottomMargin = (100 * density).toInt()
        }
        container.layoutParams = params
        parent.addView(container)
        dpadView = container
    }

    private fun hideDpad() {
        dpadView?.let { v ->
            (v.parent as? ViewGroup)?.removeView(v)
        }
        dpadView = null
    }

    private fun overlaySize(): Pair<Float, Float> {
        val w = webView.width
        val h = webView.height
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
            val p = v.layoutParams as? FrameLayout.LayoutParams
            if (p != null) {
                p.leftMargin = cursorX.toInt()
                p.topMargin = cursorY.toInt()
                v.layoutParams = p
            }
        }
    }

    private fun clickAtCursor() {
        val webX = cursorX
        val webY = cursorY

        val downTime = System.currentTimeMillis()
        val event = MotionEvent.obtain(
            downTime, downTime,
            MotionEvent.ACTION_DOWN, webX, webY, 0
        )
        webView.dispatchTouchEvent(event)
        event.recycle()

        val upEvent = MotionEvent.obtain(
            downTime, System.currentTimeMillis() + 50,
            MotionEvent.ACTION_UP, webX, webY, 0
        )
        webView.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }
}
