package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ParticipantEntity
import com.example.data.entity.SettlementEntity
import com.example.domain.model.DebtTransaction
import com.example.domain.model.ExpenseWithDetails
import com.example.domain.model.ParticipantBalance
import com.example.domain.model.SettlementWithDetails
import com.example.ui.components.AddOrEditParticipantDialog
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DebtItemCard
import com.example.ui.components.FormatHelper
import com.example.ui.components.ParticipantAvatar
import com.example.ui.theme.DarkMintCard
import com.example.ui.theme.DarkPeachCard
import com.example.ui.theme.FinancialOwePeach
import com.example.ui.theme.FinancialReceiveGreen
import com.example.ui.theme.FinancialSettledSlate
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MitmatCard
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PeachAccent
import com.example.ui.viewmodel.GroupDetailUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    state: GroupDetailUiState,
    onBackClick: () -> Unit,
    onAddExpenseClick: (Long) -> Unit,
    onEditExpenseClick: (ExpenseWithDetails) -> Unit,
    onDeleteExpenseClick: (Long) -> Unit,
    onSettleDebtClick: (DebtTransaction) -> Unit,
    onRecordCustomSettlementClick: (Long) -> Unit,
    onDeleteSettlementClick: (Long) -> Unit,
    onAddParticipant: (String) -> Unit,
    onEditParticipant: (ParticipantEntity) -> Unit,
    onDeleteParticipant: (ParticipantEntity) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val group = state.group ?: return
    val currency = group.defaultCurrency

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showAddParticipantDialog by remember { mutableStateOf(false) }
    var participantToEdit by remember { mutableStateOf<ParticipantEntity?>(null) }
    var participantToDelete by remember { mutableStateOf<ParticipantEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseWithDetails?>(null) }
    var settlementToDelete by remember { mutableStateOf<SettlementWithDetails?>(null) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }

    // Dialogs
    if (showAddParticipantDialog) {
        AddOrEditParticipantDialog(
            isEdit = false,
            onConfirm = { name ->
                onAddParticipant(name)
                showAddParticipantDialog = false
            },
            onDismiss = { showAddParticipantDialog = false }
        )
    }

    if (participantToEdit != null) {
        AddOrEditParticipantDialog(
            initialName = participantToEdit!!.name,
            isEdit = true,
            onConfirm = { newName ->
                onEditParticipant(participantToEdit!!.copy(name = newName))
                participantToEdit = null
            },
            onDismiss = { participantToEdit = null }
        )
    }

    if (participantToDelete != null) {
        ConfirmDeleteDialog(
            title = "Remove Participant",
            message = "Are you sure you want to remove '${participantToDelete!!.name}' from this group?",
            onConfirm = {
                onDeleteParticipant(participantToDelete!!)
                participantToDelete = null
            },
            onDismiss = { participantToDelete = null }
        )
    }

    if (expenseToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Expense",
            message = "Are you sure you want to delete '${expenseToDelete!!.expense.title}'? This will recalculate all group balances.",
            onConfirm = {
                onDeleteExpenseClick(expenseToDelete!!.expense.id)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }

    if (settlementToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Settlement",
            message = "Are you sure you want to delete the settlement of ${FormatHelper.formatMoney(settlementToDelete!!.settlement.amount, currency)} between ${settlementToDelete!!.payerName} and ${settlementToDelete!!.receiverName}?",
            onConfirm = {
                onDeleteSettlementClick(settlementToDelete!!.settlement.id)
                settlementToDelete = null
            },
            onDismiss = { settlementToDelete = null }
        )
    }

    if (showDeleteGroupDialog) {
        ConfirmDeleteDialog(
            title = "Delete Group",
            message = "Are you sure you want to delete '${group.name}'? All expenses, splits, and settlements will be permanently deleted.",
            onConfirm = {
                onDeleteGroup(group.id)
                showDeleteGroupDialog = false
                onBackClick()
            },
            onDismiss = { showDeleteGroupDialog = false }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Group Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("group_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${state.participants.size} members • Total ${FormatHelper.formatMoney(state.totalExpenseAmount, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showAddParticipantDialog = true },
                    modifier = Modifier.testTag("add_participant_icon")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = "Add Participant",
                        tint = LavenderAccent
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Member") },
                            onClick = {
                                showMenu = false
                                showAddParticipantDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Record Settlement") },
                            onClick = {
                                showMenu = false
                                onRecordCustomSettlementClick(group.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Group", color = PeachAccent) },
                            onClick = {
                                showMenu = false
                                showDeleteGroupDialog = true
                            }
                        )
                    }
                }
            }

            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = LavenderAccent
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Balances", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Expenses (${state.expenses.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Settlements (${state.settlements.size})", fontWeight = FontWeight.Bold) }
                )
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> BalancesTabContent(
                    balances = state.balances,
                    simplifiedDebts = state.simplifiedDebts,
                    currency = currency,
                    onSettleDebt = onSettleDebtClick,
                    onRecordSettlement = { onRecordCustomSettlementClick(group.id) },
                    onEditParticipant = { participantToEdit = it },
                    onDeleteParticipant = { participantToDelete = it }
                )
                1 -> ExpensesTabContent(
                    expenses = state.expenses,
                    currency = currency,
                    onExpenseClick = onEditExpenseClick,
                    onDeleteExpense = { expenseToDelete = it },
                    onAddExpenseClick = { onAddExpenseClick(group.id) }
                )
                2 -> SettlementsTabContent(
                    settlements = state.settlements,
                    currency = currency,
                    onDeleteSettlement = { settlementToDelete = it },
                    onRecordSettlement = { onRecordCustomSettlementClick(group.id) }
                )
            }
        }

        // Floating Action Button to add expense
        if (selectedTabIndex == 1 || selectedTabIndex == 0) {
            FloatingActionButton(
                onClick = { onAddExpenseClick(group.id) },
                shape = RoundedCornerShape(18.dp),
                containerColor = LavenderAccent,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("fab_group_add_expense")
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add Expense")
            }
        }
    }
}

