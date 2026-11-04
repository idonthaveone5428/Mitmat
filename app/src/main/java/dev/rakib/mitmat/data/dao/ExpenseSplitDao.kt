package dev.rakib.mitmat.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.rakib.mitmat.data.entity.ExpenseSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseSplitDao {
    @Query("SELECT * FROM expense_splits")
    fun getAllSplits(): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits")
    suspend fun getAllSplitsOnce(): List<ExpenseSplitEntity>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpenseOnce(expenseId: Long): List<ExpenseSplitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: Long)

    @Query("DELETE FROM expense_splits")
    suspend fun deleteAllSplits()
}
