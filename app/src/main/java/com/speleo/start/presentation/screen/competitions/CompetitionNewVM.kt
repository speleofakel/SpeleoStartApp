package com.speleo.start.presentation.screen.competitions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompetitionNewVM @Inject constructor(
    private val repository: CompetitionRepository
) : ViewModel() {

    fun saveCompetition(
        name: String, shortName: String, date: String, place: String,
        discipline: String, system: String?, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createCompetition(name, shortName, date, place, discipline, system)
            onSuccess()
        }
    }
}