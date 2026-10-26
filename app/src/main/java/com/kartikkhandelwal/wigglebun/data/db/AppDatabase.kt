package com.kartikkhandelwal.wigglebun.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [BreakLogEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // v1 only had the M1 placeholder table; v2 drops it and adds break_log.
        AutoMigration(from = 1, to = 2, spec = AppDatabase.DropPlaceholder::class),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun breakLogDao(): BreakLogDao

    @DeleteTable(tableName = "placeholder")
    class DropPlaceholder : AutoMigrationSpec

    companion object {
        const val NAME = "wigglebun.db"
    }
}
