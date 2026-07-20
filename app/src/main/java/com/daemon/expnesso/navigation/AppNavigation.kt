package com.daemon.expnesso.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SessionManagement : Screen("session_management")
    object Dashboard : Screen("dashboard/{sessionId}") {
        fun createRoute(sessionId: String) = "dashboard/$sessionId"
    }
    object BookDetails : Screen("book_details/{sessionId}") {
        fun createRoute(sessionId: String) = "book_details/$sessionId"
    }
    object ExpenseHistory : Screen("expense_history")
    object AddExpense : Screen("add_expense")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var sharedDashboardViewModel: com.daemon.expnesso.ui.dashboard.DashboardViewModel? = null
    
    val startDestination = if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
        Screen.Dashboard.createRoute("")
    } else {
        Screen.Login.route
    }
    
    NavHost(
        navController = navController, 
        startDestination = startDestination,
        modifier = Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background),
        enterTransition = {
            androidx.compose.animation.slideInHorizontally(
                animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                initialOffsetX = { it }
            ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400))
        },
        exitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                targetOffsetX = { -it / 3 }
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400))
        },
        popEnterTransition = {
            androidx.compose.animation.slideInHorizontally(
                animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                initialOffsetX = { -it / 3 }
            ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400))
        },
        popExitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                targetOffsetX = { it }
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400))
        }
    ) {
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
            
            val viewModel: com.daemon.expnesso.ui.dashboard.DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return com.daemon.expnesso.ui.dashboard.DashboardViewModel(authRepository, firestoreRepository, sessionId) as T
                    }
                }
            )
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
        composable(Screen.ExpenseHistory.route) {
            sharedDashboardViewModel?.let { vm ->
                com.daemon.expnesso.ui.dashboard.ExpenseHistoryScreen(navController, vm)
            }
        }
        composable(Screen.AddExpense.route) {
            sharedDashboardViewModel?.let { vm ->
                com.daemon.expnesso.ui.dashboard.AddExpenseScreen(navController, vm)
            }
        }
    }
}
