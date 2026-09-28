package com.daemon.expnesso.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daemon.expnesso.data.model.Session
import com.daemon.expnesso.data.model.User
import com.daemon.expnesso.ui.theme.*

@Composable
fun BooksTabContent(
    allSessions: List<Session>,
    currentSessionId: String,
    allKnownUsers: Map<String, User>,
    onSessionClick: (Session) -> Unit,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumBackground)
            .padding(padding)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Your Books",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        if (allSessions.isEmpty()) {
            Text(
                text = "You haven't joined any books yet.",
                color = TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(allSessions, key = { it.id }) { session ->
                    val isCurrent = session.id == currentSessionId
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCurrent) PremiumSurfaceVariant else PremiumSurface)
                            .clickable { onSessionClick(session) }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = session.name,
                                    color = if (isCurrent) PrimaryAccent else Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Code: ${session.inviteCode}",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (isCurrent) {
                                Text(
                                    text = "Current",
                                    color = PrimaryAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(PrimaryAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.05f))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "MEMBERS",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        val members = session.memberUids.mapNotNull { allKnownUsers[it] }
                        if (members.isEmpty()) {
                            Text("Loading members...", color = TextSecondary, fontSize = 14.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                members.forEach { user ->
                                    val isAdmin = user.uid == session.adminUid
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        coil.compose.AsyncImage(
                                            model = user.photoUrl.ifEmpty { "https://ui-avatars.com/api/?name=${user.name}" },
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(PremiumSurfaceVariant),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.name,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (isAdmin) {
                                            Text(
                                                text = "Admin",
                                                color = PrimaryAccent,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .background(PrimaryAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
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
    }
}

@Composable
fun SplitStatsTabContent(
    allSessions: List<Session>,
    allKnownUsers: Map<String, User>,
    padding: PaddingValues,
    viewModel: DashboardViewModel,
    onSessionClick: (Session) -> Unit
) {
    val sessionDebts by viewModel.sessionDebts.collectAsState()
    val sessionMemberSpending by viewModel.sessionMemberSpending.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumBackground)
            .padding(padding)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Split Stats",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        if (allSessions.isEmpty()) {
            Text(
                text = "You haven't joined any books yet.",
                color = TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(allSessions, key = { "stats_${it.id}" }) { session ->
                    val debts = sessionDebts[session.id] ?: emptyList()
                    val spendingMap = sessionMemberSpending[session.id] ?: emptyMap()
                    val currentUserId = viewModel.currentUserId
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PremiumSurfaceVariant)
                            .clickable { onSessionClick(session) }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = session.name,
                            color = PrimaryAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        if (session.memberUids.isNotEmpty()) {
                            Text(
                                text = "Who spent how much",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                session.memberUids.forEach { uid ->
                                    val user = allKnownUsers[uid]
                                    val spent = spendingMap[uid] ?: 0.0
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Avatar
                                        coil.compose.AsyncImage(
                                            model = user?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=${user.name}" } ?: "https://ui-avatars.com/api/?name=?",
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(PremiumSurfaceVariant),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        // Name, Amount and Bar
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = user?.name ?: "Unknown",
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(spent)}",
                                                    color = PrimaryAccent,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Text(
                            text = "Who owes whom",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (debts.isEmpty()) {
                            Text(
                                text = "All settled up in this book!",
                                color = SuccessGreen,
                                fontSize = 14.sp
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                debts.forEach { debt ->
                                    val debtorUser = allKnownUsers[debt.fromUid]
                                    val creditorUser = allKnownUsers[debt.toUid]
                                    val debtorName = debtorUser?.name?.split(" ")?.firstOrNull() ?: "Unknown"
                                    val creditorName = creditorUser?.name?.split(" ")?.firstOrNull() ?: "Unknown"
                                    
                                    val isDebtorMe = debt.fromUid == currentUserId
                                    val isCreditorMe = debt.toUid == currentUserId
                                    
                                    val amountColor = if (isCreditorMe) SuccessGreen else if (isDebtorMe) ErrorRed else TextPrimary
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PremiumSurface)
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Debtor
                                            coil.compose.AsyncImage(
                                                model = debtorUser?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=${debtorName}" } ?: "https://ui-avatars.com/api/?name=?",
                                                contentDescription = "Avatar",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(PremiumSurfaceVariant),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isDebtorMe) "You" else debtorName,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 80.dp)
                                            )
                                            
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "owe",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                            androidx.compose.material3.Icon(
                                                Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "owes",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(12.dp).padding(horizontal = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Creditor
                                            coil.compose.AsyncImage(
                                                model = creditorUser?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=${creditorName}" } ?: "https://ui-avatars.com/api/?name=?",
                                                contentDescription = "Avatar",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(PremiumSurfaceVariant),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isCreditorMe) "You" else creditorName,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 80.dp)
                                            )
                                        }
                                        Text(
                                            text = "₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(debt.amount)}",
                                            color = amountColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
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
}
