package com.speleo.start.presentation.screen.competitions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CompetitionListVM @Inject constructor(
    private val competitionRepository: CompetitionRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    val activeCompetitions = competitionRepository
        .getActiveCompetitions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCompetition(id: Long) {
        prefs.activeCompetitionId = id
    }
}