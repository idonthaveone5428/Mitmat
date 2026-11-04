package dev.rakib.mitmat.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.rakib.mitmat.data.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements ORDER BY dateMillis DESC")
    fun getAllSettlements(): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements ORDER BY dateMillis DESC")
    suspend fun getAllSettlementsOnce(): List<SettlementEntity>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY dateMillis DESC")
    fun getSettlementsForGroup(groupId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY dateMillis DESC")
    suspend fun getSettlementsForGroupOnce(groupId: Long): List<SettlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(settlements: List<SettlementEntity>)

    @Update
    suspend fun updateSettlement(settlement: SettlementEntity)

    @Delete
    suspend fun deleteSettlement(settlement: SettlementEntity)

    @Query("DELETE FROM settlements WHERE id = :id")
    suspend fun deleteSettlementById(id: Long)

    @Query("DELETE FROM settlements WHERE groupId = :groupId")
    suspend fun deleteSettlementsForGroup(groupId: Long)

    @Query("DELETE FROM settlements")
    suspend fun deleteAllSettlements()
}
