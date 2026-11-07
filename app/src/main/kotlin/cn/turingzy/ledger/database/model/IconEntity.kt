package cn.turingzy.ledger.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tb_icon",
    indices = [
        Index(value = ["key_name"], unique = true),
    ],
)
data class IconEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "icon_id")
    val iconId: Long = 0L,
    @ColumnInfo(name = "key_name")
    val keyName: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "icon_uri")
    val iconUri: String,
    @ColumnInfo(name = "is_system", defaultValue = "0")
    val isSystem: Boolean = false,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
)

