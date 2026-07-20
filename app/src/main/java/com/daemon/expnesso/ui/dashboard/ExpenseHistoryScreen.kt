package com.daemon.expnesso.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.daemon.expnesso.data.model.Transaction
import com.daemon.expnesso.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseHistoryScreen(navController: NavController, viewModel: DashboardViewModel) {
    val allUserTransactions by viewModel.allUserTransactions.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val currentUserId = viewModel.currentUserId

    Scaffold(
        containerColor = PremiumBackground,
        topBar = {
            TopAppBar(
                title = { Text("Expense History", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PremiumBackground)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (allUserTransactions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No expenses found.", color = Color.LightGray, fontSize = 18.sp)
                }
            } else {
                val sdf = androidx.compose.runtime.remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allUserTransactions, key = { it.id }) { tx ->
                        val session = allSessions.find { it.id == tx.sessionId }
                        val sessionName = session?.name ?: "Unknown Book"
                        
                        val paidBy = tx.paidByUid.ifEmpty { tx.addedByUid }
                        val isPaidByMe = paidBy == currentUserId
                        
                        val myExpenseAmount = if (isPaidByMe) {
                            if (tx.splits.isNotEmpty()) tx.splits[currentUserId] ?: 0.0 else tx.amount
                        } else {
                            if (tx.splits.isNotEmpty()) tx.splits[currentUserId] ?: 0.0 else 0.0
                        }

                        val formattedDate = sdf.format(tx.timestamp.toDate())

                        Card(
                            colors = CardDefaults.cardColors(containerColor = PremiumSurface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tx.description,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(myExpenseAmount)}",
                                        color = if (myExpenseAmount > 0) ErrorRed else Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formattedDate,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Book: $sessionName",
                                        color = PrimaryAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
