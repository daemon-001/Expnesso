package com.daemon.expnesso.ui.login

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.daemon.expnesso.R
import com.daemon.expnesso.data.repository.AuthRepository
import com.daemon.expnesso.navigation.Screen
import com.daemon.expnesso.ui.theme.PrimaryAccent
import com.daemon.expnesso.ui.theme.PremiumBackground
import com.daemon.expnesso.ui.theme.SecondaryAccent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val currentUser = authRepository.currentUser
        if (currentUser != null) {
            val firestoreRepository = com.daemon.expnesso.data.repository.FirestoreRepository()
            // Make sure the user document exists!
            firestoreRepository.saveUser(currentUser)
            
            val userDoc = firestoreRepository.getUser(currentUser.uid)
            var targetSessionId = userDoc?.defaultSessionId
            if (targetSessionId.isNullOrEmpty()) {
                targetSessionId = ""
            }
            navController.navigate(Screen.Dashboard.createRoute(targetSessionId)) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                scope.launch {
                    try {
                        isLoading = true
                        val authResult = authRepository.firebaseAuth.signInWithCredential(credential).await()
                        authResult.user?.let { firebaseUser ->
                            val firestoreRepository = com.daemon.expnesso.data.repository.FirestoreRepository()
                            firestoreRepository.saveUser(firebaseUser)
                            val userDoc = firestoreRepository.getUser(firebaseUser.uid)
                            var targetSessionId = userDoc?.defaultSessionId
                            if (targetSessionId.isNullOrEmpty()) {
                                targetSessionId = ""
                            }
                            navController.navigate(Screen.Dashboard.createRoute(targetSessionId)) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } ?: run {
                            throw Exception("Failed to get Firebase User")
                        }
                    } catch (e: Exception) {
                        errorMessage = e.localizedMessage
                        isLoading = false
                    }
                }
            } catch (e: ApiException) {
                Log.w("LoginScreen", "Google sign in failed", e)
                errorMessage = "Google Sign In failed: ${e.statusCode}"
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PremiumBackground, Color(0xFF1E1E2F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Expnesso",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryAccent,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Split expenses. Sync beautifully.",
                fontSize = 16.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 64.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(color = SecondaryAccent)
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        val intent = authRepository.getGoogleSignInClient().signInIntent
                        launcher.launch(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAccent,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Google", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
