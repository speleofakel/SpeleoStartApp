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
    private val _shouldRefreshStartQueue = MutableStateFlow(0L)
    val shouldRefreshStartQueue: StateFlow<Long> = _shouldRefreshStartQueue.asStateFlow()

    fun refreshStartQueue() {
        _shouldRefreshStartQueue.value = System.currentTimeMillis()
    }
    fun selectTeam(teamId: Long) {
        _selectedTeamId.value = teamId
    }
}