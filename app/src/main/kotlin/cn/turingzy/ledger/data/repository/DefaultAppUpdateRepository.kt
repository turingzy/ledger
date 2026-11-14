package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.data.transfer.readBytesLimited
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DefaultAppUpdateRepository @Inject constructor(
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AppUpdateRepository {
    override suspend fun latestVersion(): String? = withContext(ioDispatcher) {
        val connection = URL("https://api.github.com/repos/turingzy/ledger/releases/latest").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Ledger-Android")
            when (connection.responseCode) {
                404 -> null
                200 -> {
                    val body = connection.inputStream.use { it.readBytesLimited(1024 * 1024).toString(Charsets.UTF_8) }
                    currentCoroutineContext().ensureActive()
                    Json.parseToJsonElement(body).jsonObject.getValue("tag_name").jsonPrimitive.content
                }
                403, 429 -> error("检查过于频繁，请稍后重试")
                else -> error("检查更新失败（${connection.responseCode}）")
            }
        } finally { connection.disconnect() }
    }

}
