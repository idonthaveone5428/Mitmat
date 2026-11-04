package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.GroupEntity
import com.example.data.entity.ParticipantEntity
import com.example.domain.calculator.ExpenseCalculator
import com.example.domain.model.ExpenseWithDetails
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ExpenseCategory
import com.example.ui.components.FormatHelper
import com.example.ui.components.ParticipantAvatar
import java.util.Locale
import com.example.ui.theme.FinancialOwePeach
import com.example.ui.theme.FinancialReceiveGreen
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.MintAccent
import com.example.ui.theme.MitmatCard
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PeachAccent
import kotlin.math.abs

private fun formatNumberClean(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    groups: List<GroupEntity>,
    initialGroupId: Long?,
    participantsForGroup: List<ParticipantEntity>,
    onGroupSelected: (Long) -> Unit,
    onSaveExpense: (
        groupId: Long,
        title: String,
        amount: Double,
        payerId: Long,
        splitType: String,
        category: String,
        note: String,
        splits: Map<Long, Double>,
        percentages: Map<Long, Double>,
        payers: Map<Long, Double>
    ) -> Unit,
    onBackClick: () -> Unit,
    editingExpense: ExpenseWithDetails? = null,
    modifier: Modifier = Modifier
) {
    var selectedGroupId by remember {
        mutableStateOf(initialGroupId ?: editingExpense?.expense?.groupId ?: groups.firstOrNull()?.id ?: 0L)
    }
    val currentGroup = groups.firstOrNull { it.id == selectedGroupId }
    val currency = currentGroup?.defaultCurrency ?: "৳"

    var title by remember { mutableStateOf(editingExpense?.expense?.title ?: "") }
    var amountText by remember {
        mutableStateOf(
            editingExpense?.expense?.totalAmount?.let { if (it > 0) formatNumberClean(it) else "" } ?: ""
        )
    }
    var selectedCategory by remember { mutableStateOf(editingExpense?.expense?.category ?: ExpenseCategory.FOOD.title) }
    var note by remember { mutableStateOf(editingExpense?.expense?.note ?: "") }

    // Paid By: Single Member vs Multiple Members
    val initialIsMultiPayers = editingExpense?.let { it.payers.size > 1 } ?: false
    var isMultiplePayers by remember { mutableStateOf(initialIsMultiPayers) }

    val initialSinglePayerId = when {
        editingExpense?.payers?.isNotEmpty() == true -> editingExpense.payers.first().payer.participantId
        editingExpense?.expense?.paidByParticipantId != null && editingExpense.expense.paidByParticipantId > 0L -> editingExpense.expense.paidByParticipantId
        participantsForGroup.isNotEmpty() -> participantsForGroup.first().id
        else -> 0L
    }
    var selectedSinglePayerId by remember { mutableStateOf(initialSinglePayerId) }

    // Multiple payers: participantId -> entered amount string
    val multiPayerAmounts = remember {
        mutableStateMapOf<Long, String>().apply {
            editingExpense?.payers?.forEach { p ->
                if (p.payer.amount > 0) {
                    put(p.payer.participantId, formatNumberClean(p.payer.amount))
                }
            }
            if (editingExpense != null && editingExpense.payers.isEmpty() && editingExpense.expense.paidByParticipantId > 0L && editingExpense.expense.totalAmount > 0) {
                put(editingExpense.expense.paidByParticipantId, formatNumberClean(editingExpense.expense.totalAmount))
            }
        }
    }

    // Split mode: 0 = EQUAL, 1 = CUSTOM, 2 = PERCENTAGE
    val initialSplitIndex = when (editingExpense?.expense?.splitType) {
        "CUSTOM" -> 1
        "PERCENTAGE" -> 2
        else -> 0
    }
    var splitModeIndex by remember { mutableIntStateOf(initialSplitIndex) }

    // Equal Split: map of participantId -> Boolean (selected)
    val equalSelections = remember {
        mutableStateMapOf<Long, Boolean>().apply {
            if (editingExpense != null && editingExpense.splits.isNotEmpty()) {
                val includedIds = editingExpense.splits.map { it.split.participantId }.toSet()
                participantsForGroup.forEach { p ->
                    put(p.id, p.id in includedIds)
                }
            }
        }
    }

    // Custom Split: map of participantId -> amount string
    val customAmounts = remember {
        mutableStateMapOf<Long, String>().apply {
            editingExpense?.splits?.forEach { s ->
                if (s.split.shareAmount > 0) {
                    put(s.split.participantId, formatNumberClean(s.split.shareAmount))
                }
            }
        }
    }

    // Percentage Split: map of participantId -> percentage string
    val percentages = remember {
        mutableStateMapOf<Long, String>().apply {
            editingExpense?.splits?.forEach { s ->
                if (s.split.percentage > 0) {
                    put(s.split.participantId, formatNumberClean(s.split.percentage))
                }
            }
        }
    }

    // Sync participants when group changes or initial load
    LaunchedEffect(participantsForGroup) {
        if (participantsForGroup.isNotEmpty()) {
            if (editingExpense != null) {
                // Preserving the payer and splits when editing
                val targetPayerId = when {
                    editingExpense.payers.isNotEmpty() -> editingExpense.payers.first().payer.participantId
                    editingExpense.expense.paidByParticipantId > 0L -> editingExpense.expense.paidByParticipantId
                    else -> 0L
                }
                if (targetPayerId > 0L && participantsForGroup.any { it.id == targetPayerId }) {
                    selectedSinglePayerId = targetPayerId
                } else if (selectedSinglePayerId == 0L || participantsForGroup.none { it.id == selectedSinglePayerId }) {
                    selectedSinglePayerId = participantsForGroup.first().id
                }

                if (editingExpense.payers.size > 1) {
                    isMultiplePayers = true
                }
                editingExpense.payers.forEach { py ->
                    if (py.payer.amount > 0 && !multiPayerAmounts.containsKey(py.payer.participantId)) {
                        multiPayerAmounts[py.payer.participantId] = formatNumberClean(py.payer.amount)
                    }
                }

                if (equalSelections.isEmpty()) {
                    if (editingExpense.splits.isNotEmpty()) {
                        val includedIds = editingExpense.splits.map { it.split.participantId }.toSet()
                        participantsForGroup.forEach { p ->
                            equalSelections[p.id] = p.id in includedIds
                        }
                    } else {
                        participantsForGroup.forEach { p ->
                            equalSelections[p.id] = true
                        }
                    }
                } else {
                    val includedIds = editingExpense.splits.map { it.split.participantId }.toSet()
                    participantsForGroup.forEach { p ->
                        if (!equalSelections.containsKey(p.id)) {
                            equalSelections[p.id] = if (includedIds.isNotEmpty()) p.id in includedIds else true
                        }
                    }
                }

                if (customAmounts.isEmpty()) {
                    editingExpense.splits.forEach { s ->
                        if (s.split.shareAmount > 0) {
                            customAmounts[s.split.participantId] = formatNumberClean(s.split.shareAmount)
                        }
                    }
                }

                if (percentages.isEmpty()) {
                    editingExpense.splits.forEach { s ->
                        if (s.split.percentage > 0) {
                            percentages[s.split.participantId] = formatNumberClean(s.split.percentage)
                        }
                    }
                }
            } else {
                if (selectedSinglePayerId == 0L || participantsForGroup.none { it.id == selectedSinglePayerId }) {
                    selectedSinglePayerId = participantsForGroup.first().id
                }
                if (equalSelections.isEmpty()) {
                    participantsForGroup.forEach { p ->
                        equalSelections[p.id] = true
                    }
                }
                if (percentages.isEmpty()) {
                    val defaultPct = if (participantsForGroup.isNotEmpty()) {
                        ExpenseCalculator.roundTo2Decimals(100.0 / participantsForGroup.size).toString()
                    } else "0"
                    participantsForGroup.forEach { p ->
                        percentages[p.id] = defaultPct
                    }
                }
            }
        }
    }

    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var singlePayerDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Calculations for Total & Multiple Payers
    val totalPaidByMembers = multiPayerAmounts.values.mapNotNull { it.toDoubleOrNull() }.filter { it > 0 }.sum()
    val effectiveTotalAmount = if (isMultiplePayers) {
        ExpenseCalculator.roundTo2Decimals(totalPaidByMembers)
    } else {
        amountText.toDoubleOrNull() ?: 0.0
    }

    // Calculations based on split mode
    val splitRemainingToAllocate = remember(effectiveTotalAmount, splitModeIndex, customAmounts.toMap(), percentages.toMap()) {
        when (splitModeIndex) {
            1 -> { // Custom Split
                val enteredSum = customAmounts.values.mapNotNull { it.toDoubleOrNull() }.sum()
                ExpenseCalculator.roundTo2Decimals(effectiveTotalAmount - enteredSum)
            }
            2 -> { // Percentage Split
                val enteredPct = percentages.values.mapNotNull { it.toDoubleOrNull() }.sum()
                ExpenseCalculator.roundTo2Decimals(100.0 - enteredPct)
            }
            else -> 0.0
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("add_expense_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("add_expense_back_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingExpense != null) "Edit Expense" else "Add New Expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Group Selector
        item {
            if (groups.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = groupDropdownExpanded,
                    onExpandedChange = { groupDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentGroup?.name ?: "Select Group",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Group") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = groupDropdownExpanded,
                        onDismissRequest = { groupDropdownExpanded = false }
                    ) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text("${group.name} (${group.defaultCurrency})") },
                                onClick = {
                                    selectedGroupId = group.id
                                    onGroupSelected(group.id)
                                    groupDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Title & Amount
        item {
            MitmatCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            validationError = null
                        },
                        label = { Text("Expense Title (e.g. Dinner, Groceries)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_title_input")
                    )

                    OutlinedTextField(
                        value = if (isMultiplePayers) {
                            if (effectiveTotalAmount > 0) effectiveTotalAmount.toString() else ""
                        } else {
                            amountText
                        },
                        onValueChange = {
                            if (!isMultiplePayers) {
                                amountText = it
                                validationError = null
                            }
                        },
                        readOnly = isMultiplePayers,
                        label = {
                            Text(
                                if (isMultiplePayers) "Total Bill Amount (Auto-calculated)" else "Total Bill Amount ($currency)"
                            )
                        },
                        supportingText = if (isMultiplePayers) {
                            {
                                Text(
                                    "Summed automatically from member payments below (${FormatHelper.formatMoney(effectiveTotalAmount, currency)})",
                                    color = LavenderAccent,
                                    fontSize = 11.sp
                                )
                            }
                        } else null,
                        prefix = { Text("$currency ", fontWeight = FontWeight.Bold, color = LavenderAccent) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_amount_input")
                    )
                }
            }
        }

        // PAID BY SECTION (Single Member vs Multiple Members)
        item {
            MitmatCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paid By",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Segmented Toggle for Single vs Multiple Payers
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !isMultiplePayers,
                                onClick = {
                                    if (isMultiplePayers && effectiveTotalAmount > 0) {
                                        amountText = effectiveTotalAmount.toString()
                                    }
                                    isMultiplePayers = false
                                },
                                label = { Text("Single Person", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PastelLavender,
                                    selectedLabelColor = LavenderAccent
                                )
                            )
                            FilterChip(
                                selected = isMultiplePayers,
                                onClick = {
                                    isMultiplePayers = true
                                    val currentSingleAmt = amountText.toDoubleOrNull() ?: 0.0
                                    if (multiPayerAmounts.isEmpty() && currentSingleAmt > 0 && selectedSinglePayerId != 0L) {
                                        multiPayerAmounts[selectedSinglePayerId] = currentSingleAmt.toString()
                                    }
                                },
                                label = { Text("Multiple Members", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Rounded.Group, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PastelLavender,
                                    selectedLabelColor = LavenderAccent
                                )
                            )
                        }
                    }

                    if (!isMultiplePayers) {
                        // Single Payer Dropdown
                        val payer = participantsForGroup.firstOrNull { it.id == selectedSinglePayerId }

                        ExposedDropdownMenuBox(
                            expanded = singlePayerDropdownExpanded,
                            onExpandedChange = { singlePayerDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = payer?.name ?: "Select Payer",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Who Paid the Bill?") },
                                leadingIcon = {
                                    payer?.let {
                                        ParticipantAvatar(name = it.name, colorIndex = it.avatarColorIndex, size = 28.dp)
                                    }
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = singlePayerDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("paid_by_selector"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = singlePayerDropdownExpanded,
                                onDismissRequest = { singlePayerDropdownExpanded = false }
                            ) {
                                participantsForGroup.forEach { p ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ParticipantAvatar(name = p.name, colorIndex = p.avatarColorIndex, size = 26.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(p.name + if (p.isCurrentUser) " (You)" else "")
                                            }
                                        },
                                        onClick = {
                                            selectedSinglePayerId = p.id
                                            singlePayerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Multiple Payers Input List (e.g. A paid 200, B paid 400)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enter how much each person paid:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Total Sum: ${FormatHelper.formatMoney(effectiveTotalAmount, currency)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (effectiveTotalAmount > 0) FinancialReceiveGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            participantsForGroup.forEach { participant ->
                                val currentVal = multiPayerAmounts[participant.id] ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        ParticipantAvatar(
                                            name = participant.name,
                                            colorIndex = participant.avatarColorIndex,
                                            size = 32.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = participant.name + if (participant.isCurrentUser) " (You)" else "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    OutlinedTextField(
                                        value = currentVal,
                                        onValueChange = {
                                            multiPayerAmounts[participant.id] = it
                                            validationError = null
                                        },
                                        placeholder = { Text("0.00") },
                                        prefix = { Text("$currency ") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.width(140.dp)
                                    )
                                }
                            }

                            // Quick split equally among participants for paid amount
                            OutlinedButton(
                                onClick = {
                                    val baseAmt = if (effectiveTotalAmount > 0) effectiveTotalAmount else (amountText.toDoubleOrNull() ?: 0.0)
                                    if (baseAmt > 0 && participantsForGroup.isNotEmpty()) {
                                        val equalShare = ExpenseCalculator.calculateEqualSplits(baseAmt, participantsForGroup.map { it.id })
                                        equalShare.forEach { (pId, amt) ->
                                            multiPayerAmounts[pId] = amt.toString()
                                        }
                                        validationError = null
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Split Paid Equally", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Category Picker
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ExpenseCategory.entries) { cat ->
                        val isSelected = selectedCategory.equals(cat.title, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat.title },
                            label = { Text(cat.title, fontSize = 12.sp) },
                            leadingIcon = {
                                CategoryBadge(categoryName = cat.title, size = 24.dp, iconSize = 14.dp)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PastelLavender,
                                selectedLabelColor = LavenderAccent
                            )
                        )
                    }
                }
            }
        }

        // Split Mode Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Split Method (Who Shares the Bill)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                PrimaryTabRow(
                    selectedTabIndex = splitModeIndex,
                    containerColor = Color.Transparent,
                    contentColor = LavenderAccent
                ) {
                    Tab(
                        selected = splitModeIndex == 0,
                        onClick = { splitModeIndex = 0 },
                        text = { Text("Equal", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = splitModeIndex == 1,
                        onClick = { splitModeIndex = 1 },
                        text = { Text("Exact Amounts", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = splitModeIndex == 2,
                        onClick = { splitModeIndex = 2 },
                        text = { Text("Percentage", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        // Participants Split Configuration
        item {
            MitmatCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (splitModeIndex) {
                        0 -> {
                            // EQUAL SPLIT
                            val selectedParticipants = participantsForGroup.filter { equalSelections[it.id] == true }
                            val calculatedShares = ExpenseCalculator.calculateEqualSplits(
                                effectiveTotalAmount,
                                selectedParticipants.map { it.id }
                            )

                            Text(
                                text = "Select participants involved in this bill:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            participantsForGroup.forEach { participant ->
                                val isSelected = equalSelections[participant.id] ?: false
                                val share = calculatedShares[participant.id] ?: 0.0

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { equalSelections[participant.id] = !isSelected }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { equalSelections[participant.id] = it },
                                            colors = CheckboxDefaults.colors(checkedColor = LavenderAccent)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        ParticipantAvatar(
                                            name = participant.name,
                                            colorIndex = participant.avatarColorIndex,
                                            size = 32.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = participant.name + if (participant.isCurrentUser) " (You)" else "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (isSelected && effectiveTotalAmount > 0) {
                                        Text(
                                            text = FormatHelper.formatMoney(share, currency),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = LavenderAccent
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            // CUSTOM / EXACT SPLIT
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enter exact amount per person:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (abs(splitRemainingToAllocate) < 0.01) "Balanced ($currency 0.00)" else "Remaining: ${FormatHelper.formatMoney(splitRemainingToAllocate, currency)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (abs(splitRemainingToAllocate) < 0.01) FinancialReceiveGreen else PeachAccent
                                )
                            }

                            participantsForGroup.forEach { participant ->
                                val currentVal = customAmounts[participant.id] ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        ParticipantAvatar(
                                            name = participant.name,
                                            colorIndex = participant.avatarColorIndex,
                                            size = 32.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = participant.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    OutlinedTextField(
                                        value = currentVal,
                                        onValueChange = { customAmounts[participant.id] = it },
                                        placeholder = { Text("0.00") },
                                        prefix = { Text("$currency ") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.width(140.dp)
                                    )
                                }
                            }
                        }

                        2 -> {
                            // PERCENTAGE SPLIT
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enter percentage per person:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (abs(splitRemainingToAllocate) < 0.1) "Balanced (100%)" else "Remaining: $splitRemainingToAllocate%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (abs(splitRemainingToAllocate) < 0.1) FinancialReceiveGreen else PeachAccent
                                )
                            }

                            participantsForGroup.forEach { participant ->
                                val pctVal = percentages[participant.id] ?: ""
                                val pctDouble = pctVal.toDoubleOrNull() ?: 0.0
                                val calculatedShare = effectiveTotalAmount * (pctDouble / 100.0)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        ParticipantAvatar(
                                            name = participant.name,
                                            colorIndex = participant.avatarColorIndex,
                                            size = 32.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = participant.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (effectiveTotalAmount > 0 && pctDouble > 0) {
                                                Text(
                                                    text = FormatHelper.formatMoney(calculatedShare, currency),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = LavenderAccent
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = pctVal,
                                        onValueChange = { percentages[participant.id] = it },
                                        placeholder = { Text("0") },
                                        suffix = { Text("%") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.width(110.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Optional Note
        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Optional Note") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_note_input")
            )
        }

        // Validation Error Message
        if (validationError != null) {
            item {
                Text(
                    text = validationError!!,
                    color = PeachAccent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    if (title.trim().isBlank()) {
                        validationError = "Please enter an expense title"
                        return@Button
                    }
                    if (effectiveTotalAmount <= 0.0) {
                        validationError = if (isMultiplePayers) {
                            "Please enter paid amounts greater than 0"
                        } else {
                            "Please enter a valid positive amount"
                        }
                        return@Button
                    }

                    // Payer handling
                    val finalPayers = mutableMapOf<Long, Double>()
                    var finalPayerId = selectedSinglePayerId

                    if (isMultiplePayers) {
                        for (p in participantsForGroup) {
                            val paidAmt = multiPayerAmounts[p.id]?.toDoubleOrNull() ?: 0.0
                            if (paidAmt < 0) {
                                validationError = "Paid amounts cannot be negative"
                                return@Button
                            }
                            if (paidAmt > 0) {
                                finalPayers[p.id] = paidAmt
                            }
                        }
                        if (finalPayers.isEmpty()) {
                            validationError = "Please enter the amount paid by at least one member"
                            return@Button
                        }
                        finalPayerId = finalPayers.keys.first()
                    } else {
                        if (selectedSinglePayerId == 0L) {
                            validationError = "Please select who paid"
                            return@Button
                        }
                        finalPayers[selectedSinglePayerId] = effectiveTotalAmount
                    }

                    // Split handling
                    val finalSplits = mutableMapOf<Long, Double>()
                    val finalPercentages = mutableMapOf<Long, Double>()

                    when (splitModeIndex) {
                        0 -> {
                            val selectedIds = participantsForGroup
                                .filter { equalSelections[it.id] == true }
                                .map { it.id }

                            if (selectedIds.isEmpty()) {
                                validationError = "Please select at least one participant to split with"
                                return@Button
                            }

                            val calculated = ExpenseCalculator.calculateEqualSplits(effectiveTotalAmount, selectedIds)
                            finalSplits.putAll(calculated)
                        }

                        1 -> {
                            var sum = 0.0
                            for (p in participantsForGroup) {
                                val s = customAmounts[p.id]?.toDoubleOrNull() ?: 0.0
                                if (s < 0) {
                                    validationError = "Split amounts cannot be negative"
                                    return@Button
                                }
                                if (s > 0) {
                                    finalSplits[p.id] = s
                                    sum += s
                                }
                            }

                            if (abs(sum - effectiveTotalAmount) > 0.05) {
                                validationError = "Split shares (${FormatHelper.formatMoney(sum, currency)}) must equal total (${FormatHelper.formatMoney(effectiveTotalAmount, currency)})"
                                return@Button
                            }
                        }

                        2 -> {
                            var sumPct = 0.0
                            val pctMap = mutableMapOf<Long, Double>()
                            for (p in participantsForGroup) {
                                val pct = percentages[p.id]?.toDoubleOrNull() ?: 0.0
                                if (pct < 0) {
                                    validationError = "Percentages cannot be negative"
                                    return@Button
                                }
                                if (pct > 0) {
                                    pctMap[p.id] = pct
                                    sumPct += pct
                                }
                            }

                            if (abs(sumPct - 100.0) > 0.5) {
                                validationError = "Percentages must sum to 100% (currently $sumPct%)"
                                return@Button
                            }

                            finalPercentages.putAll(pctMap)
                            val calculated = ExpenseCalculator.calculatePercentageSplits(effectiveTotalAmount, pctMap)
                            finalSplits.putAll(calculated)
                        }
                    }

                    val splitTypeName = when (splitModeIndex) {
                        1 -> "CUSTOM"
                        2 -> "PERCENTAGE"
                        else -> "EQUAL"
                    }

                    onSaveExpense(
                        selectedGroupId,
                        title.trim(),
                        effectiveTotalAmount,
                        finalPayerId,
                        splitTypeName,
                        selectedCategory,
                        note.trim(),
                        finalSplits,
                        finalPercentages,
                        finalPayers
                    )
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_expense_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent)
            ) {
                Icon(imageVector = Icons.Rounded.Done, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingExpense != null) "Update Expense" else "Save Expense",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
