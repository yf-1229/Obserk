package com.obserk.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [StudyLogEntity::class], // ラベルを削除
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyLogDao(): StudyLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_database"
                )
                    .fallbackToDestructiveMigration() // 開発用：構造変更時にデータをリセット
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}