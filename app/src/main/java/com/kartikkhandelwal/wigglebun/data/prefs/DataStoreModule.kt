package com.kartikkhandelwal.wigglebun.data.prefs

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

private const val PREFS_FILE = "wigglebun_prefs"

/** Must match the exclusions in res/xml/backup_rules.xml and data_extraction_rules.xml. */
private const val DEVICE_FILE = "wigglebun_device"

/**
 * State tied to this phone's sensors and boot (see [MovementStore]). Kept in its own file so it
 * can be excluded from backup; the unqualified store (settings, pet name, sit timer) is backed up.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeviceLocal

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    // Both must be singletons: DataStore throws if two instances are active on the same file
    // in one process.
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = resetOnCorruption(),
            migrations = listOf(DropMovedMovementKeys),
            produceFile = { context.preferencesDataStoreFile(PREFS_FILE) },
        )

    @Provides
    @Singleton
    @DeviceLocal
    fun provideDevicePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = resetOnCorruption(),
            produceFile = { context.preferencesDataStoreFile(DEVICE_FILE) },
        )

    // A corrupt file would otherwise fail every read and write (alarms, notification buttons)
    // forever; start again from defaults instead.
    private fun resetOnCorruption() = ReplaceFileCorruptionHandler { emptyPreferences() }
}

/**
 * Movement bookkeeping used to live in the settings file. It now starts fresh in the device file
 * (it rebuilds itself on the next reading), so just remove the old copies from the backed-up file.
 */
private object DropMovedMovementKeys : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        MovementStore.MovedToDeviceStore.any { it in currentData }

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply { MovementStore.MovedToDeviceStore.forEach { remove(it) } }

    override suspend fun cleanUp() = Unit
}
