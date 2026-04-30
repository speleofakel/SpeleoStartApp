package com.speleo.start.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.speleo.start.data.local.dao.*
import com.speleo.start.data.local.entity.*

@Database(
    entities = [
        CheckpointEntity::class,
        CompetitionEntity::class,
        PersonEntity::class,
        TeamEntity::class,
        ParticipantEntity::class,
        MentorEntity::class,
        MasterRouteCardEntity::class,
        TeamRouteCardEntity::class,
        AppSettingsEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun checkpointDao(): CheckpointDao
    abstract fun competitionDao(): CompetitionDao
    abstract fun personDao(): PersonDao
    abstract fun teamDao(): TeamDao
    abstract fun participantDao(): ParticipantDao
    abstract fun mentorDao(): MentorDao
    abstract fun masterRouteCardDao(): MasterRouteCardDao
    abstract fun teamRouteCardDao(): TeamRouteCardDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Создаём новую таблицу без canBeMentor, с photoUri и updatedAt
                db.execSQL("""
                    CREATE TABLE persons_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        lastName TEXT NOT NULL,
                        firstName TEXT NOT NULL,
                        middleName TEXT,
                        nickname TEXT,
                        birthDate TEXT,
                        phone TEXT,
                        email TEXT,
                        gender TEXT,
                        note TEXT,
                        blacklisted INTEGER NOT NULL DEFAULT 0,
                        blacklistReason TEXT,
                        photoUri TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                // Переносим данные: canBeMentor выбрасываем, updatedAt = createdAt
                db.execSQL("""
                    INSERT INTO persons_new 
                    SELECT id, lastName, firstName, middleName, nickname, birthDate,
                           phone, email, gender, note, blacklisted, blacklistReason,
                           NULL as photoUri, createdAt, createdAt as updatedAt
                    FROM persons
                """)
                db.execSQL("DROP TABLE persons")
                db.execSQL("ALTER TABLE persons_new RENAME TO persons")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "speleo_start.db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}