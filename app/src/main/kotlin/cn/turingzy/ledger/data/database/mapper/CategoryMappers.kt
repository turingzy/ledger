package cn.turingzy.ledger.data.database.mapper

import cn.turingzy.ledger.data.database.entity.CategoryEntity
import cn.turingzy.ledger.data.database.entity.IconEntity
import cn.turingzy.ledger.data.database.entity.PopulatedCategory
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon

fun IconEntity.asExternalModel(): Icon = Icon(
    id = iconId,
    keyName = keyName,
    displayName = displayName,
    iconUri = iconUri,
    isSystem = isSystem,
    sortOrder = sortOrder,
    isDeleted = isDeleted,
)

fun Icon.asEntity(): IconEntity = IconEntity(
    iconId = id,
    keyName = keyName,
    displayName = displayName,
    iconUri = iconUri,
    isSystem = isSystem,
    sortOrder = sortOrder,
    isDeleted = isDeleted,
)

fun CategoryEntity.asExternalModel(): Category = Category(
    id = categoryId,
    name = name,
    type = type,
    iconId = iconId,
    sortOrder = sortOrder,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

fun Category.asEntity(): CategoryEntity = CategoryEntity(
    categoryId = id,
    name = name,
    type = type,
    iconId = iconId,
    sortOrder = sortOrder,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

fun PopulatedCategory.asExternalModel(): CategoryWithIcon = CategoryWithIcon(
    category = category.asExternalModel(),
    icon = icon.asExternalModel(),
)
