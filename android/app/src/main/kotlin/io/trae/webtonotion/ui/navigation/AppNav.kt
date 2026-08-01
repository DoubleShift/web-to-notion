package io.trae.webtonotion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.trae.webtonotion.ui.screens.NoteEditScreen
import io.trae.webtonotion.ui.screens.NoteListScreen
import io.trae.webtonotion.ui.screens.SettingsScreen

@Composable
fun AppNav() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "notes"
    ) {
        composable("notes") {
            NoteListScreen(
                onNoteClick = { id -> navController.navigate("edit/$id") },
                onNewNote = { navController.navigate("edit/-1") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToGroups = { /* TODO: 分组页面 */ },
                onNavigateToTrash = { /* TODO: 回收站页面 */ }
            )
        }
        composable(
            route = "edit/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            NoteEditScreen(
                noteId = noteId,
                onSaved = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}
