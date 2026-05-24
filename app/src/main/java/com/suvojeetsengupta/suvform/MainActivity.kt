package com.suvojeetsengupta.suvform

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.suvojeetsengupta.suvform.data.prefs.ThemePreferenceStore
import com.suvojeetsengupta.suvform.ui.navigation.AppNavHost
import com.suvojeetsengupta.suvform.ui.theme.SuvFormTheme
import com.suvojeetsengupta.suvform.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var themeStore: ThemePreferenceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initiallySignedIn = auth.currentUser != null
        setContent {
            val mode by themeStore.mode.collectAsState()
            val darkTheme = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SuvFormTheme(darkTheme = darkTheme) {
                AppNavHost(initiallySignedIn = initiallySignedIn)
            }
        }
    }
}
