package com.tasirin.browser.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import kotlin.math.max
import kotlin.math.min

/**
 * Controller cursor on-screen untuk mode navigasi tanpa sentuh.
 * Pemicu panah (D-pad / tombol on-screen) menggerakkan pointer;
 * tombol tengah / tap pada tombol aksi melakukan klik di posisi pointer.
 */
class CursorController(
    private val context: Context,
    private val webView: android.webkit.WebView
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

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
        // Posisikan di tengah layar
        cursorX = webView.width / 2f
        cursorY = webView.height / 2f
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
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = cursorX.toInt()
            y = cursorY.toInt()
        }
        try {
            wm.addView(iv, params)
        } catch (_: SecurityException) {
            // Izin overlay belum diberikan — gunakan cara alternatif
            addCursorToView(iv)
        }
        cursorView = iv
    }

    private fun addCursorToView(iv: ImageView) {
        val params = FrameLayout.LayoutParams(
            (32 * context.resources.displayMetrics.density).toInt(),
            (32 * context.resources.displayMetrics.density).toInt()
        ).apply {
            leftMargin = cursorX.toInt()
            topMargin = cursorY.toInt()
        }
        iv.layoutParams = params
        (webView.parent as? ViewGroup)?.addView(iv)
        cursorView = iv
    }

    private fun hideCursor() {
        cursorView?.let { v ->
            try { wm.removeView(v) } catch (_: Exception) {
                (v.parent as? ViewGroup)?.removeView(v)
            }
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
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = (16 * density).toInt()
            y = (100 * density).toInt()
        }
        try {
            wm.addView(container, params)
        } catch (_: SecurityException) {
            addDpadToView(container)
        }
        dpadView = container
    }

    private fun addDpadToView(container: View) {
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
        (webView.parent as? ViewGroup)?.addView(container)
        dpadView = container
    }

    private fun hideDpad() {
        dpadView?.let { v ->
            try { wm.removeView(v) } catch (_: Exception) {
                (v.parent as? ViewGroup)?.removeView(v)
            }
        }
        dpadView = null
    }

    private fun move(dx: Float, dy: Float) {
        val maxX = webView.width.toFloat()
        val maxY = webView.height.toFloat()
        cursorX = max(0f, min(maxX, cursorX + dx))
        cursorY = max(0f, min(maxY, cursorY + dy))
        updateCursorPosition()
    }

    private fun updateCursorPosition() {
        cursorView?.let { v ->
            if (v.parent is WindowManager) {
                val p = v.layoutParams as? WindowManager.LayoutParams
                if (p != null) {
                    p.x = cursorX.toInt()
                    p.y = cursorY.toInt()
                    wm.updateViewLayout(v, p)
                }
            } else {
                val p = v.layoutParams as? FrameLayout.LayoutParams
                if (p != null) {
                    p.leftMargin = cursorX.toInt()
                    p.topMargin = cursorY.toInt()
                    v.layoutParams = p
                }
            }
        }
    }

    private fun clickAtCursor() {
        // Konversi koordinat cursor ke posisi WebView
        val result = FloatArray(2)
        webView.getLocationOnScreen(result)
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
