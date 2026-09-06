package com.tasirin.browser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BookmarkRepository {

    private const val PREFS_NAME = "bookmarks"
    private const val KEY_BOOKMARKS = "bookmarks"

    fun getAll(context: Context): List<Bookmark> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BOOKMARKS, null) ?: return defaultBookmarks()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Bookmark(
                    title = o.optString("title", ""),
                    url = o.optString("url", ""),
                    addedAt = o.optLong("addedAt", 0)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, bookmarks: List<Bookmark>) {
        val arr = JSONArray()
        bookmarks.forEach { b ->
            arr.put(JSONObject().apply {
                put("title", b.title)
                put("url", b.url)
                put("addedAt", b.addedAt)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
    }

    fun add(context: Context, title: String, url: String) {
        val list = getAll(context).toMutableList()
        if (list.any { it.url == url }) return
        list.add(Bookmark(title, url, System.currentTimeMillis()))
        save(context, list)
    }

    fun remove(context: Context, url: String) {
        val list = getAll(context).toMutableList()
        list.removeAll { it.url == url }
        save(context, list)
    }

    private fun defaultBookmarks() = listOf(
        Bookmark("Google", "https://www.google.com", 0),
        Bookmark("YouTube", "https://m.youtube.com", 0),
        Bookmark("Wikipedia", "https://m.wikipedia.org", 0),
        Bookmark("GitHub", "https://github.com", 0),
    )
}
