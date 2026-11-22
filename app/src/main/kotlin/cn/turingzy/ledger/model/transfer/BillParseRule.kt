package cn.turingzy.ledger.model.transfer

/**
 * 账单文件解析规则模型。
 *
 * 用于在导入 CSV 或 Excel 账单时，建立电子表格中的列与交易属性之间的映射关系。
 *
 * @property id 规则唯一标识
 * @property name 规则显示名称（如“通用账单”、“银行流水”）
 * @property startRow 数据起始行号字符串（如 "2"，默认为跳过首行表头）
 * @property dateColumn 交易日期/时间所在列的列名或表头文本
 * @property amountColumn 交易金额所在列的列名或表头文本
 * @property typeColumn 收支类型所在列的列名或表头文本，可为空
 * @property categoryColumn 分类名称所在列的列名或表头文本，可为空
 * @property noteColumn 交易备注或说明所在列的列名或表头文本，可为空
 * @property accountId 默认归属账户 ID，可为空
 * @property categoryId 默认兜底分类 ID，可为空
 */
@kotlinx.serialization.Serializable
data class BillParseRule(
    val id: Long,
    val name: String = "",
    val startRow: String = "2",
    val dateColumn: String = "",
    val amountColumn: String = "",
    val typeColumn: String = "",
    val categoryColumn: String = "",
    val noteColumn: String = "",
    val accountId: Long? = null,
    val categoryId: Long? = null,
) {
    /**
     * 判断当前解析规则的核心配置是否合法有效。
     *
     * 要求规则名称非空、起始行必须为大于 0 的有效整数、且日期列与金额列映射均非空。
     */
    val valid: Boolean get() = name.isNotBlank() && (startRow.toIntOrNull() ?: 0) > 0 && dateColumn.isNotBlank() && amountColumn.isNotBlank()
    companion object {
        /**
         * 获取系统预设的默认账单解析规则列表。
         *
         * 包含“通用账单”、“银行流水”与“收支明细”三类常见模板。
         *
         * @return 预设规则列表
         */
        fun defaults() = listOf(
            BillParseRule(1, "通用账单", dateColumn = "交易时间", amountColumn = "金额", typeColumn = "收支类型", categoryColumn = "分类", noteColumn = "备注"),
            BillParseRule(2, "银行流水", dateColumn = "交易日期", amountColumn = "交易金额", typeColumn = "借贷标志", noteColumn = "摘要"),
            BillParseRule(3, "收支明细", dateColumn = "日期", amountColumn = "金额", typeColumn = "收支", categoryColumn = "类别", noteColumn = "说明"),
        )
    }
}

