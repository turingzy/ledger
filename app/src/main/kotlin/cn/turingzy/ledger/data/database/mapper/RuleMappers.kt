package cn.turingzy.ledger.data.database.mapper

import cn.turingzy.ledger.data.database.entity.ParseRuleEntity
import cn.turingzy.ledger.model.transfer.ParseRule

/**
 * 将解析规则实体 [ParseRuleEntity] 转换为业务领域模型 [ParseRule]。
 */
fun ParseRuleEntity.asExternalModel(): ParseRule = ParseRule(
    id = ruleId,
    name = name,
    startRow = startRow,
    mappingConfig = mappingConfig,
    defaultAccountId = defaultAccountId,
    defaultCategoryId = defaultCategoryId,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

/**
 * 将业务领域模型 [ParseRule] 转换为数据库实体 [ParseRuleEntity]。
 */
fun ParseRule.asEntity(): ParseRuleEntity = ParseRuleEntity(
    ruleId = id,
    name = name,
    startRow = startRow,
    mappingConfig = mappingConfig,
    defaultAccountId = defaultAccountId,
    defaultCategoryId = defaultCategoryId,
    isSystem = isSystem,
    isDeleted = isDeleted,
)
