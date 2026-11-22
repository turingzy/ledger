package cn.turingzy.ledger.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 卡片样式模板数据库实体，对应表 `tb_card_template`。
 *
 * 定义账户卡片的背景色/渐变资源 URI 以及前景色主题。
 *
 * @property templateId 模板唯一标识，自增主键
 * @property name 模板名称（如“中国工商银行”、“微信支付”）
 * @property coverUri 封面资源标识符或渐变定义 URI
 * @property textColor 卡片文字颜色 Hex 值（例如 "#FFFFFF"）
 * @property isSystem 是否为系统预置模板
 * @property isDeleted 软删除标记，true 表示已被软删除
 */
@Entity(tableName = "tb_card_template")
data class CardTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "template_id")
    val templateId: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "cover_uri")
    val coverUri: String,
    @ColumnInfo(name = "text_color", defaultValue = "'#FFFFFF'")
    val textColor: String = "#FFFFFF",
    @ColumnInfo(name = "is_system", defaultValue = "0")
    val isSystem: Boolean = false,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
)

