package com.speleo.start.di

import com.speleo.start.data.local.dao.CompetitionDao
import com.speleo.start.data.local.dao.MasterRouteCardDao
import com.speleo.start.data.local.dao.MentorDao
import com.speleo.start.data.local.dao.ParticipantDao
import com.speleo.start.data.local.dao.PersonDao
import com.speleo.start.data.local.dao.TeamDao
import com.speleo.start.data.local.dao.TeamRouteCardDao
import com.speleo.start.data.repository.CompetitionRepository
import com.speleo.start.data.repository.MasterRouteCardRepository
import com.speleo.start.data.repository.MentorRepository
import com.speleo.start.data.repository.ParticipantRepository
import com.speleo.start.data.repository.PersonRepository
import com.speleo.start.data.repository.TeamRepository
import com.speleo.start.data.repository.TeamRouteCardRepository
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