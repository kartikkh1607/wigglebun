package com.kartikkhandelwal.wigglebun.data.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideBreakLogDao(database: AppDatabase): BreakLogDao = database.breakLogDao()

    @Provides
    fun provideSparkLedgerDao(database: AppDatabase): SparkLedgerDao = database.sparkLedgerDao()
}
