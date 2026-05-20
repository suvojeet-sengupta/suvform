package com.suvojeetsengupta.suvform.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.suvojeetsengupta.suvform.data.repository.AuthRepository
import com.suvojeetsengupta.suvform.data.repository.FirebaseAuthState
import com.suvojeetsengupta.suvform.ui.auth.SignInScreen
import com.suvojeetsengupta.suvform.ui.create.CreateScreen
import com.suvojeetsengupta.suvform.ui.editor.EditorScreen
import com.suvojeetsengupta.suvform.ui.home.HomeScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel

@HiltViewModel
class AuthStateViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {
    val authState = authRepository.authState
}

@Composable
fun AppNavHost(initiallySignedIn: Boolean) {
    val nav = rememberNavController()
    val authVm: AuthStateViewModel = hiltViewModel()
    val authState by authVm.authState.collectAsStateWithLifecycle(
        initialValue = if (initiallySignedIn) FirebaseAuthState.SignedIn("") else FirebaseAuthState.SignedOut
    )

    val startDestination = if (authState is FirebaseAuthState.SignedIn) Routes.Home else Routes.SignIn

    NavHost(navController = nav, startDestination = startDestination) {
        composable(Routes.SignIn) {
            SignInScreen(onSignedIn = {
                nav.navigate(Routes.Home) { popUpTo(Routes.SignIn) { inclusive = true } }
            })
        }
        composable(Routes.Home) {
            HomeScreen(
                onSignedOut = {
                    nav.navigate(Routes.SignIn) { popUpTo(Routes.Home) { inclusive = true } }
                },
                onCreateForm = { nav.navigate(Routes.Create) },
            )
        }
        composable(Routes.Create) {
            CreateScreen(
                onBack = { nav.popBackStack() },
                onOpenEditor = {
                    nav.navigate(Routes.Editor) {
                        // Replace Create in the back stack so back from Editor goes Home.
                        popUpTo(Routes.Create) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Editor) {
            EditorScreen(onBack = { nav.popBackStack() })
        }
    }
}
