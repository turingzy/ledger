package cn.turingzy.ledger.model.transaction

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.category.CategoryWithIcon
import kotlinx.serialization.Serializable

/**
 * 交易业务数据模型。
 *
 * 记录单笔收入、支出或转账交易的核心业务属性，支持多币种汇率快照与导入防重。
 *
 * @property id 交易唯一标识，自增主键
 * @property type 交易类型，详见 [TransactionType]
 * @property amount 交易金额（单位：分）。转账类型时表示转出金额
 * @property accountId 关联账户 ID（转账时表示转出源账户）
 * @property toAccountId 转账目标账户 ID，仅转账交易有效，其他类型为 null
 * @property categoryId 关联分类 ID
 * @property transactionTime 交易发生时间戳（毫秒）
 * @property note 交易备注说明，可为空
 * @property isDeleted 软删除标记，true 表示已被删除
 * @property importKey 导入去重唯一键，用于账单导入时检测重复数据
 * @property baseAmount 折算为主币种后的金额（单位：分）
 * @property baseCurrency 主币种代码，与 [baseAmount] 对应
 * @property baseRate 折算为主币种时采用的汇率快照
 * @property baseRateDate 主币种汇率生效日期（格式：yyyy-MM-dd）
 * @property toAmount 目标账户实际入账金额（单位：分），用于跨币种转账
 * @property transferRate 转账汇率（转出币种兑转入币种的汇率），用于跨币种转账
 * @property currencySnapshots 交易发生时的多币种换算历史快照列表
 */
@Serializable
data class Transaction(
    val id: Long = 0L,
    val type: TransactionType,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long,
    val transactionTime: Long,
    val note: String? = null,
    val isDeleted: Boolean = false,
    val importKey: String? = null,
    val baseAmount: Long? = null,
    val baseCurrency: String? = null,
    val baseRate: String? = null,
    val baseRateDate: String? = null,
    val toAmount: Long? = null,
    val transferRate: String? = null,
    val currencySnapshots: List<CurrencySnapshot> = emptyList(),
)

/**
 * 包含完整关联实体信息的交易详情数据模型。
 *
 * 聚合了交易记录、关联账户及带图标分类等数据，用于 UI 层直接渲染展示。
 *
 * @property transaction 交易基本信息
 * @property account 交易关联账户（转账时的转出账户）
 * @property toAccount 转账目标账户，非转账场景为 null
 * @property category 关联的分类及其图标信息
 */
@Serializable
data class TransactionDetail(
    val transaction: Transaction,
    val account: Account,
    val toAccount: Account? = null,
    val category: CategoryWithIcon,
)


/**
 * 交易多币种估值快照模型。
 *
 * 记录交易在某一特定货币下的换算金额与汇率历史快照。
 *
 * @property currency 货币代码（ISO 4217）
 * @property amount 折算金额（单位：分）
 * @property rate 折算采用的汇率值
 * @property date 汇率生效日期（格式：yyyy-MM-dd）
 */
@Serializable
data class CurrencySnapshot(val currency: String, val amount: Long, val rate: String, val date: String)
