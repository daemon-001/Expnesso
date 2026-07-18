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
    object BookDetails : Screen("book_details/{sessionId}") {
        fun createRoute(sessionId: String) = "book_details/$sessionId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var sharedDashboardViewModel: com.daemon.expnesso.ui.dashboard.DashboardViewModel? = null
    
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
            val context = androidx.compose.ui.platform.LocalContext.current
            val authRepository = androidx.compose.runtime.remember { com.daemon.expnesso.data.repository.AuthRepository(context) }
            val firestoreRepository = androidx.compose.runtime.remember { com.daemon.expnesso.data.repository.FirestoreRepository() }
            
            // Re-instantiate or reuse the viewModel if sessionId changes
            val viewModel = androidx.compose.runtime.remember(sessionId) { 
                com.daemon.expnesso.ui.dashboard.DashboardViewModel(authRepository, firestoreRepository, sessionId) 
            }
            sharedDashboardViewModel = viewModel
            com.daemon.expnesso.ui.dashboard.DashboardScreen(navController, viewModel)
        }
        composable("transactions") {
            sharedDashboardViewModel?.let { vm ->
                com.daemon.expnesso.ui.dashboard.TransactionsScreen(navController, vm)
            }
        }
        composable(Screen.BookDetails.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            sharedDashboardViewModel?.let { vm ->
                com.daemon.expnesso.ui.dashboard.BookDetailsScreen(navController, vm, sessionId)
            }
        }
    }
}
