package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ExpensePayerDao
import com.example.data.dao.ExpenseSplitDao
import com.example.data.dao.GroupDao
import com.example.data.dao.ParticipantDao
import com.example.data.dao.SettlementDao
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpensePayerEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.GroupEntity
import com.example.data.entity.ParticipantEntity
import com.example.data.entity.SettlementEntity

@Database(
    entities = [
        GroupEntity::class,
        ParticipantEntity::class,
        ExpenseEntity::class,
        ExpensePayerEntity::class,
        ExpenseSplitEntity::class,
        SettlementEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MitmatDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun participantDao(): ParticipantDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expensePayerDao(): ExpensePayerDao
    abstract fun expenseSplitDao(): ExpenseSplitDao
    abstract fun settlementDao(): SettlementDao

    companion object {
        @Volatile
        private var INSTANCE: MitmatDatabase? = null

        fun getDatabase(context: Context): MitmatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MitmatDatabase::class.java,
                    "mitmat_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
