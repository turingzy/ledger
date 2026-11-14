package cn.turingzy.ledger.data.repository

interface AppUpdateRepository {
    suspend fun latestVersion(): String?

    companion object {
        fun isNewer(latest: String, installed: String): Boolean {
            fun parts(value: String): List<Long> {
                val text = value.removePrefix("v").removePrefix("V")
                require(Regex("[0-9]+(\\.[0-9]+)*").matches(text)) { "无法识别版本号：$value" }
                return text.split('.').map { it.toLong() }
            }
            val left = parts(latest)
            val right = parts(installed)
            for (index in 0 until maxOf(left.size, right.size)) {
                val comparison = (left.getOrNull(index) ?: 0).compareTo(right.getOrNull(index) ?: 0)
                if (comparison != 0) return comparison > 0
            }
            return false
        }
    }
}
