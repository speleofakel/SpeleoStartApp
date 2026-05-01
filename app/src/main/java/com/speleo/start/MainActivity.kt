package com.speleo.start

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.speleo.start.presentation.Screen
import com.speleo.start.presentation.SharedState
import com.speleo.start.presentation.screen.competitions.CompetitionListScreen
import com.speleo.start.presentation.screen.competitions.CompetitionNewScreen
import com.speleo.start.presentation.screen.competitions.CompetitionSettingsScreen
import com.speleo.start.presentation.screen.finish.FinishScreen
import com.speleo.start.presentation.screen.home.HomeScreen
import com.speleo.start.presentation.screen.persons.PersonListScreen
import com.speleo.start.presentation.screen.results.ResultsScreen
import com.speleo.start.presentation.screen.routecard.RouteCardScreen
import com.speleo.start.presentation.screen.start.StartScreen
import com.speleo.start.presentation.screen.team.TeamRegisterScreen
import com.speleo.start.presentation.screen.teamcard.TeamCardScreen
import com.speleo.start.presentation.screen.teamlist.TeamListScreen
import com.speleo.start.ui.theme.SpeleoStartTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.speleo.start.presentation.screen.persons.PersonDetailScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var sharedState: SharedState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeleoStartTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val teamId by sharedState.selectedTeamId.collectAsStateWithLifecycle()

                    NavHost(navController = navController, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) {
                            HomeScreen(onNavigate = { screen -> navController.navigate(screen.route) })
                        }
                        composable(Screen.PersonDetail.route) { backStackEntry ->
                            val personId = backStackEntry.arguments?.getString("personId")?.toLongOrNull() ?: -1L
                            PersonDetailScreen(
                                personId = personId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Competitions.route) {
                            CompetitionListScreen(
                                onBack = { navController.popBackStack() },
                                onCreateNew = { navController.navigate(Screen.CreateCompetition.route) },
                                onSettings = { cid -> navController.navigate(Screen.CompetitionSettings.pass(cid)) }
                            )
                        }
                        composable(Screen.CreateCompetition.route) {
                            CompetitionNewScreen(
                                onBack = { navController.popBackStack() },
                                onSaved = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.CompetitionSettings.route,
                            arguments = listOf(navArgument("competitionId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val competitionId = backStackEntry.arguments?.getLong("competitionId") ?: return@composable
                            CompetitionSettingsScreen(
                                competitionId = competitionId,
                                onBack = { navController.popBackStack() },
                                onCheckpoints = { cid -> navController.navigate(Screen.Checkpoints.pass(cid)) }
                            )
                        }
                        composable(
                            route = Screen.Checkpoints.route,
                            arguments = listOf(navArgument("competitionId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val competitionId = backStackEntry.arguments?.getLong("competitionId") ?: return@composable
                            com.speleo.start.presentation.screen.checkpoints.CheckpointListScreen(
                                competitionId = competitionId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Persons.route) {
                            PersonListScreen(
                                onBack = { navController.popBackStack() },
                                onAddNew = { navController.navigate(Screen.PersonNew.route) },
                                onPersonClick = { personId ->
                                    navController.navigate(Screen.PersonDetail.createRoute(personId))
                                }
                            )
                        }
                        composable(Screen.PersonNew.route) {
                            com.speleo.start.presentation.screen.persons.PersonNewScreen(
                                onBack = { navController.popBackStack() },
                                onSaved = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Register.route) {
                            TeamRegisterScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToTeam = { teamId ->
                                    navController.navigate(Screen.TeamCard.pass(teamId)) {
                                        popUpTo(Screen.Register.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Start.route) {
                            StartScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.Finish.route) {
                            FinishScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.TeamList.route) {
                            TeamListScreen(
                                onBack = { navController.popBackStack() },
                                onTeamClick = { tid -> navController.navigate(Screen.TeamCard.pass(tid)) }
                            )
                        }
                        composable(Screen.Results.route) {
                            ResultsScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = Screen.RouteCardDetail.route,
                            arguments = listOf(navArgument("teamId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val teamId = backStackEntry.arguments?.getLong("teamId") ?: return@composable
                            RouteCardScreen(
                                teamId = teamId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Settings.route) {
                            com.speleo.start.presentation.screen.settings.SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Screen.TeamCard.route,
                            arguments = listOf(navArgument("teamId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val teamId = backStackEntry.arguments?.getLong("teamId") ?: return@composable
                            TeamCardScreen(
                                teamId = teamId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}