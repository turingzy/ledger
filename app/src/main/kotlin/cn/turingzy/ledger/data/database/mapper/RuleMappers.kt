package cn.turingzy.ledger.data.database.mapper

import cn.turingzy.ledger.data.database.entity.ParseRuleEntity
import cn.turingzy.ledger.model.transfer.ParseRule

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
