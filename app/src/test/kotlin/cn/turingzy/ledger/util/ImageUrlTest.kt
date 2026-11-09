package cn.turingzy.ledger.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlTest {
    @Test
    fun acceptsHttpsImageUrlsWithQueryAndSurroundingWhitespace() {
        assertEquals("https://example.com/photo?id=123", parseImageUrl("  https://example.com/photo?id=123  ").toString())
        assertNotNull(parseImageUrl("HTTPS://example.com/image.png"))
    }

    @Test
    fun rejectsInsecureAndNonNetworkSchemes() {
        listOf("http://example.com/image.png", "file:///photo.png", "content://photo/1", "data:image/png;base64,abc").forEach {
            assertNull(parseImageUrl(it))
        }
    }

    @Test
    fun rejectsMissingHostAndMalformedInput() {
        listOf("", "example.com/photo.jpg", "https:///photo.jpg", "https://example.com/photo image.jpg", "https://[").forEach {
            assertNull(parseImageUrl(it))
        }
    }

    @Test
    fun rejectsCredentialsInUrl() {
        assertNull(parseImageUrl("https://user:password@example.com/image.png"))
    }
}
