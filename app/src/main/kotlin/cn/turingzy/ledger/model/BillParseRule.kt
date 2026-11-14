package cn.turingzy.ledger.model

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
    val valid: Boolean get() = name.isNotBlank() && (startRow.toIntOrNull() ?: 0) > 0 && dateColumn.isNotBlank() && amountColumn.isNotBlank()
    companion object {
        fun defaults() = listOf(
            BillParseRule(1, "通用账单", dateColumn = "交易时间", amountColumn = "金额", typeColumn = "收支类型", categoryColumn = "分类", noteColumn = "备注"),
            BillParseRule(2, "银行流水", dateColumn = "交易日期", amountColumn = "交易金额", typeColumn = "借贷标志", noteColumn = "摘要"),
            BillParseRule(3, "收支明细", dateColumn = "日期", amountColumn = "金额", typeColumn = "收支", categoryColumn = "类别", noteColumn = "说明"),
        )
    }
}

