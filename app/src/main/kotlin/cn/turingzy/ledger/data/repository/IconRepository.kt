package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.model.Icon
import kotlinx.coroutines.flow.Flow

interface IconRepository {
    fun getIconsStream(): Flow<List<Icon>>
    fun getIcon(id: Long): Flow<Icon?>
    suspend fun createIcon(icon: Icon): Long
    suspend fun deleteIcon(id: Long)
}