@Composable
fun BalancesTabContent(
    balances: List<ParticipantBalance>,
    simplifiedDebts: List<DebtTransaction>,
    currency: String,
    onSettleDebt: (DebtTransaction) -> Unit,
    onRecordSettlement: () -> Unit,
    onEditParticipant: (ParticipantEntity) -> Unit,
    onDeleteParticipant: (ParticipantEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("balances_tab_content"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simplified Debts Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Who Owes Whom",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Optimized to minimize total transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (simplifiedDebts.isNotEmpty()) {
                    Button(
                        onClick = onRecordSettlement,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PastelMint,
                            contentColor = MintAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (simplifiedDebts.isEmpty()) {
            item {
                MitmatCard(
                    modifier = Modifier.fillMaxWidth(),
                    pastelColorLight = PastelMint.copy(alpha = 0.4f),
                    pastelColorDark = DarkMintCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MintAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = FinancialReceiveGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Everyone is even!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FinancialReceiveGreen
                            )
                            Text(
                                text = "No pending debts in this group.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(simplifiedDebts) { debt ->
                DebtItemCard(
                    debt = debt,
                    currency = currency,
                    onSettleClick = { onSettleDebt(debt) }
                )
            }
        }

        // Individual Balances Section
        item {
            Text(
                text = "Individual Net Balances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(balances, key = { it.participantId }) { balance ->
            MitmatCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ParticipantAvatar(
                        name = balance.name,
                        colorIndex = balance.avatarColorIndex,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = balance.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (balance.isCurrentUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PastelLavender)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("You", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LavenderAccent)
                                }
                            }
                        }
                        Text(
                            text = "Paid ${FormatHelper.formatMoney(balance.totalPaid, currency)} • Share ${FormatHelper.formatMoney(balance.totalShare, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val net = balance.netBalance
                    val (color, text) = when {
                        net > 0.01 -> Pair(FinancialReceiveGreen, "+${FormatHelper.formatMoney(net, currency)}")
                        net < -0.01 -> Pair(FinancialOwePeach, "-${FormatHelper.formatMoney(kotlin.math.abs(net), currency)}")
                        else -> Pair(FinancialSettledSlate, "Settled")
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = if (net > 0.01) "gets back" else if (net < -0.01) "owes" else "even",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun ExpensesTabContent(
    expenses: List<ExpenseWithDetails>,
    currency: String,
    onExpenseClick: (ExpenseWithDetails) -> Unit,
    onDeleteExpense: (ExpenseWithDetails) -> Unit,
    onAddExpenseClick: () -> Unit
) {
    if (expenses.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(PastelLavender),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Receipt,
                    contentDescription = null,
                    tint = LavenderAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Expenses Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add meals, bills, tickets, or rent to split with your group.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddExpenseClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent),
                modifier = Modifier.testTag("group_empty_add_expense_button")
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add First Expense")
            }
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("expenses_tab_content"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(expenses, key = { it.expense.id }) { item ->
                MitmatCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onExpenseClick(item) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBadge(categoryName = item.expense.category)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.expense.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Paid by ${item.payerName} • ${dateFormat.format(Date(item.expense.dateMillis))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.expense.note.isNotBlank()) {
                                Text(
                                    text = item.expense.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = FormatHelper.formatMoney(item.expense.totalAmount, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PastelLavender)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.expense.splitType.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LavenderAccent
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteExpense(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun SettlementsTabContent(
    settlements: List<SettlementWithDetails>,
    currency: String,
    onDeleteSettlement: (SettlementWithDetails) -> Unit,
    onRecordSettlement: () -> Unit
) {
    if (settlements.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(PastelMint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Handshake,
                    contentDescription = null,
                    tint = MintAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Settlements Recorded",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "When someone pays back a friend via cash, bKash, or bank, record it here to clear balances.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRecordSettlement,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MintAccent),
                modifier = Modifier.testTag("record_first_settlement_button")
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Record a Settlement")
            }
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settlements_tab_content"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Button(
                    onClick = onRecordSettlement,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintAccent)
                ) {
                    Icon(imageVector = Icons.Rounded.Handshake, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record New Settlement", fontWeight = FontWeight.Bold)
                }
            }

            items(settlements, key = { it.settlement.id }) { item ->
                MitmatCard(
                    modifier = Modifier.fillMaxWidth(),
                    pastelColorLight = PastelMint.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PastelMint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MintAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.payerName} paid ${item.receiverName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${dateFormat.format(Date(item.settlement.dateMillis))}${if (item.settlement.note.isNotBlank()) " • " + item.settlement.note else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = FormatHelper.formatMoney(item.settlement.amount, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MintAccent
                        )

                        IconButton(
                            onClick = { onDeleteSettlement(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}
