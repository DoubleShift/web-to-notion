package io.trae.webtonotion.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.trae.webtonotion.ui.screens.NoteEditScreen
import io.trae.webtonotion.ui.screens.NoteListScreen
import io.trae.webtonotion.ui.screens.SettingsScreen

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // 编辑页面不显示底部导航
    val showBottomBar = currentRoute != null && !currentRoute.startsWith("edit")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "notes",
                        onClick = {
                            navController.navigate("notes") {
                                popUpTo("notes") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Outlined.Note, contentDescription = null) },
                        label = { Text("笔记") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo("notes") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        label = { Text("设置") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "notes",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("notes") {
                NoteListScreen(
                    onNoteClick = { id -> navController.navigate("edit/$id") },
                    onNewNote = { navController.navigate("edit/-1") },
                    onNavigateToSettings = { navController.navigate("settings") }
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
}

