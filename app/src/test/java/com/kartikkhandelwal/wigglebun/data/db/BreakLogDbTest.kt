package com.kartikkhandelwal.wigglebun.data.db

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
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
        db.close()
    }

    /**
     * Builds a real v1 database from the exported schema (app/schemas/.../1.json, including
     * Room's identity hash), then opens it with the v2 AppDatabase so the auto-migration runs
     * and Room validates the result.
     */
    @Test
    fun migratesFromVersion1() = runBlocking {
        val file = context.getDatabasePath("migration-test.db").apply { parentFile?.mkdirs(); delete() }
        createV1(file)

        val db = Room.databaseBuilder(context, AppDatabase::class.java, file.absolutePath).build()
        db.breakLogDao().insert(BreakLogEntity(type = BreakType.STAND_UP, completedAt = 1, reward = 5, outcome = BreakOutcome.DONE))
        assertEquals(1, db.breakLogDao().countDoneSince(0))

        val tables = mutableListOf<String>()
        db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            while (c.moveToNext()) tables += c.getString(0)
        }
        assertTrue("break_log" in tables)
        assertFalse("placeholder" in tables)
        assertEquals(2, db.openHelper.readableDatabase.version)
        db.close()
    }

    private fun createV1(file: File) {
        val schema = JSONObject(File("schemas/${AppDatabase::class.java.name}/1.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sqlite ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) sqlite.execSQL(setup.getString(i))
            sqlite.version = 1
        }
    }
}
