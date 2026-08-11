package com.daemon.expnesso.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.daemon.expnesso.data.model.User
import com.daemon.expnesso.ui.theme.*

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    navController: NavController,
    viewModel: DashboardViewModel
) {
    val session by viewModel.session.collectAsState()
    val sessionMembers by viewModel.sessionMembers.collectAsState()
    val currentUserId = viewModel.currentUserId

    val membersList = sessionMembers.values.toList()
    val isGroup = membersList.size > 1

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var paidByUid by remember { mutableStateOf(currentUserId) }
    
    var showPayerDropdown by remember { mutableStateOf(false) }

    var isCustomSplit by remember { mutableStateOf(false) }

    val selectedUids = remember(membersList) {
        mutableStateMapOf<String, Boolean>().apply {
            membersList.forEach { put(it.uid, true) }
        }
    }

    val customAmounts = remember(membersList) {
        mutableStateMapOf<String, String>().apply {
            membersList.forEach { put(it.uid, "") }
        }
    }

    val suggestions = listOf("Food 🍔", "Transport 🚕", "Rent 🏠", "Groceries 🛒", "Shopping 🛍️", "Entertainment 🍿", "Medical 💊", "Other 📌")

    var selectedTab by remember { mutableIntStateOf(0) }
    var paidToUid by remember { mutableStateOf(membersList.firstOrNull { it.uid != currentUserId }?.uid ?: "") }
    var showPayToDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PremiumBackground)
            )
        },
        containerColor = PremiumBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = PrimaryAccent,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryAccent
                        )
                    }
                },
                divider = { HorizontalDivider(color = PremiumSurfaceVariant) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Add Expense", color = if (selectedTab == 0) PrimaryAccent else TextSecondary) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pay Off", color = if (selectedTab == 1) PrimaryAccent else TextSecondary) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (₹)", color = TextSecondary) },
                        leadingIcon = { Text("₹", fontSize = 24.sp, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = PremiumSurfaceVariant,
                            focusedContainerColor = PremiumSurface,
                            unfocusedContainerColor = PremiumSurface
                        ),
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description", color = TextSecondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = PremiumSurfaceVariant,
                            focusedContainerColor = PremiumSurface,
                            unfocusedContainerColor = PremiumSurface
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Suggestion Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(suggestions) { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PremiumSurfaceVariant)
                                    .clickable { description = suggestion }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = TextPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = suggestion, color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (isGroup) {
                    item {
                        Text("Paid By", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val paidByUser = membersList.find { it.uid == paidByUid }
                            OutlinedTextField(
                                value = paidByUser?.name ?: "Unknown",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPayerDropdown = true },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    disabledTextColor = TextPrimary,
                                    focusedBorderColor = PremiumSurfaceVariant,
                                    unfocusedBorderColor = PremiumSurfaceVariant,
                                    focusedContainerColor = PremiumSurface,
                                    unfocusedContainerColor = PremiumSurface
                                ),
                                enabled = false,
                                shape = RoundedCornerShape(16.dp)
                            )
                            DropdownMenu(
                                expanded = showPayerDropdown,
                                onDismissRequest = { showPayerDropdown = false },
                                modifier = Modifier.background(PremiumSurfaceVariant)
                            ) {
                                membersList.forEach { member ->
                                    DropdownMenuItem(
                                        text = { Text(member.name, color = TextPrimary) },
                                        onClick = {
                                            paidByUid = member.uid
                                            showPayerDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        // Split Mode Toggle
                        if (selectedTab == 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(PremiumSurfaceVariant.copy(alpha = 0.5f))
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(if (!isCustomSplit) PrimaryAccent else Color.Transparent)
                                        .clickable { isCustomSplit = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Split Equally", 
                                        color = if (!isCustomSplit) Color.Black else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(if (isCustomSplit) PrimaryAccent else Color.Transparent)
                                        .clickable { isCustomSplit = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Split Custom", 
                                        color = if (isCustomSplit) Color.Black else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        } else {
                            Text("Paid To", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val paidToUser = membersList.find { it.uid == paidToUid }
                                OutlinedTextField(
                                    value = paidToUser?.name ?: "Unknown",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showPayToDropdown = true },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        disabledTextColor = TextPrimary,
                                        focusedBorderColor = PremiumSurfaceVariant,
                                        unfocusedBorderColor = PremiumSurfaceVariant,
                                        focusedContainerColor = PremiumSurface,
                                        unfocusedContainerColor = PremiumSurface
                                    ),
                                    enabled = false,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                DropdownMenu(
                                    expanded = showPayToDropdown,
                                    onDismissRequest = { showPayToDropdown = false },
                                    modifier = Modifier.background(PremiumSurfaceVariant)
                                ) {
                                    membersList.forEach { member ->
                                        if (member.uid != paidByUid) {
                                            DropdownMenuItem(
                                                text = { Text(member.name, color = TextPrimary) },
                                                onClick = {
                                                    paidToUid = member.uid
                                                    showPayToDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTab == 0) {
                        if (isCustomSplit) {
                            item {
                                val totalAmt = amount.toDoubleOrNull() ?: 0.0
                                val currentSum = selectedUids.filterValues { it }.keys.sumOf { customAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                                
                                val diff = totalAmt - currentSum
                                
                                val (tallyText, tallyColor) = when {
                                    totalAmt == 0.0 -> "Enter amount above" to TextSecondary
                                    kotlin.math.abs(diff) < 0.01 -> "Tallied" to Color(0xFF4CAF50)
                                    diff > 0 -> "Remaining: ₹${String.format("%.2f", diff)}" to Color(0xFFFFC107)
                                    else -> "Over by: ₹${String.format("%.2f", -diff)}" to ErrorRed
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Custom Split", color = TextSecondary, fontSize = 14.sp)
                                    Text(tallyText, color = tallyColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            item {
                                Text("Split equally between:", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                            }
                        }

                        items(membersList) { member ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PremiumSurface)
                                    .clickable {
                                        selectedUids[member.uid] = !(selectedUids[member.uid] ?: false)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = selectedUids[member.uid] == true,
                                    onCheckedChange = { checked -> selectedUids[member.uid] = checked },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PrimaryAccent,
                                        checkmarkColor = Color.Black,
                                        uncheckedColor = TextSecondary
                                    )
                                )
                                Text(member.name, color = TextPrimary, modifier = Modifier.weight(1f))
                                
                                if (isCustomSplit && selectedUids[member.uid] == true) {
                                    OutlinedTextField(
                                        value = customAmounts[member.uid] ?: "",
                                        onValueChange = { customAmounts[member.uid] = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        singleLine = true,
                                        placeholder = { Text("0", color = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) },
                                        modifier = Modifier.width(90.dp).height(50.dp),
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedBorderColor = PrimaryAccent,
                                            unfocusedBorderColor = PremiumSurfaceVariant.copy(alpha = 0.5f),
                                            focusedContainerColor = PremiumSurfaceVariant.copy(alpha = 0.3f),
                                            unfocusedContainerColor = PremiumSurfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            
            // Bottom Button
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && description.isNotBlank()) {
                        if (selectedTab == 0) {
                            val activeSplits = selectedUids.filterValues { it }.keys
                            val splits = if (isGroup && activeSplits.isNotEmpty()) {
                                if (isCustomSplit) {
                                    activeSplits.associateWith { customAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                                } else {
                                    val splitAmount = amt / activeSplits.size
                                    activeSplits.associateWith { splitAmount }
                                }
                            } else emptyMap()
                            
                            // Check custom split tally
                            if (isGroup && isCustomSplit) {
                                val sum = splits.values.sum()
                                if (kotlin.math.abs(sum - amt) > 0.01) {
                                    return@Button
                                }
                            }
                            
                            viewModel.addTransaction(amt, description, paidByUid, splits) {
                                navController.popBackStack()
                            }
                        } else {
                            if (paidToUid.isNotBlank()) {
                                val splits = mapOf(paidToUid to amt)
                                viewModel.addTransaction(amt, description, paidByUid, splits) {
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAccent,
                    contentColor = Color.Black,
                    disabledContainerColor = PremiumSurfaceVariant,
                    disabledContentColor = TextSecondary
                ),
                enabled = amount.toDoubleOrNull() != null && description.isNotBlank() && 
                          (if (selectedTab == 0) 
                              (!isCustomSplit || kotlin.math.abs((amount.toDoubleOrNull() ?: 0.0) - selectedUids.filterValues { it }.keys.sumOf { customAmounts[it]?.toDoubleOrNull() ?: 0.0 }) < 0.01)
                           else 
                              paidToUid.isNotBlank() && paidByUid != paidToUid)
            ) {
                Text(if (selectedTab == 0) "Add Expense" else "Pay Off", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
