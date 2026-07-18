package com.daemon.expnesso.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.daemon.expnesso.R
import com.daemon.expnesso.data.model.User
import com.daemon.expnesso.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    navController: NavController,
    viewModel: DashboardViewModel,
    sessionId: String
) {
    val session by viewModel.session.collectAsState()
    val sessionMembers by viewModel.sessionMembers.collectAsState()
    val netBalances by viewModel.netBalances.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val totalSpent = transactions.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PremiumBackground)
            )
        },
        containerColor = PremiumBackground
    ) { padding ->
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Premium Summary Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(colors = listOf(PremiumSurfaceVariant, PremiumSurface)))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = session!!.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 26.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Invite Code: ${session!!.inviteCode}",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        Text(
                            text = "Created: ${sdf.format(session!!.createdAt.toDate())}",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Total Book Expenses", color = TextSecondary, fontSize = 14.sp)
                        Text(
                            text = "₹${String.format("%.2f", totalSpent)}",
                            color = PrimaryAccent,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (transactions.isEmpty() || totalSpent == 0.0) {
                item {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.no_history))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No expenses added yet",
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Member Balances",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom Bar Chart for balances
                    if (netBalances.isNotEmpty()) {
                        BalanceBarChart(netBalances = netBalances, members = sessionMembers)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            item {
                Text(
                    text = "Members (${sessionMembers.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(sessionMembers.values.toList()) { member ->
                MemberBalanceCard(
                    member = member,
                    balance = netBalances[member.uid] ?: 0.0,
                    isAdmin = session!!.adminUid == member.uid
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun BalanceBarChart(netBalances: Map<String, Double>, members: Map<String, User>) {
    // Only show people with non-zero balances for the chart, or show all? Let's show top 5 for simplicity or all if small
    val chartData = netBalances.toList().sortedByDescending { it.second }
    val maxAbsBalance = chartData.maxOfOrNull { Math.abs(it.second) }?.toFloat() ?: 1f

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(chartData) {
        animationProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = Modifier.fillMaxWidth().height((chartData.size * 60).dp)) {
        val width = size.width
        val barHeight = 24.dp.toPx()
        val spacing = 36.dp.toPx()
        val centerLine = width / 2f
        val maxBarWidth = (width / 2f) - 30.dp.toPx()

        chartData.forEachIndexed { index, (uid, balance) ->
            val yOffset = index * spacing + spacing / 2
            val member = members[uid]
            val name = member?.name?.split(" ")?.firstOrNull() ?: "Unknown"

            // Calculate bar dimensions
            val normalizedAbsBalance = (Math.abs(balance).toFloat() / maxAbsBalance) * maxBarWidth * animationProgress.value
            val isPositive = balance >= 0
            
            val barColor = if (isPositive) SuccessGreen else ErrorRed
            val barStartX = if (isPositive) centerLine else centerLine - normalizedAbsBalance
            val barWidth = normalizedAbsBalance

            drawRoundRect(
                color = barColor,
                topLeft = Offset(barStartX, yOffset - barHeight / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Draw text
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 12.sp.toPx()
                textAlign = if (isPositive) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.LEFT
                isAntiAlias = true
            }

            val textX = if (isPositive) centerLine - 10.dp.toPx() else centerLine + 10.dp.toPx()
            
            drawContext.canvas.nativeCanvas.drawText(
                "$name: ₹${String.format("%.0f", balance)}",
                textX,
                yOffset + (paint.textSize / 3), // vertical centering approximation
                paint
            )
        }
        
        // Draw center zero line
        drawLine(
            color = TextSecondary.copy(alpha = 0.5f),
            start = Offset(centerLine, 0f),
            end = Offset(centerLine, size.height),
            strokeWidth = 2f
        )
    }
}

@Composable
fun MemberBalanceCard(member: User, balance: Double, isAdmin: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = member.photoUrl.ifEmpty { "https://ui-avatars.com/api/?name=${member.name}" },
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(PremiumSurfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(member.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = PrimaryAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ADMIN",
                                color = PrimaryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(member.email, color = TextSecondary, fontSize = 12.sp)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (balance > 0) {
                    Text("Gets Back", color = SuccessGreen, fontSize = 12.sp)
                    Text("+₹${String.format("%.2f", balance)}", color = SuccessGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else if (balance < 0) {
                    Text("Owes", color = ErrorRed, fontSize = 12.sp)
                    Text("-₹${String.format("%.2f", Math.abs(balance))}", color = ErrorRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Settled", color = TextSecondary, fontSize = 12.sp)
                    Text("₹0.00", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
