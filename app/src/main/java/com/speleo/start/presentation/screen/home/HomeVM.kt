package com.speleo.start.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.TestDataGenerator
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeVM @Inject constructor(
    private val generator: TestDataGenerator,
    private val prefs: PreferencesManager,
    private val competitionRepo: CompetitionRepository
) : ViewModel() {

    private val _competitionName = MutableStateFlow("")
    val competitionName: StateFlow<String> = _competitionName.asStateFlow()

    init {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid != -1L) {
                val comp = competitionRepo.getCompetitionById(cid)
                _competitionName.value = comp?.shortName ?: ""
            }
        }
    }

    fun generateTestData() {
        viewModelScope.launch { generator.generate() }
    }

    fun clearTestData() {
        viewModelScope.launch { generator.clearAll() }
    }
}