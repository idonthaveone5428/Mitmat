package dev.rakib.mitmat.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.rakib.mitmat.data.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups WHERE isTrashed = 0 AND isArchived = 0 ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE isTrashed = 0 AND isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun getTrashedGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE isTrashed = 0 ORDER BY createdAt DESC")
    fun getAllActiveAndArchivedGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups")
    suspend fun getAllGroupsRawOnce(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    fun getGroupById(groupId: Long): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupByIdOnce(groupId: Long): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: Long)

    @Query("DELETE FROM groups WHERE isTrashed = 1 AND trashedAt IS NOT NULL AND trashedAt < :cutoffMillis")
    suspend fun deleteTrashedOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM groups WHERE isTrashed = 1")
    suspend fun deleteAllTrashed(): Int

    @Query("DELETE FROM groups WHERE name IN ('Sylhet Trip', 'Flatmates')")
    suspend fun deleteDemoGroups()

    @Query("DELETE FROM groups")
    suspend fun deleteAllGroups()
}
