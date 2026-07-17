package com.daemon.expnesso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.daemon.expnesso.navigation.AppNavigation
import com.daemon.expnesso.ui.theme.ExpnessoTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContent {
            ExpnessoTheme(darkTheme = true) {
                AppNavigation()
            }
        }
    }
}