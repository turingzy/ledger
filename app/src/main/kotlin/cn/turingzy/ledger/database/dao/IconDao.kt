package cn.turingzy.ledger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.turingzy.ledger.database.model.IconEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IconDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIcon(icon: IconEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIcons(icons: List<IconEntity>)

    @Update
    suspend fun updateIcon(icon: IconEntity)

    @Query("SELECT * FROM tb_icon WHERE is_deleted = 0 ORDER BY sort_order ASC, icon_id ASC")
    fun getAllIcons(): Flow<List<IconEntity>>

    @Query("SELECT * FROM tb_icon WHERE icon_id = :id AND is_deleted = 0")
    fun getIconById(id: Long): Flow<IconEntity?>

    @Query("SELECT * FROM tb_icon WHERE icon_id = :id AND is_deleted = 0")
    suspend fun getIconByIdOnce(id: Long): IconEntity?

    @Query("UPDATE tb_icon SET is_deleted = 1 WHERE icon_id = :id")
    suspend fun softDeleteIcon(id: Long): Int
}

