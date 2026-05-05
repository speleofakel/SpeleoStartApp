package com.speleo.start.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedState @Inject constructor() {

    private val _selectedTeamId = MutableStateFlow(-1L)
    val selectedTeamId: StateFlow<Long> = _selectedTeamId.asStateFlow()

    fun selectTeam(teamId: Long) {
        _selectedTeamId.value = teamId
    }
}