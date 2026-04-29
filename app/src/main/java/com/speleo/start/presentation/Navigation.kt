package com.speleo.start.presentation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Competitions : Screen("competitions")
    object RouteCardDetail : Screen("route_card_detail/{teamId}") {
        fun pass(id: Long) = "route_card_detail/$id"
    }
    object PersonDetail : Screen("person_detail/{personId}") {
        fun createRoute(personId: Long) = "person_detail/$personId"
    }
    object Start : Screen("start")
    object Finish : Screen("finish")
    object Register : Screen("register")
    object TeamList : Screen("team_list")
    object Persons : Screen("persons")
    object Results : Screen("results")
    object Settings : Screen("settings")
    object RouteCard : Screen("route_card")
    object CreateCompetition : Screen("create_competition")
    object Checkpoints : Screen("checkpoints/{competitionId}") {
        fun pass(id: Long) = "checkpoints/$id"
    }
    object PersonNew : Screen("person_new")
    object CompetitionSettings : Screen("competition_settings/{competitionId}") {
        fun pass(id: Long) = "competition_settings/$id"
    }
    object TeamCard : Screen("team_card/{teamId}") {
        fun pass(id: Long) = "team_card/$id"
    }
}
