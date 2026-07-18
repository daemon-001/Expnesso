package com.daemon.expnesso.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.daemon.expnesso.data.model.Transaction
import com.daemon.expnesso.data.model.User
import com.daemon.expnesso.ui.theme.ErrorRed
import com.daemon.expnesso.ui.theme.PremiumBackground
import com.daemon.expnesso.ui.theme.PremiumSurface
import com.daemon.expnesso.ui.theme.PremiumSurfaceVariant
import com.daemon.expnesso.ui.theme.TextPrimary
import com.daemon.expnesso.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(navController: NavController, viewModel: DashboardViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val sessionMembers by viewModel.sessionMembers.collectAsState()
    val session by viewModel.session.collectAsState()
    
    Scaffold(
        containerColor = PremiumBackground,
        topBar = {
            TopAppBar(
                title = { Text("Transactions", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PremiumBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(transactions) { tx ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(500)),
                        exit = fadeOut(animationSpec = tween(500))
                    ) {
                        TransactionItem(
                            transaction = tx,
                            paidBy = sessionMembers[tx.paidByUid.ifEmpty { tx.addedByUid }],
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
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    paidBy: User?,
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
            AsyncImage(
                model = paidBy?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=${paidBy.name}" } ?: "https://ui-avatars.com/api/?name=?",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PremiumSurfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                Text("${sdf.format(transaction.timestamp.toDate())} • Paid by ${paidBy?.name ?: "Unknown"}", color = TextSecondary, fontSize = 12.sp)
            }
            Text(
                text = "₹${String.format("%.2f", transaction.amount)}",
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
