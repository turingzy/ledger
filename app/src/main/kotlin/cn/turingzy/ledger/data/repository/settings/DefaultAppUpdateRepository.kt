package cn.turingzy.ledger.data.repository.settings

import cn.turingzy.ledger.data.transfer.readBytesLimited
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
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

/**
 * [AppUpdateRepository] 的默认实现。
 *
 * 通过 GitHub REST API 查询项目的最新 Release 标签信息。
 *
 * @property ioDispatcher 用于执行网络请求的 IO 协程调度器
 */
class DefaultAppUpdateRepository @Inject constructor(
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AppUpdateRepository {
    /**
     * 发送网络请求获取 GitHub 仓库最新 Release 的 tag_name。
     *
     * @return 最新版本标签；若不存在发布版本则返回 null
     * @throws IllegalStateException 当触发 GitHub API 限流或发生请求错误时抛出
     */
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
