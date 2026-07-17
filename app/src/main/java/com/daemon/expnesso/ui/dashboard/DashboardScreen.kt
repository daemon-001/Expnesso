package com.daemon.expnesso.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.daemon.expnesso.data.model.Transaction
import com.daemon.expnesso.data.repository.AuthRepository
import com.daemon.expnesso.data.repository.FirestoreRepository
import com.daemon.expnesso.ui.theme.PrimaryAccent
import com.daemon.expnesso.ui.theme.PremiumBackground
import com.daemon.expnesso.ui.theme.PremiumSurface
import com.daemon.expnesso.ui.theme.PremiumSurfaceVariant
import com.daemon.expnesso.ui.theme.ErrorRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, sessionId: String) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val firestoreRepository = remember { FirestoreRepository() }
    val viewModel = remember { DashboardViewModel(authRepository, firestoreRepository, sessionId) }

    val transactions by viewModel.transactions.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val session by viewModel.session.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PremiumBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryAccent,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(session?.name ?: "Book", fontWeight = FontWeight.Bold, color = Color.White)
                        if (session != null) {
                            Text("Code: ${session?.inviteCode}", fontSize = 12.sp, color = PrimaryAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PremiumBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Premium Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PremiumSurfaceVariant, PremiumSurface)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text("Total Spent", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$${String.format("%.2f", totalSpent)}",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Recent Transactions", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(transactions) { tx ->
                    TransactionItem(
                        transaction = tx,
                        currentUserId = viewModel.currentUserId,
                        adminUid = session?.adminUid,
                        onDelete = {
                            viewModel.deleteTransaction(tx.id, session?.adminUid ?: "", tx.addedByUid)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var amount by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Expense") },
            text = {
                Column {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && description.isNotBlank()) {
                        viewModel.addTransaction(amt, description) {
                            showAddDialog = false
                        }
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            },
            containerColor = PremiumSurface
        )
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    currentUserId: String?,
    adminUid: String?,
    onDelete: () -> Unit
) {
    val canDelete = currentUserId == adminUid || currentUserId == transaction.addedByUid
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                Text("$", color = PrimaryAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                Text(sdf.format(transaction.timestamp.toDate()), color = Color.Gray, fontSize = 12.sp)
            }
            Text(
                text = "$${String.format("%.2f", transaction.amount)}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                }
            }
        }
    }
}
