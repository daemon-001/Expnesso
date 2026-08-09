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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.daemon.expnesso.ui.utils.AutoSizeText
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
        @OptIn(ExperimentalMaterial3Api::class)
        val pullRefreshState = rememberPullToRefreshState()
        var isRefreshing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        val sdf = androidx.compose.runtime.remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
        
        androidx.compose.runtime.LaunchedEffect(transactions) {
            isRefreshing = false
        }

        @OptIn(ExperimentalMaterial3Api::class)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { 
                isRefreshing = true
                session?.id?.let { viewModel.switchSession(it) } ?: run { isRefreshing = false }
            },
            state = pullRefreshState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(transactions, key = { it.id }) { tx ->
                        TransactionItem(
                            transaction = tx,
                            paidBy = sessionMembers[tx.paidByUid.ifEmpty { tx.addedByUid }],
                            currentUserId = viewModel.currentUserId,
                            adminUid = session?.adminUid,
                            sessionMembers = sessionMembers,
                            sdf = sdf,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    paidBy: User?,
    currentUserId: String?,
    adminUid: String?,
    sessionMembers: Map<String, User>,
    sdf: SimpleDateFormat,
    onDelete: () -> Unit
) {
    val canDelete = currentUserId == adminUid || currentUserId == transaction.addedByUid
    var showDeleteDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction", color = TextPrimary) },
            text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = PremiumSurface
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = paidBy?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=${paidBy?.name}" } ?: "https://ui-avatars.com/api/?name=?",
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
                    Text("${sdf.format(transaction.timestamp.toDate())} • Paid by ${paidBy?.name ?: "Unknown"}", color = TextSecondary, fontSize = 12.sp)
                }
                AutoSizeText(
                    text = "₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(transaction.amount)}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (canDelete) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                    }
                }
            }

            if (transaction.splits.isNotEmpty()) {
                HorizontalDivider(color = PremiumSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                SimpleFlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalSpacing = 8.dp,
                    verticalSpacing = 8.dp
                ) {
                    val splitsList = transaction.splits.toList()
                    splitsList.forEach { (uid, splitAmount) ->
                        val user = sessionMembers[uid]
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PremiumBackground)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = user?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=${user?.name}" } ?: "https://ui-avatars.com/api/?name=?",
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(PremiumSurfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AutoSizeText(
                                text = "${user?.name?.split(" ")?.firstOrNull() ?: "Unknown"} • ₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(splitAmount)}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    verticalSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val horizontalSpacingPx = horizontalSpacing.roundToPx()
        val verticalSpacingPx = verticalSpacing.roundToPx()

        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()

        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            if (currentRowWidth + placeable.width > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                rowWidths.add(currentRowWidth - horizontalSpacingPx)
                rowHeights.add(currentRowHeight)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + horizontalSpacingPx
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentRowWidth - horizontalSpacingPx)
            rowHeights.add(currentRowHeight)
        }

        val contentWidth = rowWidths.maxOrNull() ?: 0
        val width = maxOf(constraints.minWidth, minOf(constraints.maxWidth, contentWidth))
        val height = maxOf(constraints.minHeight, rowHeights.sum() + maxOf(0, rows.size - 1) * verticalSpacingPx)

        layout(width, height) {
            var y = 0
            rows.forEachIndexed { i, row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + horizontalSpacingPx
                }
                y += rowHeights[i] + verticalSpacingPx
            }
        }
    }
}
