package com.speleo.start.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.speleo.start.data.local.dao.*
import com.speleo.start.data.local.entity.*
import com.speleo.start.data.local.entity.CheckpointEntity

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
    version = 5,
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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "speleo_start.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}