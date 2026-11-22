package cn.turingzy.ledger.data.repository.settings

/**
 * 应用版本更新数据仓库接口。
 *
 * 提供检查最新发布版本与版本号对比逻辑。
 */
interface AppUpdateRepository {
    /**
     * 检查并获取远程发布的最新版本号标签。
     *
     * @return 最新版本标签（如 "v1.2.0"）；若未找到发布版本则返回 null
     */
    suspend fun latestVersion(): String?

    companion object {
        /**
         * 比较远端版本与当前安装版本，判断是否存在更新。
         *
         * @param latest 远端最新版本字符串（可带或不带前缀 'v'/'V'）
         * @param installed 当前安装的版本字符串
         * @return 如果 [latest] 高于 [installed] 则返回 true，否则返回 false
         * @throws IllegalArgumentException 当版本号不符合点分数字格式时抛出
         */
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
