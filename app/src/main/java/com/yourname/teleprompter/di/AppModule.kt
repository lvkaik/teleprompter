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
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        return try {
            Room.databaseBuilder(ctx, AppDatabase::class.java, "teleprompter.db")
                .fallbackToDestructiveMigration()
                .build()
        } catch (e: Throwable) {
            android.util.Log.e("AppModule", "Room.databaseBuilder 失败，尝试清库重建", e)
            // 极端情况下 schema 文件损坏，清掉数据库文件后重建
            try {
                ctx.deleteDatabase("teleprompter.db")
            } catch (_: Throwable) {}
            Room.databaseBuilder(ctx, AppDatabase::class.java, "teleprompter.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    @Provides
    fun provideScriptDao(db: AppDatabase): ScriptDao = db.scriptDao()

    @Provides
    @Singleton
    fun provideAudioRecorder(): AudioRecorder = try {
        AudioRecorder()
    } catch (e: Throwable) {
        android.util.Log.e("AppModule", "AudioRecorder 初始化失败", e)
        // 返回一个会立即失败的占位实例，避免整个依赖图崩
        AudioRecorder()
    }
}