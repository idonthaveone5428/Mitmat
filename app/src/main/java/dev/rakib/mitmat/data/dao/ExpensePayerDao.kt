package dev.rakib.mitmat.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.rakib.mitmat.data.entity.ExpensePayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpensePayerDao {
    @Query("SELECT * FROM expense_payers")
    fun getAllExpensePayers(): Flow<List<ExpensePayerEntity>>

    @Query("SELECT * FROM expense_payers")
    suspend fun getAllExpensePayersOnce(): List<ExpensePayerEntity>

    @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
    fun getPayersForExpense(expenseId: Long): Flow<List<ExpensePayerEntity>>

    @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun getPayersForExpenseOnce(expenseId: Long): List<ExpensePayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayers(payers: List<ExpensePayerEntity>)

    @Query("DELETE FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun deletePayersForExpense(expenseId: Long)

    @Query("DELETE FROM expense_payers")
    suspend fun deleteAllPayers()
}
