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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import com.daemon.expnesso.utils.QRCodeUtils
import com.daemon.expnesso.ui.utils.AutoSizeText
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
    val sessionUserExpenses by viewModel.sessionUserExpenses.collectAsState()
    val allDebts by viewModel.allDebts.collectAsState()
    val myExpense = sessionUserExpenses[sessionId] ?: 0.0
    var showInviteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

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

        @OptIn(ExperimentalMaterial3Api::class)
        val pullRefreshState = rememberPullToRefreshState()
        var isRefreshing by remember { mutableStateOf(false) }
        
        LaunchedEffect(session) {
            isRefreshing = false
        }

        @OptIn(ExperimentalMaterial3Api::class)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { 
                isRefreshing = true
                viewModel.switchSession(sessionId) 
            },
            state = pullRefreshState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = session!!.name,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 26.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (session!!.adminUid == viewModel.currentUserId) {
                                IconButton(
                                    onClick = { 
                                        showDeleteConfirmDialog = true
                                    },
                                    modifier = Modifier
                                        .background(ErrorRed.copy(alpha = 0.2f), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Book", tint = ErrorRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        Text(
                            text = "Created ${sdf.format(session!!.createdAt.toDate())}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            color = PremiumSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Invite Code", session!!.inviteCode)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Code copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Invite Code: ${session!!.inviteCode}",
                                    color = PrimaryAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Code",
                                    tint = PrimaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Total Book Expenses", color = TextSecondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            AutoSizeText(
                                text = "₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(totalSpent)}",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AutoSizeText(
                                text = buildAnnotatedString {
                                    append("Your Expense: ")
                                    withStyle(style = SpanStyle(color = Color.White)) {
                                        append("₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(myExpense)}")
                                    }
                                },
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showInviteDialog = true },
                                modifier = Modifier
                                    .background(PremiumSurfaceVariant, CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = "Add Member", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { navController.navigate("transactions") },
                                modifier = Modifier
                                    .background(PremiumSurfaceVariant, CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Filled.List, contentDescription = "Transactions", tint = Color.White)
                            }
                            IconButton(
                                onClick = { navController.navigate("activity_log/${session!!.id}") },
                                modifier = Modifier
                                    .background(PremiumSurfaceVariant, CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Filled.History, contentDescription = "Activity Log", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { navController.navigate(com.daemon.expnesso.navigation.Screen.AddExpense.route) },
                                modifier = Modifier
                                    .background(PrimaryAccent, CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Expense", tint = Color.White)
                            }
                        }
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
                    if (allDebts.isNotEmpty()) {
                        Text(
                            text = "Who Owes Whom",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allDebts.forEach { debt ->
                                val debtorUser = sessionMembers[debt.fromUid]
                                val creditorUser = sessionMembers[debt.toUid]
                                val debtorName = debtorUser?.name?.split(" ")?.firstOrNull() ?: "Unknown"
                                val creditorName = creditorUser?.name?.split(" ")?.firstOrNull() ?: "Unknown"
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
                                        AsyncImage(
                                            model = debtorUser?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=${debtorName}" } ?: "https://ui-avatars.com/api/?name=?",
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(PremiumSurfaceVariant),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = debtorName,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 80.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "owes",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "owes",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(12.dp).padding(horizontal = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Creditor
                                        AsyncImage(
                                            model = creditorUser?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=${creditorName}" } ?: "https://ui-avatars.com/api/?name=?",
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(PremiumSurfaceVariant),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = creditorName,
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
                                        color = ErrorRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    Text(
                        text = "Member Balances",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Improved Bar Chart for balances
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
        } // End PullToRefreshBox

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Book", color = ErrorRed, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "Are you sure you want to delete this book? This will move it to the bin, and you can restore it within 30 days.",
                        color = TextPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteSession(
                                sessionId = session!!.id, 
                                adminUid = session!!.adminUid,
                                onSuccess = {
                                    navController.navigate("session_management") {
                                        popUpTo(0)
                                    }
                                },
                                onError = { err -> 
                                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = PremiumSurface
            )
        }

        if (showInviteDialog) {
            var isAddingManual by remember { mutableStateOf(false) }
            var manualName by remember { mutableStateOf("") }
            var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
            
            LaunchedEffect(session?.inviteCode) {
                session?.inviteCode?.let { code ->
                    qrBitmap = QRCodeUtils.generateQRCode(code)
                }
            }

            AlertDialog(
                onDismissRequest = { showInviteDialog = false },
                title = {
                    Text(
                        text = "Invite to ${session!!.name}",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Share this code or scan the QR", color = TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(12.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier.size(150.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PremiumSurfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = session!!.inviteCode,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryAccent,
                                letterSpacing = 4.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = PremiumSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Or add offline member", color = TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            placeholder = { Text("Enter member name", color = TextSecondary.copy(alpha = 0.5f)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = PremiumSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (isAddingManual) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = PrimaryAccent,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            isAddingManual = true
                                            viewModel.addManualMember(
                                                name = manualName,
                                                onSuccess = {
                                                    isAddingManual = false
                                                    manualName = ""
                                                    Toast.makeText(context, "Added successfully", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = {
                                                    isAddingManual = false
                                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        },
                                        enabled = manualName.isNotBlank()
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add Member",
                                            tint = if (manualName.isNotBlank()) PrimaryAccent else TextSecondary.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInviteDialog = false }) {
                        Text("Close", color = TextSecondary)
                    }
                },
                containerColor = PremiumSurface,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun BalanceBarChart(netBalances: Map<String, Double>, members: Map<String, User>) {
    val chartData = netBalances.toList().sortedByDescending { it.second }
    val maxAbsBalance = chartData.maxOfOrNull { Math.abs(it.second) }?.takeIf { it > 0.0 } ?: 1.0

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        chartData.forEach { (uid, balance) ->
            val member = members[uid]
            val name = member?.name?.split(" ")?.firstOrNull() ?: "Unknown"
            val isPositive = balance >= 0
            val barColor = if (isPositive) SuccessGreen else ErrorRed
            val fraction = (Math.abs(balance) / maxAbsBalance).toFloat()

            var animationProgress by remember { mutableStateOf(0f) }
            LaunchedEffect(fraction) {
                androidx.compose.animation.core.animate(
                    initialValue = 0f,
                    targetValue = fraction,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
                ) { value, _ ->
                    animationProgress = value
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        name, 
                        color = TextPrimary, 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    AutoSizeText(
                        text = "${if (isPositive) "+" else "-"}₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(Math.abs(balance))}",
                        color = barColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PremiumSurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animationProgress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }
            }
        }
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
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        member.name, 
                        color = Color.White, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
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
                Text(
                    member.email, 
                    color = TextSecondary, 
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 120.dp)) {
                if (balance > 0) {
                    Text("Gets Back", color = SuccessGreen, fontSize = 12.sp)
                    AutoSizeText(
                        text = "+₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(balance)}", 
                        color = SuccessGreen, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (balance < 0) {
                    Text("Owes", color = ErrorRed, fontSize = 12.sp)
                    AutoSizeText(
                        text = "-₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(Math.abs(balance))}", 
                        color = ErrorRed, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("Settled", color = TextSecondary, fontSize = 12.sp)
                    Text("₹0.00", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
