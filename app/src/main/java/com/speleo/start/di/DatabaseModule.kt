package com.speleo.start.di

import android.content.Context
import androidx.room.Room
import com.speleo.start.data.TestDataGenerator
import com.speleo.start.data.local.AppDatabase
import com.speleo.start.data.local.dao.AppSettingsDao
import com.speleo.start.data.local.dao.CheckpointDao
import com.speleo.start.data.local.dao.CompetitionDao
import com.speleo.start.data.local.dao.MasterRouteCardDao
import com.speleo.start.data.local.dao.MentorDao
import com.speleo.start.data.local.dao.ParticipantDao
import com.speleo.start.data.local.dao.PersonDao
import com.speleo.start.data.local.dao.TeamDao
import com.speleo.start.data.local.dao.TeamRouteCardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "speleo_start.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides @Singleton
    fun provideCompetitionDao(db: AppDatabase): CompetitionDao = db.competitionDao()

    @Provides @Singleton
    fun providePersonDao(db: AppDatabase): PersonDao = db.personDao()

    @Provides @Singleton
    fun provideTeamDao(db: AppDatabase): TeamDao = db.teamDao()

    @Provides @Singleton
    fun provideParticipantDao(db: AppDatabase): ParticipantDao = db.participantDao()

    @Provides @Singleton
    fun provideMentorDao(db: AppDatabase): MentorDao = db.mentorDao()

    @Provides @Singleton
    fun provideMasterRouteCardDao(db: AppDatabase): MasterRouteCardDao = db.masterRouteCardDao()

    @Provides @Singleton
    fun provideTeamRouteCardDao(db: AppDatabase): TeamRouteCardDao = db.teamRouteCardDao()

    @Provides @Singleton
    fun provideAppSettingsDao(db: AppDatabase): AppSettingsDao = db.appSettingsDao()

    @Provides @Singleton
    fun provideCheckpointDao(db: AppDatabase): CheckpointDao = db.checkpointDao()


    @Provides @Singleton
    fun provideTestDataGenerator(
        db: AppDatabase,
        competitionDao: CompetitionDao,
        personDao: PersonDao,
        teamDao: TeamDao,
        participantDao: ParticipantDao,
        mentorDao: MentorDao,
        masterRouteCardDao: MasterRouteCardDao,
        appSettingsDao: AppSettingsDao
    ): TestDataGenerator = TestDataGenerator(db, competitionDao, personDao, teamDao, participantDao, mentorDao, masterRouteCardDao, appSettingsDao)
}