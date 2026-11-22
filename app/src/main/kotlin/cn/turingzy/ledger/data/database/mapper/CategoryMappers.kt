package cn.turingzy.ledger.data.database.mapper

import cn.turingzy.ledger.data.database.entity.CategoryEntity
import cn.turingzy.ledger.data.database.entity.IconEntity
import cn.turingzy.ledger.data.database.entity.PopulatedCategory
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon

/**
 * 将图标实体 [IconEntity] 转换为业务领域模型 [Icon]。
 */
fun IconEntity.asExternalModel(): Icon = Icon(
    id = iconId,
    keyName = keyName,
    displayName = displayName,
    iconUri = iconUri,
    isSystem = isSystem,
    sortOrder = sortOrder,
    isDeleted = isDeleted,
)

/**
 * 将业务领域模型 [Icon] 转换为数据库实体 [IconEntity]。
 */
fun Icon.asEntity(): IconEntity = IconEntity(
    iconId = id,
    keyName = keyName,
    displayName = displayName,
    iconUri = iconUri,
    isSystem = isSystem,
    sortOrder = sortOrder,
    isDeleted = isDeleted,
)

/**
 * 将分类实体 [CategoryEntity] 转换为业务领域模型 [Category]。
 */
fun CategoryEntity.asExternalModel(): Category = Category(
    id = categoryId,
    name = name,
    type = type,
    iconId = iconId,
    sortOrder = sortOrder,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

/**
 * 将业务领域模型 [Category] 转换为数据库实体 [CategoryEntity]。
 */
fun Category.asEntity(): CategoryEntity = CategoryEntity(
    categoryId = id,
    name = name,
    type = type,
    iconId = iconId,
    sortOrder = sortOrder,
    isSystem = isSystem,
    isDeleted = isDeleted,
)

/**
 * 将聚合分类实体 [PopulatedCategory] 转换为包含图标的领域聚合模型 [CategoryWithIcon]。
 */
fun PopulatedCategory.asExternalModel(): CategoryWithIcon = CategoryWithIcon(
    category = category.asExternalModel(),
    icon = icon.asExternalModel(),
)
