package com.daemon.expnesso.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SessionManagement : Screen("session_management")
    object Dashboard : Screen("dashboard/{sessionId}") {
        fun createRoute(sessionId: String) = "dashboard/$sessionId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    // Default to Login for now, we will handle auth state later
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            com.daemon.expnesso.ui.login.LoginScreen(navController)
        }
        composable(Screen.SessionManagement.route) {
            com.daemon.expnesso.ui.session.SessionManagementScreen(navController)
        }
        composable(Screen.Dashboard.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            com.daemon.expnesso.ui.dashboard.DashboardScreen(navController, sessionId)
        }
    }
}
