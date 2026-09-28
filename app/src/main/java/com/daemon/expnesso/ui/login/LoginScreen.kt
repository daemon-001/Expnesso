package com.daemon.expnesso.ui.login

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.daemon.expnesso.R
import com.daemon.expnesso.data.repository.AuthRepository
import com.daemon.expnesso.data.repository.FirestoreRepository
import com.daemon.expnesso.navigation.Screen
import com.daemon.expnesso.ui.theme.PrimaryAccent
import com.daemon.expnesso.ui.theme.PremiumBackground
import com.daemon.expnesso.ui.theme.SecondaryAccent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val firestoreRepository = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    var showGoogleNameDialog by remember { mutableStateOf(false) }
    var googleName by remember { mutableStateOf("") }
    var pendingGoogleUser by remember { mutableStateOf<FirebaseUser?>(null) }

    LaunchedEffect(Unit) {
        val currentUser = authRepository.currentUser
        if (currentUser != null) {
            val userDoc = firestoreRepository.getUser(currentUser.uid)
            if (userDoc == null) {
                firestoreRepository.saveUser(currentUser)
            }
            val finalUserDoc = firestoreRepository.getUser(currentUser.uid)
            var targetSessionId = finalUserDoc?.defaultSessionId
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
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            scope.launch {
                try {
                    isLoading = true
                    val authResult = authRepository.firebaseAuth.signInWithCredential(credential).await()
                    authResult.user?.let { firebaseUser ->
                        val userDoc = firestoreRepository.getUser(firebaseUser.uid)
                        if (userDoc == null) {
                            pendingGoogleUser = firebaseUser
                            googleName = firebaseUser.displayName ?: ""
                            showGoogleNameDialog = true
                            isLoading = false
                        } else {
                            var targetSessionId = userDoc.defaultSessionId
                            if (targetSessionId.isNullOrEmpty()) {
                                targetSessionId = ""
                            }
                            navController.navigate(Screen.Dashboard.createRoute(targetSessionId)) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    } ?: run {
                        throw Exception("Failed to get Firebase User")
                    }
                } catch (e: Exception) {
                    errorMessage = e.localizedMessage
                    isLoading = false
                }
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            android.util.Log.w("LoginScreen", "Google sign in failed", e)
            errorMessage = "Google Sign In failed: ${e.statusCode} (${com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)})"
            isLoading = false
        }
    }

    if (showGoogleNameDialog && pendingGoogleUser != null) {
        AlertDialog(
            onDismissRequest = { /* Require name */ },
            title = { Text("Welcome to Expnesso!") },
            text = {
                Column {
                    Text("Please enter your name to continue:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = googleName,
                        onValueChange = { googleName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (googleName.isNotBlank()) {
                            scope.launch {
                                isLoading = true
                                showGoogleNameDialog = false
                                try {
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(googleName)
                                        .build()
                                    pendingGoogleUser!!.updateProfile(profileUpdates).await()
                                    firestoreRepository.saveUser(pendingGoogleUser!!, googleName)
                                    
                                    val userDoc = firestoreRepository.getUser(pendingGoogleUser!!.uid)
                                    var targetSessionId = userDoc?.defaultSessionId
                                    if (targetSessionId.isNullOrEmpty()) {
                                        targetSessionId = ""
                                    }
                                    navController.navigate(Screen.Dashboard.createRoute(targetSessionId)) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage
                                    isLoading = false
                                }
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        )
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
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
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
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(color = SecondaryAccent)
            } else {
                if (!isLoginMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = Color.Gray,
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color.LightGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = Color.Gray,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.LightGray) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = Color.Gray,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank() || (!isLoginMode && name.isBlank())) {
                            errorMessage = "Please fill all fields"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                if (isLoginMode) {
                                    val authResult = authRepository.firebaseAuth.signInWithEmailAndPassword(email, password).await()
                                    val user = authResult.user
                                    if (user != null) {
                                        val userDoc = firestoreRepository.getUser(user.uid)
                                        var targetSessionId = userDoc?.defaultSessionId
                                        if (targetSessionId.isNullOrEmpty()) {
                                            targetSessionId = ""
                                        }
                                        navController.navigate(Screen.Dashboard.createRoute(targetSessionId)) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                } else {
                                    val authResult = authRepository.firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                                    val user = authResult.user
                                    if (user != null) {
                                        val profileUpdates = UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .build()
                                        user.updateProfile(profileUpdates).await()
                                        firestoreRepository.saveUser(user, name)
                                        
                                        val userDoc = firestoreRepository.getUser(user.uid)
                                        var targetSessionId = userDoc?.defaultSessionId
                                        if (targetSessionId.isNullOrEmpty()) {
                                            targetSessionId = ""
                                        }
                                        navController.navigate(Screen.Dashboard.createRoute(targetSessionId)) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAccent,
                        contentColor = Color.Black
                    )
                ) {
                    Text(if (isLoginMode) "Login" else "Sign Up", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                TextButton(onClick = { 
                    isLoginMode = !isLoginMode 
                    errorMessage = null
                }) {
                    Text(
                        text = if (isLoginMode) "Don't have an account? Sign up" else "Already have an account? Login",
                        color = PrimaryAccent
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
                    Text(" OR ", color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        val intent = authRepository.getGoogleSignInClient().signInIntent
                        launcher.launch(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
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
                    Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
