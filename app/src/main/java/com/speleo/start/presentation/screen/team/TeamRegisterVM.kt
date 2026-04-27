package com.speleo.start.presentation.screen.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.PersonRepository
import com.speleo.start.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamRegisterVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val personRepo: PersonRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.length >= 2) personRepo.searchPersons(query)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun registerTeam(
        className: String,
        personIds: List<Long>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val competitionId = prefs.activeCompetitionId
            val teams = teamRepo.getTeamsByCompetition(competitionId)
            // Эта часть требует Flow-подписки, пока упростим
        }
    }
}