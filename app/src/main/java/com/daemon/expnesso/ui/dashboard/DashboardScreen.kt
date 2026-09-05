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
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
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
import com.daemon.expnesso.data.model.Session
import com.daemon.expnesso.data.model.Transaction
import com.daemon.expnesso.data.model.User
import com.daemon.expnesso.data.repository.AuthRepository
import com.daemon.expnesso.data.repository.FirestoreRepository
import com.daemon.expnesso.ui.theme.*
import com.daemon.expnesso.ui.utils.shimmerEffect
import com.daemon.expnesso.ui.utils.AutoSizeText
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
    val isSessionsLoaded by viewModel.isSessionsLoaded.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val allUserTransactions by viewModel.allUserTransactions.collectAsState()
    val sessionMembers by viewModel.sessionMembers.collectAsState()
    val netBalances by viewModel.netBalances.collectAsState()
    val isNetworkAvailable by com.daemon.expnesso.ui.utils.rememberNetworkStatus()

    // Dialog States
    var fabExpanded by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showCreateBookDialog by remember { mutableStateOf(false) }
    var showJoinBookDialog by remember { mutableStateOf(false) }
    var showDeleteBookDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var bookToRename by remember { mutableStateOf<com.daemon.expnesso.data.model.Session?>(null) }
    var bookToDelete by remember { mutableStateOf<com.daemon.expnesso.data.model.Session?>(null) }

    val scannerOptions = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
    }
    val scanner = remember { GmsBarcodeScanning.getClient(context, scannerOptions) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var backPressedTime by remember { mutableLongStateOf(0L) }
    val activity = context as? android.app.Activity

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = drawerState.isClosed) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            activity?.finish()
        } else {
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            backPressedTime = currentTime
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = PremiumSurface,
                modifier = Modifier.width(300.dp)
            ) {
                val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val photoUrl = fbUser?.photoUrl?.toString() ?: ""
                val displayName = fbUser?.displayName ?: "User"
                val email = fbUser?.email ?: ""
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    AsyncImage(
                        model = photoUrl.ifEmpty { "https://ui-avatars.com/api/?name=$displayName" },
                        contentDescription = "Profile Icon",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PremiumSurfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = email, fontSize = 14.sp, color = TextSecondary)
                }
                
                HorizontalDivider(color = PremiumSurfaceVariant)
                
                Spacer(modifier = Modifier.height(8.dp))
                NavigationDrawerItem(
                    label = { Text("Create Book", color = TextPrimary) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null, tint = TextPrimary) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        showCreateBookDialog = true 
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    label = { Text("Join Book", color = TextPrimary) },
                    icon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextPrimary) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        showJoinBookDialog = true 
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                
                HorizontalDivider(color = PremiumSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Your Books",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allSessions) { book ->
                        NavigationDrawerItem(
                            label = { Text(book.name, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            icon = { 
                                Icon(Icons.Default.List, contentDescription = null, tint = TextSecondary)
                            },
                            badge = {
                                var dropdownExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { dropdownExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary)
                                    }
                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false },
                                        containerColor = PremiumSurfaceVariant
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Add Fav.", color = Color.White) },
                                            onClick = {
                                                dropdownExpanded = false
                                                viewModel.setAsDefaultSession(book.id)
                                                Toast.makeText(context, "${book.name} set as default", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        if (book.adminUid == viewModel.currentUserId) {
                                            DropdownMenuItem(
                                                text = { Text("Rename Book", color = Color.White) },
                                                onClick = {
                                                    dropdownExpanded = false
                                                    bookToRename = book
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete Book", color = ErrorRed) },
                                                onClick = {
                                                    dropdownExpanded = false
                                                    bookToDelete = book
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            selected = book.id == currentSessionId,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (book.id != currentSessionId) {
                                    viewModel.switchSession(book.id)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = PremiumSurfaceVariant,
                                unselectedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
                
                HorizontalDivider(color = PremiumSurfaceVariant)
                
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = PremiumBackground,
            floatingActionButton = {
                Box(contentAlignment = Alignment.BottomEnd) {
                    val progress by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (fabExpanded) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                        ),
                        label = "fabProgress"
                    )

                    val radius = 120f // We'll use dp multiplication later
                    
                    if (progress > 0f) {
                        // Join Book (Left)
                        FloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                showJoinBookDialog = true
                            },
                            modifier = Modifier
                                .offset(x = (-(radius * progress)).dp, y = 0.dp)
                                .alpha(progress)
                                .size(48.dp),
                            containerColor = Color(0xFF8B127C), // Dark Magenta
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Join Book")
                        }

                        // Create Book (Diagonal)
                        FloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                showCreateBookDialog = true
                            },
                            modifier = Modifier
                                .offset(x = (-(radius * 0.7071f * progress)).dp, y = (-(radius * 0.7071f * progress)).dp)
                                .alpha(progress)
                                .size(48.dp),
                            containerColor = Color(0xFF6A67CE), // Blue/Indigo
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Create Book")
                        }

                        // Add Expense (Up)
                        FloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                navController.navigate(com.daemon.expnesso.navigation.Screen.AddExpense.route)
                            },
                            modifier = Modifier
                                .offset(x = 0.dp, y = (-(radius * progress)).dp)
                                .alpha(progress)
                                .size(48.dp),
                            containerColor = Color(0xFFB554D8), // Purple
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Expense")
                        }
                    }

                    // Main FAB
                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = if (fabExpanded) Color(0xFFFF6B4A) else PrimaryAccent,
                        contentColor = if (fabExpanded) Color.White else Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.rotate(progress * 135f)
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
        val sessionUserExpenses by viewModel.sessionUserExpenses.collectAsState()
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

            var globalTotalExpense = 0.0
            val globalNetBalance = sessionBalances.values.sumOf { it.first - it.second }
            
            allUserTransactions.forEach { tx ->
                val paidBy = tx.paidByUid.ifEmpty { tx.addedByUid }
                if (tx.splits.isNotEmpty()) {
                    val mySplit = tx.splits[viewModel.currentUserId] ?: 0.0
                    globalTotalExpense += mySplit
                } else {
                    if (paidBy == viewModel.currentUserId) {
                        globalTotalExpense += tx.amount
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Your Total Expense",
                        fontSize = 15.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    AutoSizeText(
                        text = "₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(globalTotalExpense)}",
                        fontSize = 36.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val (globalBalanceText, globalBalanceColor) = when {
                        globalNetBalance > 0 -> "+₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(globalNetBalance)}" to SuccessGreen
                        globalNetBalance < 0 -> "-₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(-globalNetBalance)}" to ErrorRed
                        else -> "Settled" to TextSecondary
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = TextSecondary)) {
                                append("Current: ")
                            }
                            withStyle(style = SpanStyle(color = globalBalanceColor)) {
                                append(globalBalanceText)
                            }
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                            .clickable { navController.navigate("expense_analytics") }
                            .background(PrimaryAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Info, contentDescription = "Details", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                        Text("Details", color = PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Dashboard Card fixed at the top
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                val targetSession = session
                if (targetSession == null) {
                    if (isSessionsLoaded && allSessions.isEmpty() && currentSessionId.isBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Brush.linearGradient(colors = listOf(PremiumSurfaceVariant, PremiumSurface)))
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Welcome to Expnesso!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Create or join a book to start tracking expenses.", color = TextSecondary, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    } else {
                        Column(modifier = Modifier.background(PremiumBackground)) {
                            Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect())
                        }
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
                                            val hasPendingSync = transactions.any { !it.isSynced }
                                            if (!isNetworkAvailable || hasPendingSync) {
                                                Icon(Icons.Default.CloudOff, contentDescription = "Offline or Pending Sync", tint = ErrorRed, modifier = Modifier.padding(end = 8.dp).size(24.dp))
                                            } else if (transactions.isNotEmpty()) {
                                                Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = SuccessGreen, modifier = Modifier.padding(end = 8.dp).size(24.dp))
                                            }
                                            IconButton(onClick = { navController.navigate(com.daemon.expnesso.navigation.Screen.AddExpense.route) }) {
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
                                                    val isDefaultBook = currentUser?.defaultSessionId == targetSession.id
                                                    if (!isDefaultBook) {
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
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = currentUser?.photoUrl?.ifEmpty { "https://ui-avatars.com/api/?name=${currentUser?.name}" } ?: "https://ui-avatars.com/api/?name=?",
                                            contentDescription = "My Avatar",
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(PremiumSurfaceVariant),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            val currentUserExpense = sessionUserExpenses[targetSession.id] ?: 0.0
                                            Text(
                                                text = "Your Expense",
                                                color = TextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            AutoSizeText(
                                                text = "₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(currentUserExpense)}",
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                            Column {
                                                Text("Will get", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                AutoSizeText("₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(totalWillGet)}", color = SuccessGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Column {
                                                Text("Will pay", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                AutoSizeText("₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(totalWillPay)}", color = ErrorRed, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }

                                        val membersList = sessionMembers.values.toList()
                                        if (membersList.isNotEmpty()) {
                                            val displayMembers = membersList.take(3)
                                            val remainingCount = membersList.size - 3
                                            Box(contentAlignment = Alignment.CenterStart) {
                                                displayMembers.forEachIndexed { index, user ->
                                                    val firstName = user.name.split(" ").firstOrNull() ?: "Unknown"
                                                    AsyncImage(
                                                        model = user.photoUrl.ifEmpty { "https://ui-avatars.com/api/?name=$firstName" },
                                                        contentDescription = "Member Avatar",
                                                        modifier = Modifier
                                                            .padding(start = (index * 20).dp)
                                                            .size(32.dp)
                                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                                            .border(2.dp, PremiumSurface, androidx.compose.foundation.shape.CircleShape)
                                                            .background(PremiumSurfaceVariant),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                }
                                                if (remainingCount > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(start = (displayMembers.size * 20).dp)
                                                            .size(32.dp)
                                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                                            .border(2.dp, PremiumSurface, androidx.compose.foundation.shape.CircleShape)
                                                            .background(PrimaryAccent),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "+$remainingCount",
                                                            color = Color.Black,
                                                            fontSize = 11.sp,
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


            // Collapsible Mid Section
            androidx.compose.animation.AnimatedVisibility(
                visible = headerState.targetState,
                enter = androidx.compose.animation.expandVertically(
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    )
                ),
                exit = androidx.compose.animation.shrinkVertically(
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    )
                )
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
                    val targetSession = session

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
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(firstName, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        AutoSizeText("+₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(debt.amount)}", color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(firstName, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        AutoSizeText("-₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(debt.amount)}", color = ErrorRed, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                }

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
                val sessionTx = allUserTransactions.filter { it.sessionId == s.id }
                BookItem(
                    session = s,
                    expense = sessionUserExpenses[s.id] ?: 0.0,
                    willGet = balances.first,
                    willPay = balances.second,
                    isSelected = s.id == session?.id,
                    isDefault = s.id == currentUser?.defaultSessionId,
                    hasPendingSync = sessionTx.any { !it.isSynced },
                    hasTransactions = sessionTx.isNotEmpty(),
                    isNetworkAvailable = isNetworkAvailable,
                    onClick = { viewModel.switchSession(s.id) }
                )
            }
            }
        } // End of Column
    }
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
                                adminUid = currentSession.adminUid,
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

    bookToRename?.let { book ->
        var newName by remember { mutableStateOf(book.name) }
        AlertDialog(
            onDismissRequest = { bookToRename = null },
            title = { Text("Rename Book", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Book Name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = PremiumSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameSession(
                            sessionId = book.id,
                            newName = newName,
                            onSuccess = {
                                bookToRename = null
                                Toast.makeText(context, "Book renamed", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    enabled = newName.isNotBlank() && newName != book.name
                ) {
                    Text("Rename", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToRename = null }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = PremiumSurface
        )
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Book", color = ErrorRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete '${book.name}'? This will move it to the bin, and you can restore it within 30 days.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(
                            sessionId = book.id,
                            adminUid = book.adminUid,
                            onSuccess = {
                                bookToDelete = null
                                Toast.makeText(context, "Book deleted", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = PremiumSurface
        )
    }
} // Closes ModalNavigationDrawer
@Composable
fun BookItem(
    session: Session,
    expense: Double,
    willGet: Double,
    willPay: Double,
    isSelected: Boolean,
    isDefault: Boolean = false,
    hasPendingSync: Boolean = false,
    hasTransactions: Boolean = false,
    isNetworkAvailable: Boolean = true,
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
                Text(session.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Expense: ₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(expense)}", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    
                    if (willGet > 0) {
                        Text("| Get: +₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(willGet)}", color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else if (willPay > 0) {
                        Text("| Pay: -₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(willPay)}", color = ErrorRed, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text("| Settled", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDefault) {
                    Icon(Icons.Default.Star, contentDescription = "Default", tint = SecondaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (!isNetworkAvailable || hasPendingSync) {
                    Icon(Icons.Default.CloudOff, contentDescription = "Offline or Pending Sync", tint = ErrorRed, modifier = Modifier.size(24.dp))
                } else if (hasTransactions) {
                    Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = SuccessGreen, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
