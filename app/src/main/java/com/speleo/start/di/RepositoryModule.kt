package com.speleo.start.di

import com.speleo.start.data.local.dao.*
import com.speleo.start.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCompetitionRepository(dao: CompetitionDao): CompetitionRepository {
        return CompetitionRepository(dao)
    }

    @Provides
    @Singleton
    fun providePersonRepository(dao: PersonDao): PersonRepository {
        return PersonRepository(dao)
    }

    @Provides
    @Singleton
    fun provideTeamRepository(dao: TeamDao): TeamRepository {
        return TeamRepository(dao)
    }

    @Provides
    @Singleton
    fun provideParticipantRepository(dao: ParticipantDao): ParticipantRepository {
        return ParticipantRepository(dao)
    }

    @Provides
    @Singleton
    fun provideMentorRepository(dao: MentorDao): MentorRepository {
        return MentorRepository(dao)
    }

    @Provides
    @Singleton
    fun provideMasterRouteCardRepository(dao: MasterRouteCardDao): MasterRouteCardRepository {
        return MasterRouteCardRepository(dao)
    }

    @Provides
    @Singleton
    fun provideTeamRouteCardRepository(dao: TeamRouteCardDao): TeamRouteCardRepository {
        return TeamRouteCardRepository(dao)
    }
}