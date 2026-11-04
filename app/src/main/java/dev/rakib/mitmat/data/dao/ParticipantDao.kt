package dev.rakib.mitmat.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.rakib.mitmat.data.entity.ParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantDao {
    @Query("SELECT * FROM participants WHERE groupId = :groupId ORDER BY isCurrentUser DESC, name ASC")
    fun getParticipantsForGroup(groupId: Long): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE groupId = :groupId ORDER BY isCurrentUser DESC, name ASC")
    suspend fun getParticipantsForGroupOnce(groupId: Long): List<ParticipantEntity>

    @Query("SELECT * FROM participants")
    fun getAllParticipants(): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants")
    suspend fun getAllParticipantsOnce(): List<ParticipantEntity>

    @Query("SELECT * FROM participants WHERE id = :id LIMIT 1")
    suspend fun getParticipantById(id: Long): ParticipantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    @Update
    suspend fun updateParticipant(participant: ParticipantEntity)

    @Delete
    suspend fun deleteParticipant(participant: ParticipantEntity)

    @Query("DELETE FROM participants WHERE id = :id")
    suspend fun deleteParticipantById(id: Long)

    @Query("DELETE FROM participants WHERE groupId = :groupId")
    suspend fun deleteParticipantsForGroup(groupId: Long)

    @Query("DELETE FROM participants")
    suspend fun deleteAllParticipants()
}
