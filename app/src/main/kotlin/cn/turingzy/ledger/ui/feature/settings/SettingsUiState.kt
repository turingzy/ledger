package cn.turingzy.ledger.ui.feature.settings

import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.category.CategoryWithIcon

/**
 * 设置界面的 UI 状态接口。
 */
sealed interface SettingsUiState {
    /**
     * 设置数据加载中的状态。
     */
    data object Loading : SettingsUiState

    /**
     * 设置数据成功加载后的状态。
     *
     * @property rules 账单解析规则列表
     * @property message 弹窗提示消息，为 null 时不显示
     * @property appVersion 应用程序版本名称
     * @property categories 分类及其图标列表
     * @property cardTemplates 卡片背景模板列表
     * @property accounts 账户及其模板配置列表
     */
    data class Success(
        val rules: List<cn.turingzy.ledger.model.transfer.BillParseRule> = emptyList(),
        val message: String? = null,
        val appVersion: String = "1.0.0",
        val categories: List<CategoryWithIcon>,
        val cardTemplates: List<CardTemplate> = emptyList(),
        val accounts: List<AccountWithTemplate> = emptyList(),
    ) : SettingsUiState
}
