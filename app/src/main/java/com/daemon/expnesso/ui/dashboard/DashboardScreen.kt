package com.daemon.expnesso.ui.dashboard

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.daemon.expnesso.data.model.Session
import com.daemon.expnesso.data.model.Transaction
import com.daemon.expnesso.data.model.User
import com.daemon.expnesso.data.repository.AuthRepository
import com.daemon.expnesso.data.repository.FirestoreRepository
import com.daemon.expnesso.ui.theme.*
import com.daemon.expnesso.ui.utils.shimmerEffect
import com.daemon.expnesso.utils.QRCodeUtils
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: DashboardViewModel) {
    val context = LocalContext.current

    val transactions by viewModel.transactions.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val session by viewModel.session.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val sessionMembers by viewModel.sessionMembers.collectAsState()
    val netBalances by viewModel.netBalances.collectAsState()

    // Dialog States
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showCreateBookDialog by remember { mutableStateOf(false) }
    var showJoinBookDialog by remember { mutableStateOf(false) }
    var showDeleteBookDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val scannerOptions = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
    }
    val scanner = remember { GmsBarcodeScanning.getClient(context, scannerOptions) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = PremiumSurface,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Expnesso",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryAccent,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = PremiumSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = { Text("Sign Out", fontSize = 16.sp) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = ErrorRed) },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedTextColor = ErrorRed,
                        unselectedIconColor = ErrorRed
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = PremiumBackground,
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })
                    ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Join Book", color = Color.White, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(12.dp))
                            SmallFloatingActionButton(
                                onClick = {
                                    fabExpanded = false
                                    showJoinBookDialog = true
                                },
                                containerColor = PremiumSurfaceVariant,
                                contentColor = Color.White
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Join Book")
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Create Book", color = Color.White, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(12.dp))
                            SmallFloatingActionButton(
                                onClick = {
                                    fabExpanded = false
                                    showCreateBookDialog = true
                                },
                                containerColor = PremiumSurfaceVariant,
                                contentColor = Color.White
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Create Book")
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Add Expense", color = Color.White, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(12.dp))
                            SmallFloatingActionButton(
                                onClick = {
                                    fabExpanded = false
                                    showAddExpenseDialog = true
                                },
                                containerColor = PrimaryAccent,
                                contentColor = Color.Black
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Expense")
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = if (fabExpanded) PremiumSurfaceVariant else PrimaryAccent,
                    contentColor = if (fabExpanded) Color.White else Color.Black
                ) {
                    Icon(if (fabExpanded) Icons.Default.Close else Icons.Default.Add, contentDescription = "Toggle Actions")
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Expnesso",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PremiumBackground)
            )
        }
    ) { padding ->
        val sessionBalances by viewModel.sessionBalances.collectAsState()
        val myCredits by viewModel.myCredits.collectAsState()
        val myDebts by viewModel.myDebts.collectAsState()
        val currentUser = sessionMembers[viewModel.currentUserId]
        val myBalance = netBalances[viewModel.currentUserId] ?: 0.0
        val totalWillGet = myCredits.sumOf { it.amount }
        val totalWillPay = myDebts.sumOf { it.amount }

        val listState = rememberLazyListState()
        val headerState = remember { androidx.compose.animation.core.MutableTransitionState(true) }
        var isConsumingCurrentGesture by remember { mutableStateOf(false) }

        val nestedScrollConnection = remember {
            object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                    if (available.y < 0) {
                        if (headerState.targetState) {
                            headerState.targetState = false
                            isConsumingCurrentGesture = true
                        }
                        if (isConsumingCurrentGesture) {
                            return androidx.compose.ui.geometry.Offset(0f, available.y)
                        }
                    } else if (available.y > 0) {
                        if (isConsumingCurrentGesture) {
                            return androidx.compose.ui.geometry.Offset(0f, available.y)
                        }
                    }
                    return androidx.compose.ui.geometry.Offset.Zero
                }

                override fun onPostScroll(
                    consumed: androidx.compose.ui.geometry.Offset,
                    available: androidx.compose.ui.geometry.Offset,
                    source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
                ): androidx.compose.ui.geometry.Offset {
                    if (available.y > 0) {
                        if (!headerState.targetState) {
                            headerState.targetState = true
                            isConsumingCurrentGesture = true
                        }
                        if (isConsumingCurrentGesture) {
                            return androidx.compose.ui.geometry.Offset(0f, available.y)
                        }
                    }
                    return androidx.compose.ui.geometry.Offset.Zero
                }

                override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                    if (isConsumingCurrentGesture) {
                        isConsumingCurrentGesture = false
                        return available
                    }
                    return androidx.compose.ui.unit.Velocity.Zero
                }
            }
        }

        LaunchedEffect(session?.id) {
            headerState.targetState = true
            listState.scrollToItem(0)
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // Dashboard Card fixed at the top
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                AnimatedContent(
                    targetState = session,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "session_animation_header"
                ) { targetSession ->
                    if (targetSession == null) {
                        Column(modifier = Modifier.background(PremiumBackground)) {
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect())
                        }
                    } else {
                        Column(modifier = Modifier.background(PremiumBackground)) {
                            // Premium Summary Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Brush.linearGradient(colors = listOf(PremiumSurfaceVariant, PremiumSurface)))
                                    .clickable { navController.navigate("book_details/${targetSession.id}") }
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = targetSession.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 22.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Current Book",
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { showAddExpenseDialog = true }) {
                                                Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Color.White)
                                            }
                                            Box {
                                                IconButton(onClick = { menuExpanded = true }) {
                                                    Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                                                }
                                                DropdownMenu(
                                                    expanded = menuExpanded,
                                                    onDismissRequest = { menuExpanded = false },
                                                    containerColor = PremiumSurface
                                                ) {
                                                    val isDefault = currentUser?.defaultSessionId == targetSession.id
                                                    if (!isDefault) {
                                                        DropdownMenuItem(
                                                            text = { Text("Set as Default", color = Color.White) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                viewModel.setAsDefaultSession(targetSession.id)
                                                                Toast.makeText(context, "${targetSession.name} set as default", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                    DropdownMenuItem(
                                                        text = { Text("Invite Member", color = Color.White) },
                                                        onClick = {
                                                            menuExpanded = false
                                                            showInviteDialog = true
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Transactions", color = Color.White) },
                                                        onClick = {
                                                            menuExpanded = false
                                                            navController.navigate("transactions")
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Book Details", color = Color.White) },
                                                        onClick = {
                                                            menuExpanded = false
                                                            navController.navigate("book_details/${targetSession.id}")
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Delete Book", color = ErrorRed) },
                                                        onClick = {
                                                            menuExpanded = false
                                                            showDeleteBookDialog = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = currentUser?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=${currentUser?.name}" } ?: "https://ui-avatars.com/api/?name=?",
                                            contentDescription = "My Avatar",
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(PremiumSurfaceVariant),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(24.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Total Bal: ₹${String.format("%.2f", myBalance)}",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("will get", color = TextSecondary, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("₹${String.format("%.2f", totalWillGet)}", color = SuccessGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("will pay", color = TextSecondary, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("₹${String.format("%.2f", totalWillPay)}", color = ErrorRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    val membersList = sessionMembers.values.toList()
                                    if (membersList.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(20.dp))
                                        val displayMembers = membersList.take(4)
                                        val remainingCount = membersList.size - 4

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Members:", color = TextSecondary, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Box(contentAlignment = Alignment.CenterStart) {
                                                displayMembers.forEachIndexed { index, user ->
                                                    val firstName = user.name.split(" ").firstOrNull() ?: "Unknown"
                                                    AsyncImage(
                                                        model = user.photoUrl.ifEmpty { "https://ui-avatars.com/api/?name=$firstName" },
                                                        contentDescription = "Member Avatar",
                                                        modifier = Modifier
                                                            .padding(start = (index * 24).dp)
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .border(2.dp, PremiumSurfaceVariant, CircleShape)
                                                            .background(PremiumSurface),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                if (remainingCount > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(start = (displayMembers.size * 24).dp)
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .border(2.dp, PremiumSurfaceVariant, CircleShape)
                                                            .background(PrimaryAccent),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "+$remainingCount",
                                                            color = Color.Black,
                                                            fontSize = 12.sp,
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
            }

            // Collapsible Mid Section
            AnimatedVisibility(
                visibleState = headerState,
                enter = expandVertically(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
                exit = shrinkVertically(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
            ) {
                Column(modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -5f) { // Swiped up
                                headerState.targetState = false
                            }
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedContent(
                    targetState = session,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "session_animation_credit_debt"
                ) { targetSession ->
                    if (targetSession == null) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.width(80.dp).height(24.dp).shimmerEffect())
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.width(80.dp).height(24.dp).shimmerEffect())
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
                            }
                        }
                    } else {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (myCredits.isEmpty() && myDebts.isEmpty()) {
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
                                        text = "All settled up!",
                                        color = TextSecondary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                val membersList = sessionMembers.values.toList()
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // Left Column (Credits / Owes You)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Credit", color = SuccessGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                        HorizontalDivider(color = SuccessGreen, modifier = Modifier.padding(vertical = 8.dp))
                                        if (myCredits.isEmpty()) {
                                            Text("No credits", color = TextSecondary, fontSize = 14.sp)
                                        } else {
                                            myCredits.forEach { debt ->
                                                val debtor = sessionMembers[debt.fromUid]
                                                val firstName = debtor?.name?.split(" ")?.firstOrNull() ?: "Unknown"
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    AsyncImage(
                                                        model = debtor?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=$firstName" } ?: "https://ui-avatars.com/api/?name=?",
                                                        contentDescription = "Avatar",
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(PremiumSurfaceVariant),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(firstName, color = TextPrimary, fontSize = 14.sp)
                                                        Text("+₹${String.format("%.2f", debt.amount)}", color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                            }
                                        }
                                    }
                                    
                                    // Right Column (Debts / You Owe)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Debt", color = ErrorRed, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                        HorizontalDivider(color = ErrorRed, modifier = Modifier.padding(vertical = 8.dp))
                                        if (myDebts.isEmpty()) {
                                            Text("No debts", color = TextSecondary, fontSize = 14.sp)
                                        } else {
                                            myDebts.forEach { debt ->
                                                val creditor = sessionMembers[debt.toUid]
                                                val firstName = creditor?.name?.split(" ")?.firstOrNull() ?: "Unknown"
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    AsyncImage(
                                                        model = creditor?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=$firstName" } ?: "https://ui-avatars.com/api/?name=?",
                                                        contentDescription = "Avatar",
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(PremiumSurfaceVariant),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(firstName, color = TextPrimary, fontSize = 14.sp)
                                                        Text("-₹${String.format("%.2f", debt.amount)}", color = ErrorRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // End of AnimatedContent
            } // End of Column

            // Scrollable list below the fixed card
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PremiumBackground)
                        .padding(vertical = 12.dp)
                ) {
                    Text("Your Books", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            items(allSessions) { s ->
                    val balances = sessionBalances[s.id] ?: Pair(0.0, 0.0)
                    BookItem(
                        session = s,
                        willGet = balances.first,
                        willPay = balances.second,
                        isSelected = s.id == session?.id,
                        isDefault = s.id == currentUser?.defaultSessionId,
                        onClick = { viewModel.switchSession(s.id) }
                    )
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        val isGroup = sessionMembers.size > 1
        AddTransactionDialog(
            sessionMembers = sessionMembers.values.toList(),
            currentUserId = viewModel.currentUserId,
            isGroup = isGroup,
            onDismiss = { showAddExpenseDialog = false },
            onAdd = { amount, desc, paidByUid, splits ->
                viewModel.addTransaction(amount, desc, paidByUid, splits) {
                    showAddExpenseDialog = false
                }
            }
        )
    }

    if (showInviteDialog && session != null) {
        var manualName by remember { mutableStateOf("") }
        var isAddingManual by remember { mutableStateOf(false) }

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

                    val qrBitmap = remember(session!!.inviteCode) {
                        QRCodeUtils.generateQRCode(session!!.inviteCode)
                    }
                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(12.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
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

    if (showCreateBookDialog) {
        var bookName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateBookDialog = false },
            title = { Text("Create New Book", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = bookName,
                    onValueChange = { bookName = it },
                    label = { Text("Book Name", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createSession(bookName) {
                            showCreateBookDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBookDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = PremiumSurface
        )
    }

    if (showJoinBookDialog) {
        var inviteCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinBookDialog = false },
            title = { Text("Join Book", color = TextPrimary) },
            text = {
                Column {
                    Button(
                        onClick = {
                            showJoinBookDialog = false
                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    val rawValue = barcode.rawValue
                                    if (!rawValue.isNullOrBlank()) {
                                        viewModel.joinSession(
                                            inviteCode = rawValue,
                                            onSuccess = { Toast.makeText(context, "Joined successfully!", Toast.LENGTH_SHORT).show() },
                                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                                        )
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Scan failed: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Scan", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan QR Code", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Or enter code manually:", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it.uppercase() },
                        label = { Text("Invite Code", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteCode.length == 6) {
                            viewModel.joinSession(
                                inviteCode = inviteCode,
                                onSuccess = {
                                    Toast.makeText(context, "Joined successfully!", Toast.LENGTH_SHORT).show()
                                    showJoinBookDialog = false
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    enabled = inviteCode.length == 6
                ) {
                    Text("Join", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinBookDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = PremiumSurface
        )
    }

    if (showDeleteBookDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteBookDialog = false },
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
                        session?.let { currentSession ->
                            viewModel.deleteSession(
                                sessionId = currentSession.id,
                                onSuccess = {
                                    Toast.makeText(context, "Book moved to bin", Toast.LENGTH_SHORT).show()
                                    showDeleteBookDialog = false
                                },
                                onError = {
                                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                    showDeleteBookDialog = false
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBookDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = PremiumSurface
        )
    }
    } // Closes ModalNavigationDrawer
} // Closes DashboardScreen
@Composable
fun BookItem(
    session: Session,
    willGet: Double,
    willPay: Double,
    isSelected: Boolean,
    isDefault: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PremiumSurfaceVariant else PremiumSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Get: +₹${String.format("%.2f", willGet)}", color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Pay: -₹${String.format("%.2f", willPay)}", color = ErrorRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (isDefault) {
                Icon(Icons.Default.Star, contentDescription = "Default", tint = SecondaryAccent)
            }
        }
    }
}
