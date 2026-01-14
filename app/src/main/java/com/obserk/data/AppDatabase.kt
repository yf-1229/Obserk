package com.obserk.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StudyLogEntity::class, StudyLabelEntity::class], // StudyLabelEntity を追加
    version = 4, // バージョンを更新
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyLogDao(): StudyLogDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "obserk_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
