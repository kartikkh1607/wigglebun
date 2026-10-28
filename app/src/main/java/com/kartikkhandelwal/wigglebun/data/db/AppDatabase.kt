package com.kartikkhandelwal.wigglebun.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kartikkhandelwal.wigglebun.domain.rewards.RewardRules

@Database(
    entities = [BreakLogEntity::class, SparkLedgerEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        // v1 only had the M1 placeholder table; v2 drops it and adds break_log.
        AutoMigration(from = 1, to = 2, spec = AppDatabase.DropPlaceholder::class),
        // v3 adds spark_ledger (M6). The old DataStore balance was always 0, so nothing moves over.
        AutoMigration(from = 2, to = 3),
        // v4 changes no tables: streak milestones become once per lifetime (see LifetimeMilestones).
        AutoMigration(from = 3, to = 4, spec = AppDatabase.LifetimeMilestones::class),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun breakLogDao(): BreakLogDao

    abstract fun sparkLedgerDao(): SparkLedgerDao

    @DeleteTable(tableName = "placeholder")
    class DropPlaceholder : AutoMigrationSpec

    /**
     * Up to v3 a milestone paid once per streak, keyed `streak:<days>:<start>`. From v4 each pays
     * once per lifetime under `milestone:<days>`. The first payment of each milestone is renamed to
     * the lifetime key, so an existing install never earns it twice; later repeat payments keep
     * their old keys as history (no sparks are taken back).
     */
    class LifetimeMilestones : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            RewardRules.MILESTONES.forEach { milestone ->
                val lifetimeKey = RewardRules.milestoneKey(milestone.days)
                db.execSQL(
                    """
                    UPDATE spark_ledger SET `key` = ?
                    WHERE id = (SELECT MIN(id) FROM spark_ledger WHERE `key` LIKE ?)
                      AND NOT EXISTS (SELECT 1 FROM spark_ledger WHERE `key` = ?)
                    """.trimIndent(),
                    arrayOf(lifetimeKey, "${RewardRules.LEGACY_STREAK_PREFIX}${milestone.days}:%", lifetimeKey),
                )
            }
        }
    }

    companion object {
        const val NAME = "wigglebun.db"
    }
}
