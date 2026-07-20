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

    val suggestions = listOf("Food \uD83C\uDF54", "Transport \uD83D\uDE95", "Rent \uD83C\uDFE0", "Groceries \uD83D\uDED2", "Shopping \uD83D\uDECD\uFE0F", "Entertainment \uD83C\uDF7F", "Medical \uD83D\uDC8A", "Other \uD83D\uDCCC")

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
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
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(PremiumSurfaceVariant)
                                    .clickable { description = suggestion }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(text = suggestion, color = TextPrimary, fontSize = 14.sp)
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(PremiumSurface),
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
                                    fontWeight = FontWeight.Bold
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
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

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
                                    modifier = Modifier.width(100.dp).height(50.dp),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = PrimaryAccent,
                                        unfocusedBorderColor = PremiumSurfaceVariant,
                                        focusedContainerColor = PremiumBackground,
                                        unfocusedContainerColor = PremiumBackground
                                    )
                                )
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
                enabled = amount.toDoubleOrNull() != null && description.isNotBlank() && (!isCustomSplit || kotlin.math.abs((amount.toDoubleOrNull() ?: 0.0) - selectedUids.filterValues { it }.keys.sumOf { customAmounts[it]?.toDoubleOrNull() ?: 0.0 }) < 0.01)
            ) {
                Text("Add Expense", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
