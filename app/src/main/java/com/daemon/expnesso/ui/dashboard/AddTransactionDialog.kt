package com.daemon.expnesso.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daemon.expnesso.data.model.User
import com.daemon.expnesso.ui.theme.PremiumSurface
import com.daemon.expnesso.ui.theme.PremiumSurfaceVariant
import com.daemon.expnesso.ui.theme.PrimaryAccent
import com.daemon.expnesso.ui.theme.TextPrimary
import com.daemon.expnesso.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    sessionMembers: List<User>,
    currentUserId: String,
    isGroup: Boolean,
    onDismiss: () -> Unit,
    onAdd: (amount: Double, description: String, paidByUid: String, splits: Map<String, Double>) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var paidByUid by remember { mutableStateOf(currentUserId) }
    
    var showPayerDropdown by remember { mutableStateOf(false) }

    val selectedUids = remember(sessionMembers) {
        mutableStateMapOf<String, Boolean>().apply {
            sessionMembers.forEach { put(it.uid, true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isGroup) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Paid By", color = TextSecondary, fontSize = 14.sp)
                    
                    Box {
                        val paidByUser = sessionMembers.find { it.uid == paidByUid }
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
                                disabledTextColor = TextPrimary
                            ),
                            enabled = false
                        )
                        DropdownMenu(
                            expanded = showPayerDropdown,
                            onDismissRequest = { showPayerDropdown = false },
                            modifier = Modifier.background(PremiumSurfaceVariant)
                        ) {
                            sessionMembers.forEach { member ->
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Split equally between:", color = TextSecondary, fontSize = 14.sp)
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(sessionMembers) { member ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedUids[member.uid] = !(selectedUids[member.uid] ?: false)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectedUids[member.uid] == true,
                                    onCheckedChange = { checked -> selectedUids[member.uid] = checked },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PrimaryAccent,
                                        checkmarkColor = Color.Black
                                    )
                                )
                                Text(member.name, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && description.isNotBlank()) {
                        val activeSplits = selectedUids.filterValues { it }.keys
                        val splits = if (isGroup && activeSplits.isNotEmpty()) {
                            val splitAmount = amt / activeSplits.size
                            activeSplits.associateWith { splitAmount }
                        } else emptyMap()
                        
                        onAdd(amt, description, paidByUid, splits)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.Black)
            ) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = TextSecondary) 
            }
        },
        containerColor = PremiumSurface
    )
}
