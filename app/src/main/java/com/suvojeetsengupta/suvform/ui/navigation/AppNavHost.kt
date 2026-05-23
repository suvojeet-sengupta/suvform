package com.suvojeetsengupta.suvform.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.suvojeetsengupta.suvform.data.repository.AuthRepository
import com.suvojeetsengupta.suvform.data.repository.FirebaseAuthState
import com.suvojeetsengupta.suvform.ui.auth.SignInScreen
import com.suvojeetsengupta.suvform.ui.create.CreateScreen
import com.suvojeetsengupta.suvform.ui.editor.EditorScreen
import com.suvojeetsengupta.suvform.ui.preview.PreviewScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthStateViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {
    val authState = authRepository.authState
    val isAdmin = authRepository.isAdmin
}

@Composable
fun AppNavHost(initiallySignedIn: Boolean) {
    val nav = rememberNavController()
    val authVm: AuthStateViewModel = hiltViewModel()
    val authState by authVm.authState.collectAsStateWithLifecycle(
        initialValue = if (initiallySignedIn) FirebaseAuthState.SignedIn("") else FirebaseAuthState.SignedOut,
    )

    val startDestination = if (authState is FirebaseAuthState.SignedIn) Routes.Main else Routes.SignIn

    NavHost(
        navController = nav,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Routes.SignIn) {
            SignInScreen(onSignedIn = {
                nav.navigate(Routes.Main) { popUpTo(Routes.SignIn) { inclusive = true } }
            })
        }
        composable(Routes.Main) {
            val isAdmin by authVm.isAdmin.collectAsStateWithLifecycle(initialValue = false)
            MainScreen(
                isAdmin = isAdmin,
                onSignedOut = {
                    nav.navigate(Routes.SignIn) { popUpTo(Routes.Main) { inclusive = true } }
                },
                onCreateForm = { nav.navigate(Routes.Create) },
                onOpenForm = { nav.navigate(Routes.Editor) },
            )
        }
        composable(Routes.Create) {
            CreateScreen(
                onBack = { nav.popBackStack() },
                onOpenEditor = {
                    nav.navigate(Routes.Editor) {
                        popUpTo(Routes.Create) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Editor) {
            EditorScreen(
                onBack = { nav.popBackStack() },
                onPreview = { nav.navigate(Routes.Preview) },
                onSaved = {},
            )
        }
        composable(Routes.Preview) {
            PreviewScreen(onBack = { nav.popBackStack() })
        }
    }
}
