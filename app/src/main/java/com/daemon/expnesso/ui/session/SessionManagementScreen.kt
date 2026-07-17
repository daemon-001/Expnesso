package com.daemon.expnesso.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.daemon.expnesso.data.model.Session
import com.daemon.expnesso.data.repository.AuthRepository
import com.daemon.expnesso.data.repository.FirestoreRepository
import com.daemon.expnesso.navigation.Screen
import com.daemon.expnesso.ui.theme.PrimaryAccent
import com.daemon.expnesso.ui.theme.PremiumBackground
import com.daemon.expnesso.ui.theme.PremiumSurface
import com.daemon.expnesso.ui.theme.SecondaryAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionManagementScreen(navController: NavController) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val firestoreRepository = remember { FirestoreRepository() }
    val viewModel = remember { SessionViewModel(authRepository, firestoreRepository) }

    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PremiumBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryAccent,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Session")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Your Books", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PremiumBackground,
                    titleContentColor = Color.White
                ),
                actions = {
                    TextButton(onClick = { showJoinDialog = true }) {
                        Text("JOIN", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (sessions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No books yet.", color = Color.LightGray, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Create or join one to start.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sessions) { session ->
                        SessionCard(session) {
                            navController.navigate(Screen.Dashboard.createRoute(session.id))
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = SecondaryAccent
                )
            }
            
            if (error != null) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text(text = error ?: "An error occurred", color = Color.White)
                }
            }
        }
    }

    if (showCreateDialog) {
        var sessionName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Book") },
            text = {
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Book Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (sessionName.isNotBlank()) {
                        viewModel.createSession(sessionName) { sessionId ->
                            showCreateDialog = false
                            navController.navigate(Screen.Dashboard.createRoute(sessionId))
                        }
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            },
            containerColor = PremiumSurface
        )
    }

    if (showJoinDialog) {
        var inviteCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Book") },
            text = {
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it.uppercase() },
                    label = { Text("Invite Code") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (inviteCode.isNotBlank()) {
                        viewModel.joinSession(inviteCode) { sessionId ->
                            showJoinDialog = false
                            navController.navigate(Screen.Dashboard.createRoute(sessionId))
                        }
                    }
                }) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) { Text("Cancel") }
            },
            containerColor = PremiumSurface
        )
    }
}

@Composable
fun SessionCard(session: Session, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PremiumSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SecondaryAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(session.name.take(1).uppercase(), color = SecondaryAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${session.memberUids.size} members", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}
