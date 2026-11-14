package cn.turingzy.ledger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.turingzy.ledger.database.model.CardTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardTemplateDao {
    @Query("SELECT * FROM tb_card_template")
    suspend fun backupRows(): List<CardTemplateEntity>

    @Query("DELETE FROM tb_card_template")
    suspend fun clearRows()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: CardTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplates(templates: List<CardTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: CardTemplateEntity)

    @Query("SELECT * FROM tb_card_template WHERE is_deleted = 0 ORDER BY template_id ASC")
    fun getAllTemplates(): Flow<List<CardTemplateEntity>>

    @Query("SELECT * FROM tb_card_template WHERE template_id = :id AND is_deleted = 0")
    fun getTemplateById(id: Long): Flow<CardTemplateEntity?>

    @Query("SELECT * FROM tb_card_template WHERE template_id = :id AND is_deleted = 0")
    suspend fun getTemplateByIdOnce(id: Long): CardTemplateEntity?

    @Query("UPDATE tb_card_template SET is_deleted = 1 WHERE template_id = :id")
    suspend fun softDeleteTemplate(id: Long): Int
}

