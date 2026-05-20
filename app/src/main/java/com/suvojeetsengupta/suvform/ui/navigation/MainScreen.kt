package com.suvojeetsengupta.suvform.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.suvojeetsengupta.suvform.ui.home.HomeScreen
import com.suvojeetsengupta.suvform.ui.responses.ResponsesScreen
import com.suvojeetsengupta.suvform.ui.settings.SettingsScreen

sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Home : BottomNavDestination(
        Routes.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home
    )
    data object Responses : BottomNavDestination(
        Routes.Responses, "Responses", Icons.Filled.Inbox, Icons.Outlined.Inbox
    )
    data object Settings : BottomNavDestination(
        Routes.Settings, "Profile", Icons.Filled.Person, Icons.Outlined.Person
    )
}

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold

@Composable
fun MainScreen(
    onSignedOut: () -> Unit,
    onCreateForm: () -> Unit,
    onOpenForm: () -> Unit,
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavDestination.Home,
        BottomNavDestination.Responses,
        BottomNavDestination.Settings,
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Disable default inset handling
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    onSignedOut = onSignedOut,
                    onCreateForm = onCreateForm,
                    onOpenForm = onOpenForm,
                    onViewResponses = {
                        navController.navigate(Routes.Responses) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Routes.Responses) {
                ResponsesScreen(
                    onBack = {
                        navController.navigate(Routes.Home) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(onSignedOut = onSignedOut)
            }
        }
    }
}
