package cn.turingzy.ledger.data

import cn.turingzy.ledger.data.repository.CardBackgroundDownloader
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CardBackgroundDownloaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun successfulDownloadWritesBytesAndClosesConnection() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val connection = connection(200)
        every { connection.contentLengthLong } returns bytes.size.toLong()
        every { connection.inputStream } returns ByteArrayInputStream(bytes)
        val destination = temporaryFolder.newFile()
        CardBackgroundDownloader { connection }.download("https://example.com/photo", destination)
        assertArrayEquals(bytes, destination.readBytes())
        verify(exactly = 1) { connection.disconnect() }
    }

    @Test
    fun relativeRedirectIsResolvedAndConnectionsAreClosed() = runTest {
        val redirect = connection(302)
        every { redirect.getHeaderField("Location") } returns "/final.png"
        val success = connection(200)
        every { success.inputStream } returns ByteArrayInputStream(byteArrayOf(1))
        val addresses = mutableListOf<URI>()
        val downloader = CardBackgroundDownloader { address ->
            addresses.add(address)
            if (addresses.size == 1) redirect else success
        }
        downloader.download("https://example.com/path/photo", temporaryFolder.newFile())
        assertEquals("https://example.com/final.png", addresses.last().toString())
        verify { redirect.disconnect(); success.disconnect() }
    }

    @Test
    fun redirectToHttpIsRejectedBeforeOpeningInsecureConnection() = runTest {
        val redirect = connection(302)
        every { redirect.getHeaderField("Location") } returns "http://example.com/image.png"
        var opened = 0
        val downloader = CardBackgroundDownloader { opened += 1; redirect }
        try {
            downloader.download("https://example.com/photo", temporaryFolder.newFile())
            fail("Expected insecure redirect rejection")
        } catch (_: IOException) {
            assertEquals(1, opened)
            verify { redirect.disconnect() }
        }
    }

    @Test
    fun oversizedImageIsRejectedWithoutReadingBody() = runTest {
        val connection = connection(200)
        every { connection.contentLengthLong } returns 21L * 1024 * 1024
        try {
            CardBackgroundDownloader { connection }.download("https://example.com/photo", temporaryFolder.newFile())
            fail("Expected size limit rejection")
        } catch (_: IOException) {
            verify(exactly = 0) { connection.inputStream }
            verify { connection.disconnect() }
        }
    }

    private fun connection(status: Int): HttpURLConnection = mockk<HttpURLConnection>(relaxed = true).also {
        every { it.responseCode } returns status
        every { it.contentLengthLong } returns -1
    }
}
