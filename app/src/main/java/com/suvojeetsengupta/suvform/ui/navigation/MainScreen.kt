package com.suvojeetsengupta.suvform.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.suvojeetsengupta.suvform.ui.home.HomeScreen
import com.suvojeetsengupta.suvform.ui.responses.ResponseDetailScreen
import com.suvojeetsengupta.suvform.ui.responses.ResponsesScreen
import com.suvojeetsengupta.suvform.ui.responses.ResponsesViewModel
import com.suvojeetsengupta.suvform.ui.settings.SettingsScreen

sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Home : BottomNavDestination(
        "home_tab", "Home", Icons.Filled.Home, Icons.Outlined.Home
    )
    data object Responses : BottomNavDestination(
        "responses_flow", "Responses", Icons.Filled.Inbox, Icons.Outlined.Inbox
    )
    data object Settings : BottomNavDestination(
        Routes.Settings, "Settings", Icons.Filled.Person, Icons.Outlined.Person
    )
}

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
            startDestination = "home_tab",
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            composable("home_tab") {
                HomeScreen(
                    onSignedOut = onSignedOut,
                    onCreateForm = onCreateForm,
                    onOpenForm = onOpenForm,
                    onViewResponses = {
                        navController.navigate("responses_flow") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            
            navigation(startDestination = Routes.Responses, route = "responses_flow") {
                composable(Routes.Responses) {
                    val parentEntry = remember(it) {
                        runCatching { navController.getBackStackEntry("responses_flow") }.getOrNull()
                    }
                    if (parentEntry is NavBackStackEntry) {
                        val viewModel: ResponsesViewModel = hiltViewModel(parentEntry)
                        ResponsesScreen(
                            onBack = {
                                navController.navigate("home_tab") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onViewDetail = {
                                navController.navigate(Routes.ResponseDetail)
                            },
                            viewModel = viewModel
                        )
                    }
                }
                composable(Routes.ResponseDetail) {
                    val parentEntry = remember(it) {
                        runCatching { navController.getBackStackEntry("responses_flow") }.getOrNull()
                    }
                    if (parentEntry is NavBackStackEntry) {
                        val responsesVm: ResponsesViewModel = hiltViewModel(parentEntry)
                        val state by responsesVm.state.collectAsStateWithLifecycle()
                        val resp = state.selectedResponse
                        if (resp != null) {
                            ResponseDetailScreen(
                                response = resp,
                                fields = state.fields,
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            }

            composable(Routes.Settings) {
                SettingsScreen(onSignedOut = onSignedOut)
            }
        }
    }
}
