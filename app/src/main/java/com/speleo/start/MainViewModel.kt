package com.speleo.start

import androidx.lifecycle.ViewModel
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val competitionRepository: CompetitionRepository
) : ViewModel() {

    private val _activeCompetitionName = MutableStateFlow<String?>(null)
    val activeCompetitionName: StateFlow<String?> = _activeCompetitionName.asStateFlow()

    fun setActiveCompetition(id: Long) {
        prefs.activeCompetitionId = id
        // TODO: загрузить имя соревнования по id
        _activeCompetitionName.value = "Соревнование #$id"
    }
}