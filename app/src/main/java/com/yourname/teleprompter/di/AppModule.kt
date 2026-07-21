package com.yourname.teleprompter.di

import android.content.Context
import androidx.room.Room
import com.yourname.teleprompter.data.audio.AudioRecorder
import com.yourname.teleprompter.data.local.AppDatabase
import com.yourname.teleprompter.data.local.ScriptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "teleprompter.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideScriptDao(db: AppDatabase): ScriptDao = db.scriptDao()

    @Provides
    @Singleton
    fun provideAudioRecorder(): AudioRecorder = AudioRecorder()
}