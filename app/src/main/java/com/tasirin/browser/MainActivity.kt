package com.tasirin.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import android.webkit.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tasirin.browser.data.BookmarkRepository
import com.tasirin.browser.ui.BookmarkAdapter
import com.tasirin.browser.ui.CursorController

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var bookmarksPage: LinearLayout
    private lateinit var bookmarkList: RecyclerView
    private lateinit var textNoBookmarks: TextView
    private lateinit var btnBack: TextView
    private lateinit var btnForward: TextView
    private lateinit var btnCursorMode: TextView
    private lateinit var btnBookmark: TextView
    private lateinit var btnMenu: TextView
    private lateinit var cursorController: CursorController
    private lateinit var bookmarkAdapter: BookmarkAdapter

    private var bookmarksVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        urlInput = findViewById(R.id.urlInput)
        progressBar = findViewById(R.id.progressBar)
        bookmarksPage = findViewById(R.id.bookmarksPage)
        bookmarkList = findViewById(R.id.bookmarkList)
        textNoBookmarks = findViewById(R.id.textNoBookmarks)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnCursorMode = findViewById(R.id.btnCursorMode)
        btnBookmark = findViewById(R.id.btnBookmark)
        btnMenu = findViewById(R.id.btnMenu)

        setupWebView()
        setupBookmarks()
        setupAddressBar()
        setupButtons()
        setupBackDispatcher()
        cursorController = CursorController(this, webView)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null) {
            loadUrl(data.toString())
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = true
            defaultTextEncodingName = "UTF-8"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                // Tangkap intent://deep links
                if (url.startsWith("intent://")) {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        if (intent != null) {
                            startActivity(intent)
                            return true
                        }
                    } catch (_: Exception) {}
                    return true
                }
                urlInput.setText(url)
                return false
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
                urlInput.setText(url ?: "")
            }

            override fun onPageFinished(view: WebView, url: String?) {
                progressBar.visibility = View.GONE
                urlInput.setText(url ?: view.url ?: "")
                updateNavigationButtons()
                updateBookmarkStar()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
            }
        }
    }

    private fun setupBookmarks() {
        bookmarkAdapter = BookmarkAdapter(
            mutableListOf(),
            onClick = { bm -> loadUrl(bm.url) },
            onRemove = { bm ->
                AlertDialog.Builder(this)
                    .setTitle("Remove bookmark?")
                    .setMessage(bm.title)
                    .setPositiveButton("Remove") { _, _ ->
                        BookmarkRepository.remove(this, bm.url)
                        refreshBookmarks()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        bookmarkList.layoutManager = LinearLayoutManager(this)
        bookmarkList.adapter = bookmarkAdapter
        refreshBookmarks()
    }

    private fun setupAddressBar() {
        urlInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                loadFromInput()
                true
            } else false
        }
    }

    private fun setupButtons() {
        btnBack.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
            else if (!bookmarksVisible) showBookmarks()
        }
        btnForward.setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        btnCursorMode.setOnClickListener {
            val isOn = cursorController.toggle()
            btnCursorMode.text = if (isOn) getString(R.string.cursor_mode_on)
                else getString(R.string.cursor_mode_off)
            btnCursorMode.setBackgroundColor(
                if (isOn) 0xFFFF5722.toInt() else 0xFF1565C0.toInt()
            )
        }
        btnBookmark.setOnClickListener {
            val url = webView.url ?: return@setOnClickListener
            val title = webView.title ?: url
            val existing = BookmarkRepository.getAll(this).any { it.url == url }
            if (existing) {
                BookmarkRepository.remove(this, url)
                Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
            } else {
                BookmarkRepository.add(this, title, url)
                Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show()
            }
            updateBookmarkStar()
        }
        btnMenu.setOnClickListener { showMenu() }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Cursor mode: handle panah (bukan BACK — BACK ditangani dispatcher)
        if (keyCode != KeyEvent.KEYCODE_BACK && event != null) {
            if (cursorController.dispatchKeyEvent(event)) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setupBackDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (cursorController.isCursorMode) {
                    cursorController.disable()
                    btnCursorMode.text = getString(R.string.cursor_mode_off)
                    btnCursorMode.setBackgroundColor(0xFF1565C0.toInt())
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else if (!bookmarksVisible) {
                    showBookmarks()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun loadUrl(url: String) {
        val normalized = normalizeUrl(url)
        webView.loadUrl(normalized)
        showWebView()
    }

    private fun loadFromInput() {
        val input = urlInput.text.toString().trim()
        if (input.isEmpty()) return
        loadUrl(input)
    }

    private fun normalizeUrl(input: String): String {
        if (input.startsWith("http://") || input.startsWith("https://")) return input
        if (input.contains(".") && !input.contains(" ")) {
            return "https://$input"
        }
        return "https://www.google.com/search?q=${Uri.encode(input)}"
    }

    private fun showWebView() {
        bookmarksVisible = false
        bookmarksPage.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }

    private fun showBookmarks() {
        webView.loadUrl("about:blank")
        bookmarksVisible = true
        bookmarksPage.visibility = View.VISIBLE
        webView.visibility = View.GONE
        refreshBookmarks()
    }

    private fun refreshBookmarks() {
        val list = BookmarkRepository.getAll(this)
        bookmarkAdapter.update(list)
        textNoBookmarks.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateNavigationButtons() {
        btnBack.alpha = if (webView.canGoBack()) 1f else 0.3f
        btnForward.alpha = if (webView.canGoForward()) 1f else 0.3f
    }

    private fun updateBookmarkStar() {
        val url = webView.url
        val isBookmarked = url != null && BookmarkRepository.getAll(this).any { it.url == url }
        btnBookmark.text = if (isBookmarked) "★" else "☆"
        btnBookmark.setTextColor(if (isBookmarked) 0xFFFFC107.toInt() else 0xFF999999.toInt())
    }

    private fun showMenu() {
        val items = arrayOf(
            "Share",
            "Add bookmark",
            "Open in Chrome",
            "Refresh",
            "Clear history & cache",
            "Exit"
        )
        AlertDialog.Builder(this)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> shareCurrentPage()
                    1 -> showAddBookmarkDialog()
                    2 -> openInExternalBrowser()
                    3 -> webView.reload()
                    4 -> { webView.clearCache(true); webView.clearHistory(); Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show() }
                    5 -> finish()
                }
            }
            .show()
    }

    private fun shareCurrentPage() {
        val url = webView.url ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, "Share page"))
    }

    private fun openInExternalBrowser() {
        val url = webView.url ?: return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    @SuppressLint("InflateParams")
    private fun showAddBookmarkDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_bookmark, null)
        val titleInput = view.findViewById<EditText>(R.id.inputBookmarkTitle)
        val urlInput2 = view.findViewById<EditText>(R.id.inputBookmarkUrl)
        titleInput.setText(webView.title ?: "")
        urlInput2.setText(webView.url ?: "")

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_add_bookmark))
            .setView(view)
            .setPositiveButton(getString(R.string.dialog_add)) { _, _ ->
                val t = titleInput.text.toString().trim()
                val u = urlInput2.text.toString().trim()
                if (u.isNotEmpty()) {
                    BookmarkRepository.add(this, t.ifEmpty { u }, u)
                    refreshBookmarks()
                    Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }
}
