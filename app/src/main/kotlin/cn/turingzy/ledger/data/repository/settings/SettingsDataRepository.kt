package cn.turingzy.ledger.data.repository.settings

import android.net.Uri
import cn.turingzy.ledger.model.transfer.BillParseRule
import cn.turingzy.ledger.model.account.CardTemplate

/**
 * 设置中心综合数据操作仓库接口。
 *
 * 聚合系统默认解析规则初始化、卡片模板与图片导入、账单导入/导出（CSV/Excel）、
 * 全量/部分备份与恢复等设置模块的核心数据能力。
 */
interface SettingsDataRepository {
    /**
     * 检查并确保系统内置的默认账单解析规则（微信、支付宝等）已初始化入库。
     */
    suspend fun ensureDefaultRules()

    /**
     * 保存卡面模板（支持新增或更新）。
     *
     * 自动处理卡面图片的本地化存储，防止使用易失效的临时 content URI。
     *
     * @param template 待保存的卡面模板
     * @return 模板的自增 ID
     */
    suspend fun saveTemplate(template: CardTemplate): Long

    /**
     * 将外部 URI 对应的图片拷贝保存至应用内部私有存储目录。
     *
     * @param uri 图片的 URI
     * @return 存储在私有目录中的文件 file URI 字符串
     */
    suspend fun copyImage(uri: Uri): String

    /**
     * 导出指定时间范围内的账单数据到目标 URI。
     *
     * @param uri 导出的目标文件 URI
     * @param excel 为 true 导出为 Excel (.xlsx) 格式，为 false 导出为 CSV 格式
     * @param start 起始时间戳（毫秒）
     * @param end 截止时间戳（毫秒）
     * @return 成功导出的账单记录条数
     */
    suspend fun exportBills(uri: Uri, excel: Boolean, start: Long, end: Long): Int

    /**
     * 预解析并预览账单导入，检测导入条数、确定的重复项以及疑似重复项，不写入数据库。
     *
     * @param uri 账单文件 URI
     * @param rule 账单解析映射规则
     * @param selectedAccountId 预选的目标账户 ID（可为空）
     * @return 导入规划与统计结果 [cn.turingzy.ledger.model.transfer.BillImportResult]
     */
    suspend fun previewImport(uri: Uri, rule: BillParseRule, selectedAccountId: Long?): cn.turingzy.ledger.model.transfer.BillImportResult

    /**
     * 正式执行账单文件的解析与入库导入。
     *
     * 包含去重校验、外币估值计算、账户余额原子更新与交易记录插入。
     *
     * @param uri 账单文件 URI
     * @param rule 账单解析映射规则
     * @param selectedAccountId 预选的目标账户 ID（可为空）
     * @param includePossibleDuplicates 是否同时导入被标记为疑似重复的账单项，默认为 false
     * @return 最终的导入结果统计 [cn.turingzy.ledger.model.transfer.BillImportResult]
     */
    suspend fun importBills(uri: Uri, rule: BillParseRule, selectedAccountId: Long?, includePossibleDuplicates: Boolean = false): cn.turingzy.ledger.model.transfer.BillImportResult

    /**
     * 创建应用数据备份并写入到指定目标 URI。
     *
     * @param uri 备份文件输出 URI
     * @param includeBills 是否包含账单数据（账户、交易、分类、图标等）
     * @param includeSettings 是否包含设置数据（解析规则、汇率、偏好设置、卡片外观等）
     */
    suspend fun backup(uri: Uri, includeBills: Boolean, includeSettings: Boolean)

    /**
     * 从指定 URI 恢复备份数据。
     *
     * 校验备份格式和数据完整性，并在事务中恢复数据库、偏好设置与卡片外观资源。
     *
     * @param uri 备份源文件 URI
     */
    suspend fun restore(uri: Uri)
}
