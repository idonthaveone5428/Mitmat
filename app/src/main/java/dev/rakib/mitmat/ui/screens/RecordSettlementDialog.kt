package dev.rakib.mitmat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rakib.mitmat.data.entity.ParticipantEntity
import dev.rakib.mitmat.domain.model.DebtTransaction
import dev.rakib.mitmat.ui.components.ParticipantAvatar
import dev.rakib.mitmat.ui.theme.LavenderAccent
import dev.rakib.mitmat.ui.theme.MintAccent
import dev.rakib.mitmat.ui.theme.PeachAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSettlementDialog(
    groupId: Long,
    participants: List<ParticipantEntity>,
    currency: String,
    initialDebt: DebtTransaction? = null,
    onConfirm: (payerId: Long, receiverId: Long, amount: Double, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var payerId by remember {
        mutableStateOf(initialDebt?.fromParticipantId ?: participants.firstOrNull()?.id ?: 0L)
    }
    var receiverId by remember {
        mutableStateOf(
            initialDebt?.toParticipantId
                ?: participants.getOrNull(1)?.id
                ?: participants.firstOrNull()?.id
                ?: 0L
        )
    }
    var amountText by remember {
        mutableStateOf(initialDebt?.amount?.let { if (it > 0) it.toString() else "" } ?: "")
    }
    var note by remember { mutableStateOf(if (initialDebt != null) "Settled simplified debt" else "Cash / bKash payment") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var payerExpanded by remember { mutableStateOf(false) }
    var receiverExpanded by remember { mutableStateOf(false) }

    val payer = participants.firstOrNull { it.id == payerId }
    val receiver = participants.firstOrNull { it.id == receiverId }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Handshake,
                contentDescription = null,
                tint = MintAccent
            )
        },
        title = {
            Text(
                text = "Record Settlement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Record a direct payment between members to settle debts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Who Paid
                ExposedDropdownMenuBox(
                    expanded = payerExpanded,
                    onExpandedChange = { payerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = payer?.name ?: "Select Payer",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Who Paid (Payer)") },
                        leadingIcon = {
                            payer?.let {
                                ParticipantAvatar(name = it.name, colorIndex = it.avatarColorIndex, size = 26.dp)
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = payerExpanded,
                        onDismissRequest = { payerExpanded = false }
                    ) {
                        participants.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name + if (p.isCurrentUser) " (You)" else "") },
                                onClick = {
                                    payerId = p.id
                                    payerExpanded = false
                                }
                            )
                        }
                    }
                }

                // Who Received
                ExposedDropdownMenuBox(
                    expanded = receiverExpanded,
                    onExpandedChange = { receiverExpanded = it }
                ) {
                    OutlinedTextField(
                        value = receiver?.name ?: "Select Receiver",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Who Received (Receiver)") },
                        leadingIcon = {
                            receiver?.let {
                                ParticipantAvatar(name = it.name, colorIndex = it.avatarColorIndex, size = 26.dp)
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = receiverExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = receiverExpanded,
                        onDismissRequest = { receiverExpanded = false }
                    ) {
                        participants.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name + if (p.isCurrentUser) " (You)" else "") },
                                onClick = {
                                    receiverId = p.id
                                    receiverExpanded = false
                                }
                            )
                        }
                    }
                }

                // Settlement Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Settlement Amount") },
                    prefix = { Text("$currency ", fontWeight = FontWeight.Bold, color = MintAccent) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settlement_amount_input")
                )

                // Optional Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g. bKash, Cash, Bank)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = PeachAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        shape = RoundedCornerShape(22.dp),
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (payerId == 0L || receiverId == 0L) {
                        errorMessage = "Please select both payer and receiver"
                        return@Button
                    }
                    if (payerId == receiverId) {
                        errorMessage = "Payer and receiver cannot be the same person"
                        return@Button
                    }
                    if (amount <= 0.0) {
                        errorMessage = "Please enter a valid settlement amount"
                        return@Button
                    }
                    onConfirm(payerId, receiverId, amount, note.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MintAccent),
                modifier = Modifier.testTag("confirm_record_settlement_button")
            ) {
                Text("Record Settlement", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
