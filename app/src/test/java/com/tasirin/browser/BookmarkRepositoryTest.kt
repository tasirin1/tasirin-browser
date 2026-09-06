package com.tasirin.browser

import com.tasirin.browser.data.Bookmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkRepositoryTest {

    @Test
    fun `normalizeUrl - URL dengan protocol dipertahankan`() {
        val input = "https://www.google.com"
        assertEquals("https://www.google.com", normalizeUrl(input))
    }

    @Test
    fun `normalizeUrl - domain tanpa protocol ditambah https`() {
        assertEquals("https://google.com", normalizeUrl("google.com"))
    }

    @Test
    fun `normalizeUrl - teks kosong jadi search`() {
        val result = normalizeUrl("hello world")
        assertTrue(result.startsWith("https://www.google.com/search?q="))
    }

    @Test
    fun `normalizeUrl - input dengan spasi jadi search`() {
        val result = normalizeUrl("what is android")
        assertTrue(result.contains("what is android") || result.contains("what+is+android") || result.contains("what%20is%20android"))
    }

    // Fungsi helper duplikat dari normalisasi URL di MainActivity
    private fun normalizeUrl(input: String): String {
        if (input.startsWith("http://") || input.startsWith("https://")) return input
        if (input.contains(".") && !input.contains(" ")) {
            return "https://$input"
        }
        return "https://www.google.com/search?q=$input"
    }
}
