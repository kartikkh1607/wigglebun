package com.kartikkhandelwal.wigglebun.data.db

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class BreakLogDbTest {

    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun daoInsertsAndQueries() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val dao = db.breakLogDao()
        dao.insert(BreakLogEntity(type = BreakType.WALK, completedAt = 1_000, reward = 10, outcome = BreakOutcome.DONE))
        dao.insert(BreakLogEntity(type = BreakType.NECK, completedAt = 2_000, reward = 0, outcome = BreakOutcome.SKIPPED))
        dao.insert(BreakLogEntity(type = BreakType.EYES, completedAt = 3_000, reward = 5, outcome = BreakOutcome.DONE))

        val since = dao.observeSince(1_500).first()
        assertEquals(listOf(BreakType.EYES, BreakType.NECK), since.map { it.type })
        assertEquals(1, dao.countDoneSince(1_500))
        assertEquals(2, dao.countDoneSince(0))
        assertEquals(listOf(DoneBreakRow(BreakType.WALK, 1_000), DoneBreakRow(BreakType.EYES, 3_000)), dao.doneBreaks())
        db.close()
    }

    /**
     * Builds a real v1 database from the exported schema (app/schemas/.../1.json, including
     * Room's identity hash), then opens it with the current AppDatabase so the auto-migrations
     * run and Room validates the result.
     */
    @Test
    fun migratesFromVersion1() = runBlocking {
        val file = freshFile("migration-test.db")
        createFromSchema(file, version = 1)

        val db = Room.databaseBuilder(context, AppDatabase::class.java, file.absolutePath).build()
        db.breakLogDao().insert(BreakLogEntity(type = BreakType.STAND_UP, completedAt = 1, reward = 5, outcome = BreakOutcome.DONE))
        assertEquals(1, db.breakLogDao().countDoneSince(0))

        val tables = tables(db)
        assertTrue("break_log" in tables)
        assertFalse("placeholder" in tables)
        assertEquals(3, db.openHelper.readableDatabase.version)
        db.close()
    }

    /** M6: v2 → v3 adds spark_ledger and keeps the break log. */
    @Test
    fun migratesFromVersion2() = runBlocking {
        val file = freshFile("migration-v2-test.db")
        createFromSchema(file, version = 2) { sqlite ->
            sqlite.execSQL("INSERT INTO break_log (type, completedAt, reward, outcome) VALUES ('NECK', 42, 5, 'DONE')")
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, file.absolutePath).build()
        assertEquals(listOf(DoneBreakRow(BreakType.NECK, 42)), db.breakLogDao().doneBreaks())
        assertTrue("spark_ledger" in tables(db))
        assertEquals(3, db.openHelper.readableDatabase.version)

        val ledger = db.sparkLedgerDao()
        val row = SparkLedgerEntity(amount = 5, reason = SparkReason.BREAK, key = "break:2026-10-27:NECK", createdAt = 1)
        assertTrue(ledger.insert(row) > 0)
        assertEquals(-1L, ledger.insert(row.copy(createdAt = 2))) // unique key: ignored
        assertEquals(5, ledger.observeBalance().first())
        db.close()
    }

    private fun freshFile(name: String): File =
        context.getDatabasePath(name).apply { parentFile?.mkdirs(); delete() }

    private fun tables(db: AppDatabase): List<String> {
        val tables = mutableListOf<String>()
        db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            while (c.moveToNext()) tables += c.getString(0)
        }
        return tables
    }

    /** A database exactly as Room created it at [version] (exported schema JSON), plus [seed]. */
    private fun createFromSchema(file: File, version: Int, seed: (SQLiteDatabase) -> Unit = {}) {
        val schema = JSONObject(File("schemas/${AppDatabase::class.java.name}/$version.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sqlite ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    sqlite.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) sqlite.execSQL(setup.getString(i))
            seed(sqlite)
            sqlite.version = version
        }
    }
}
