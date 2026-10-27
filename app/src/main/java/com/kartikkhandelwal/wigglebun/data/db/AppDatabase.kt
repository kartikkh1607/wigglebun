package com.kartikkhandelwal.wigglebun.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [BreakLogEntity::class, SparkLedgerEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v1 only had the M1 placeholder table; v2 drops it and adds break_log.
        AutoMigration(from = 1, to = 2, spec = AppDatabase.DropPlaceholder::class),
        // v3 adds spark_ledger (M6). The old DataStore balance was always 0, so nothing moves over.
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun breakLogDao(): BreakLogDao

    abstract fun sparkLedgerDao(): SparkLedgerDao

    @DeleteTable(tableName = "placeholder")
    class DropPlaceholder : AutoMigrationSpec

    companion object {
        const val NAME = "wigglebun.db"
    }
}
