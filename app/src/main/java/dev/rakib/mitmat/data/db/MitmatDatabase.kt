package dev.rakib.mitmat.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.rakib.mitmat.data.dao.ExpenseDao
import dev.rakib.mitmat.data.dao.ExpensePayerDao
import dev.rakib.mitmat.data.dao.ExpenseSplitDao
import dev.rakib.mitmat.data.dao.GroupDao
import dev.rakib.mitmat.data.dao.ParticipantDao
import dev.rakib.mitmat.data.dao.SettlementDao
import dev.rakib.mitmat.data.entity.ExpenseEntity
import dev.rakib.mitmat.data.entity.ExpensePayerEntity
import dev.rakib.mitmat.data.entity.ExpenseSplitEntity
import dev.rakib.mitmat.data.entity.GroupEntity
import dev.rakib.mitmat.data.entity.ParticipantEntity
import dev.rakib.mitmat.data.entity.SettlementEntity

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
