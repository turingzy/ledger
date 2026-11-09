package cn.turingzy.ledger.util

import java.net.URI
import java.net.URISyntaxException

internal fun parseImageUrl(value: String): URI? = try {
    val uri = URI(value.trim())
    uri.takeIf {
        it.scheme.equals("https", ignoreCase = true) &&
            !it.host.isNullOrBlank() && it.userInfo == null
    }
} catch (_: URISyntaxException) {
    null
}
