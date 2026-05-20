package com.suvojeetsengupta.suvform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.suvojeetsengupta.suvform.ui.navigation.AppNavHost
import com.suvojeetsengupta.suvform.ui.theme.SuvFormTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initiallySignedIn = auth.currentUser != null
        setContent {
            SuvFormTheme {
                AppNavHost(initiallySignedIn = initiallySignedIn)
            }
        }
    }
}
