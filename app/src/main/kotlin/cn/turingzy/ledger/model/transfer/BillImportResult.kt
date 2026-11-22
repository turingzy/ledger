package cn.turingzy.ledger.model.transfer

/**
 * 外部账单文件导入执行结果统计数据模型。
 *
 * 记录账单导入完成后的入账、完全去重及疑似重复交易的数量。
 *
 * @property imported 成功导入入账的交易笔数
 * @property duplicates 确认为完全重复并自动跳过的交易笔数
 * @property possibleDuplicates 检测为疑似重复（如时间与金额相近）的交易笔数
 */
data class BillImportResult(val imported: Int = 0, val duplicates: Int = 0, val possibleDuplicates: Int = 0)
