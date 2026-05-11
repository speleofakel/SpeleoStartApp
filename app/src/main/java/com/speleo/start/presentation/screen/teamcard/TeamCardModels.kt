package com.speleo.start.presentation.screen.teamcard

import androidx.compose.runtime.Immutable

// ============================================================
// СОСТОЯНИЕ ЭКРАНА
// ============================================================

@Immutable
data class TeamCardUiState(
    val mode: TeamCardMode = TeamCardMode.VIEW,
    val teamInfo: TeamInfo? = null,
    val members: List<MemberUi> = emptyList(),
    val routeCardEntries: List<RouteCardEntryUi> = emptyList(),
    val routeCardStats: RouteCardStats = RouteCardStats(),
    val replacedHistory: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val competitionStartTimestamp: Long = 0L,
    val isSecretarySigned: Boolean = false,
    val isJudgeSigned: Boolean = false
)

sealed class TeamCardMode {
    object VIEW : TeamCardMode()
    object EDIT : TeamCardMode()
    object MASTER_EDIT : TeamCardMode()
}

// ============================================================
// ИНФОРМАЦИЯ О КОМАНДЕ
// ============================================================

@Immutable
data class TeamInfo(
    val id: Long,
    val number: Int,
    val className: String,
    val status: String,
    val colorMark: String,
    val startTimestamp: Long?,
    val finishTimestamp: Long?,
    val checkpointsEntered: Boolean
)

// ============================================================
// УЧАСТНИКИ
// ============================================================

@Immutable
data class MemberUi(
    val participantId: Long,
    val personId: Long,
    val firstName: String,
    val lastName: String,
    val nickname: String?,
    val age: Int?,
    val phone: String?,
    val role: String,
    val mentorName: String?,
    val mentorConfirmed: Boolean,
    val judgeApproved: Boolean
)

// ============================================================
// ПУТЕВОЙ ЛИСТ
// ============================================================

@Immutable
data class RouteCardEntryUi(
    val checkpointId: Long,
    val displayNumber: Int,
    val weight: Int,
    val type: String,
    val taken: Boolean,
    val takenWithError: Boolean,
    val offsetTime: String,
    val penalty: Int,
    val secretaryConfirmed: Boolean,
    val judgeConfirmed: Boolean
)

@Immutable
data class RouteCardStats(
    val takenCount: Int = 0,
    val totalCount: Int = 0
)

// ============================================================
// ВСПОМОГАТЕЛЬНЫЕ DATA CLASS
// ============================================================

@Immutable
data class RelativeTimes(
    val startTime: String,
    val finishTime: String
)

// ============================================================
// СОБЫТИЯ (для Snackbar и навигации)
// ============================================================

sealed class TeamCardUiEvent {
    data class ShowMessage(val message: String) : TeamCardUiEvent()
    data class ShowFinishTimeDialog(val currentSeconds: Int) : TeamCardUiEvent()
    data class ShowMasterPasswordDialog(val onConfirm: () -> Unit) : TeamCardUiEvent()
    object NavigateBack : TeamCardUiEvent()
    object RouteCardLocked : TeamCardUiEvent()
}