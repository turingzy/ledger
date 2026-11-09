package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.util.parseImageUrl
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class CardBackgroundDownloader(
    private val openConnection: (URI) -> HttpURLConnection = { it.toURL().openConnection() as HttpURLConnection },
) {
    suspend fun download(address: String, destination: File) {
        var url = requireNotNull(parseImageUrl(address)) { "Invalid HTTPS image URL" }
        repeat(6) {
            currentCoroutineContext().ensureActive()
            val connection = openConnection(url)
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "image/*")
            try {
                val status = connection.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    val location = connection.getHeaderField("Location") ?: throw IOException("Missing image redirect")
                    url = parseImageUrl(url.resolve(location).toString()) ?: throw IOException("Invalid HTTPS image redirect")
                    return@repeat
                }
                if (status != HttpURLConnection.HTTP_OK) throw IOException("Image server returned HTTP $status")
                if (connection.contentLengthLong > MAX_IMAGE_BYTES) throw IOException("Image exceeds size limit")
                connection.inputStream.use { input ->
                    destination.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_IMAGE_BYTES) throw IOException("Image exceeds size limit")
                            output.write(buffer, 0, count)
                        }
                    }
                }
                return
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Too many image redirects")
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 20L * 1024 * 1024
    }
}
