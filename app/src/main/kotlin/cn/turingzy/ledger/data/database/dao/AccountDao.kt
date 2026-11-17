package cn.turingzy.ledger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.data.database.entity.AccountEntity
import cn.turingzy.ledger.data.database.entity.PopulatedAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("UPDATE tb_account SET base_balance = :balance, base_currency = :currency, base_rate = :rate, base_rate_date = :date WHERE account_id = :id AND current_balance = :original")
    suspend fun updateValuation(id: Long, original: Long, balance: Long, currency: String, rate: String?, date: String)

    @Query("SELECT * FROM tb_account WHERE account_id = :id")
    suspend fun getAccountIncludingDeleted(id: Long): AccountEntity?

    @Query("SELECT * FROM tb_account")
    suspend fun backupRows(): List<AccountEntity>

    @Query("DELETE FROM tb_account")
    suspend fun clearRows()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query(
        """
        UPDATE tb_account
        SET name = :name, type = :type, initial_balance = :initialBalance,
            template_id = :templateId, card_number = :cardNumber,
            include_in_total = :includeInTotal
        WHERE account_id = :accountId AND is_deleted = 0
        """,
    )
    suspend fun updateAccountInfo(
        accountId: Long,
        name: String,
        type: AccountType,
        initialBalance: Long,
        templateId: Long?,
        cardNumber: String?,
        includeInTotal: Boolean,
    ): Int

    @Query("UPDATE tb_account SET is_deleted = 1 WHERE account_id = :id")
    suspend fun softDeleteAccount(id: Long): Int

    @Transaction
    @Query("SELECT * FROM tb_account WHERE is_deleted = 0 ORDER BY account_id ASC")
    fun getPopulatedAccounts(): Flow<List<PopulatedAccount>>

    @Transaction
    @Query("SELECT * FROM tb_account WHERE account_id = :id AND is_deleted = 0")
    fun getPopulatedAccountById(id: Long): Flow<PopulatedAccount?>

    @Query("SELECT * FROM tb_account WHERE account_id = :id AND is_deleted = 0")
    suspend fun getAccountByIdOnce(id: Long): AccountEntity?

    @Query("UPDATE tb_account SET current_balance = current_balance + :deltaAmount, base_balance = NULL WHERE account_id = :accountId")
    suspend fun updateBalance(accountId: Long, deltaAmount: Long): Int

    @Query("SELECT COALESCE(SUM(current_balance), 0) FROM tb_account WHERE include_in_total = 1 AND is_deleted = 0")
    fun getTotalAsset(): Flow<Long>

    @Query("SELECT COALESCE(SUM(current_balance), 0) FROM tb_account WHERE type = 'CREDIT' AND is_deleted = 0")
    fun getTotalCreditDebt(): Flow<Long>
}

