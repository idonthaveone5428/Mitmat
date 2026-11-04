package com.example.data.repository

import com.example.data.backup.DataBackupManager
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ExpensePayerDao
import com.example.data.dao.ExpenseSplitDao
import com.example.data.dao.GroupDao
import com.example.data.dao.ParticipantDao
import com.example.data.dao.SettlementDao
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpensePayerEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.GroupEntity
import com.example.data.entity.ParticipantEntity
import com.example.data.entity.SettlementEntity
import com.example.domain.calculator.ExpenseCalculator
import com.example.domain.model.ActivityFeedItem
import com.example.domain.model.ExpensePayerWithParticipant
import com.example.domain.model.ExpenseSplitWithParticipant
import com.example.domain.model.ExpenseWithDetails
import com.example.domain.model.GroupSummary
import com.example.domain.model.SettlementWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class MitmatRepository(
    private val groupDao: GroupDao,
    private val participantDao: ParticipantDao,
    private val expenseDao: ExpenseDao,
    private val expensePayerDao: ExpensePayerDao,
    private val expenseSplitDao: ExpenseSplitDao,
    private val settlementDao: SettlementDao
) {
    private val backupManager = DataBackupManager(
        groupDao = groupDao,
        participantDao = participantDao,
        expenseDao = expenseDao,
        expensePayerDao = expensePayerDao,
        expenseSplitDao = expenseSplitDao,
        settlementDao = settlementDao
    )

    // Active, unarchived, non-trashed groups for dashboard and regular group views
    val allGroups: Flow<List<GroupEntity>> = groupDao.getAllGroups()
    val archivedGroups: Flow<List<GroupEntity>> = groupDao.getArchivedGroups()
    val trashedGroups: Flow<List<GroupEntity>> = groupDao.getTrashedGroups()

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allSplits: Flow<List<ExpenseSplitEntity>> = expenseSplitDao.getAllSplits()
    val allExpensePayers: Flow<List<ExpensePayerEntity>> = expensePayerDao.getAllExpensePayers()
    val allSettlements: Flow<List<SettlementEntity>> = settlementDao.getAllSettlements()
    val allParticipants: Flow<List<ParticipantEntity>> = participantDao.getAllParticipants()

    fun getGroupById(groupId: Long): Flow<GroupEntity?> = groupDao.getGroupById(groupId)
    fun getParticipantsForGroup(groupId: Long): Flow<List<ParticipantEntity>> =
        participantDao.getParticipantsForGroup(groupId)
    fun getExpensesForGroup(groupId: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesForGroup(groupId)
    fun getSettlementsForGroup(groupId: Long): Flow<List<SettlementEntity>> =
        settlementDao.getSettlementsForGroup(groupId)

    private data class ExpenseFullData(
        val expenses: List<ExpenseEntity>,
        val splits: List<ExpenseSplitEntity>,
        val payers: List<ExpensePayerEntity>
    )

    private val expenseFullDataFlow = combine(
        allExpenses,
        allSplits,
        allExpensePayers
    ) { exp, splits, payers ->
        ExpenseFullData(exp, splits, payers)
    }

    // Flow of group summaries for active groups
    val groupSummaries: Flow<List<GroupSummary>> = combine(
        allGroups,
        allParticipants,
        expenseFullDataFlow,
        allSettlements
    ) { groups, participants, expData, settlements ->
        val participantsByGroup = participants.groupBy { it.groupId }
        val expensesByGroup = expData.expenses.groupBy { it.groupId }
        val settlementsByGroup = settlements.groupBy { it.groupId }

        groups.map { group ->
            val groupParticipants = participantsByGroup[group.id] ?: emptyList()
            val groupExpenses = expensesByGroup[group.id] ?: emptyList()
            val groupSettlements = settlementsByGroup[group.id] ?: emptyList()

            val expenseIds = groupExpenses.map { it.id }.toSet()
            val groupSplits = expData.splits.filter { it.expenseId in expenseIds }
            val groupPayers = expData.payers.filter { it.expenseId in expenseIds }

            val balances = ExpenseCalculator.calculateBalances(
                participants = groupParticipants,
                expenses = groupExpenses,
                splits = groupSplits,
                settlements = groupSettlements,
                payers = groupPayers
            )

            val currentUserBalance = balances.firstOrNull { it.isCurrentUser }?.netBalance
                ?: balances.firstOrNull()?.netBalance ?: 0.0

            val simplified = ExpenseCalculator.simplifyDebts(balances)
            val totalExpenseAmount = groupExpenses.sumOf { it.totalAmount }

            GroupSummary(
                group = group,
                participants = groupParticipants,
                totalExpenses = ExpenseCalculator.roundTo2Decimals(totalExpenseAmount),
                userNetBalance = ExpenseCalculator.roundTo2Decimals(currentUserBalance),
                totalSimplifiedDebtsCount = simplified.size
            )
        }
    }

    // Recent activity feed across active groups
    val activityFeed: Flow<List<ActivityFeedItem>> = combine(
        groupDao.getAllActiveAndArchivedGroups(),
        allParticipants,
        expenseFullDataFlow,
        allSettlements
    ) { groups, participants, expData, settlements ->
        val groupMap = groups.associateBy { it.id }
        val participantMap = participants.associateBy { it.id }

        val items = mutableListOf<ActivityFeedItem>()

        for (exp in expData.expenses) {
            val group = groupMap[exp.groupId] ?: continue
            val expPayers = expData.payers.filter { it.expenseId == exp.id }

            val payerName = if (expPayers.size > 1) {
                expPayers.joinToString(", ") {
                    val p = participantMap[it.participantId]
                    "${p?.name ?: "Unknown"} (${group.defaultCurrency}${it.amount.toInt()})"
                }
            } else {
                val singleId = expPayers.firstOrNull()?.participantId ?: exp.paidByParticipantId
                participantMap[singleId]?.name ?: "Someone"
            }

            items.add(
                ActivityFeedItem.ExpenseFeed(
                    id = "exp_${exp.id}",
                    groupId = exp.groupId,
                    groupName = group.name,
                    currency = group.defaultCurrency,
                    dateMillis = exp.dateMillis,
                    title = exp.title,
                    subtitle = "Paid by $payerName",
                    amount = exp.totalAmount,
                    category = exp.category,
                    payerName = payerName,
                    expenseId = exp.id
                )
            )
        }

        for (set in settlements) {
            val group = groupMap[set.groupId] ?: continue
            val payerName = participantMap[set.payerParticipantId]?.name ?: "Someone"
            val receiverName = participantMap[set.receiverParticipantId]?.name ?: "Someone"

            items.add(
                ActivityFeedItem.SettlementFeed(
                    id = "set_${set.id}",
                    groupId = set.groupId,
                    groupName = group.name,
                    currency = group.defaultCurrency,
                    dateMillis = set.dateMillis,
                    title = "Settlement",
                    subtitle = "$payerName paid $receiverName",
                    amount = set.amount,
                    payerName = payerName,
                    receiverName = receiverName,
                    settlementId = set.id
                )
            )
        }

        items.sortedByDescending { it.dateMillis }
    }

    suspend fun createGroup(
        name: String,
        currency: String = "৳",
        colorIndex: Int = 0,
        participantNames: List<String>
    ): Long {
        val group = GroupEntity(
            name = name.trim(),
            defaultCurrency = currency.trim(),
            colorIndex = colorIndex
        )
        val groupId = groupDao.insertGroup(group)

        val cleanNames = participantNames.map { it.trim() }.filter { it.isNotBlank() }
        val participantsToInsert = mutableListOf<ParticipantEntity>()

        if (cleanNames.none { it.equals("You", ignoreCase = true) }) {
            participantsToInsert.add(
                ParticipantEntity(
                    groupId = groupId,
                    name = "You",
                    isCurrentUser = true,
                    avatarColorIndex = 0
                )
            )
        }

        cleanNames.forEachIndexed { index, nameStr ->
            val isYou = nameStr.equals("You", ignoreCase = true)
            participantsToInsert.add(
                ParticipantEntity(
                    groupId = groupId,
                    name = nameStr,
                    isCurrentUser = isYou,
                    avatarColorIndex = (index + 1) % 6
                )
            )
        }

        participantDao.insertParticipants(participantsToInsert)
        return groupId
    }

    suspend fun updateGroup(group: GroupEntity) {
        groupDao.updateGroup(group)
    }

    // Soft delete: move group to Trash Bin
    suspend fun trashGroup(groupId: Long) {
        val group = groupDao.getGroupByIdOnce(groupId) ?: return
        groupDao.updateGroup(group.copy(isTrashed = true, trashedAt = System.currentTimeMillis()))
    }

    // Restore from Trash Bin
    suspend fun restoreGroup(groupId: Long) {
        val group = groupDao.getGroupByIdOnce(groupId) ?: return
        groupDao.updateGroup(group.copy(isTrashed = false, trashedAt = null))
    }

    // Toggle Archive status
    suspend fun archiveGroup(groupId: Long, archive: Boolean) {
        val group = groupDao.getGroupByIdOnce(groupId) ?: return
        groupDao.updateGroup(group.copy(isArchived = archive))
    }

    // Permanent Deletion
    suspend fun permanentlyDeleteGroup(groupId: Long) {
        val expenses = expenseDao.getExpensesForGroupOnce(groupId)
        for (exp in expenses) {
            expenseSplitDao.deleteSplitsForExpense(exp.id)
            expensePayerDao.deletePayersForExpense(exp.id)
        }
        expenseDao.deleteExpensesForGroup(groupId)
        settlementDao.deleteSettlementsForGroup(groupId)
        participantDao.deleteParticipantsForGroup(groupId)
        groupDao.deleteGroupById(groupId)
    }

    // Empty entire trash
    suspend fun emptyTrash() {
        val trashed = groupDao.getTrashedGroups()
        // We delete by fetching once
        val all = groupDao.getAllGroupsRawOnce().filter { it.isTrashed }
        for (g in all) {
            permanentlyDeleteGroup(g.id)
        }
    }

    // 7-day automatic cleanup of trashed items
    suspend fun purgeOldTrashedGroups() {
        val cutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val all = groupDao.getAllGroupsRawOnce().filter { it.isTrashed && (it.trashedAt ?: 0L) < cutoff }
        for (g in all) {
            permanentlyDeleteGroup(g.id)
        }
    }

    // Remove any previously seeded demo groups ("Sylhet Trip", "Flatmates")
    suspend fun purgeDemoDataIfPresent() {
        val all = groupDao.getAllGroupsRawOnce().filter {
            it.name.equals("Sylhet Trip", ignoreCase = true) ||
            it.name.equals("Flatmates", ignoreCase = true)
        }
        for (g in all) {
            permanentlyDeleteGroup(g.id)
        }
    }

    suspend fun seedDemoData() {
        val existing = groupDao.getAllGroupsRawOnce()
        if (existing.any { it.name.equals("Sylhet Trip", ignoreCase = true) }) {
            return
        }

        val groupId = createGroup(
            name = "Sylhet Trip",
            currency = "৳",
            colorIndex = 1,
            participantNames = listOf("You", "Rakib", "Tanvir", "Siam")
        )
        val participants = participantDao.getParticipantsForGroupOnce(groupId)
        val you = participants.firstOrNull { it.isCurrentUser } ?: participants[0]
        val rakib = participants.getOrNull(1) ?: you
        val tanvir = participants.getOrNull(2) ?: you
        val siam = participants.getOrNull(3) ?: you

        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        // Expense 1: Resort Booking (Paid by You, split equally)
        val resortAmount = 12000.0
        val splitPerPerson = resortAmount / participants.size
        addExpense(
            groupId = groupId,
            title = "Resort Booking",
            totalAmount = resortAmount,
            paidByParticipantId = you.id,
            splitType = "EQUAL",
            category = "Travel",
            note = "2 nights stay at Grand Sultan",
            dateMillis = now - 2 * dayMillis,
            splits = participants.associate { it.id to splitPerPerson }
        )

        // Expense 2: Highway Dinner (Paid by Rakib, split equally)
        val dinnerAmount = 3200.0
        val dinnerSplit = dinnerAmount / participants.size
        addExpense(
            groupId = groupId,
            title = "Highway Dinner",
            totalAmount = dinnerAmount,
            paidByParticipantId = rakib.id,
            splitType = "EQUAL",
            category = "Food",
            note = "Pansi restaurant dinner",
            dateMillis = now - dayMillis,
            splits = participants.associate { it.id to dinnerSplit }
        )

        // Expense 3: Boat Tour (Paid by Tanvir, split equally)
        val boatAmount = 2400.0
        val boatSplit = boatAmount / participants.size
        addExpense(
            groupId = groupId,
            title = "Ratargul Boat Tour",
            totalAmount = boatAmount,
            paidByParticipantId = tanvir.id,
            splitType = "EQUAL",
            category = "Entertainment",
            note = "Boat rental for 4",
            dateMillis = now - (dayMillis / 2),
            splits = participants.associate { it.id to boatSplit }
        )

        // Settlement 1: Siam paid You ৳2500
        addSettlement(
            groupId = groupId,
            payerId = siam.id,
            receiverId = you.id,
            amount = 2500.0,
            note = "bKash transfer for resort",
            dateMillis = now - (dayMillis / 4)
        )
    }

    suspend fun addParticipant(groupId: Long, name: String, isCurrentUser: Boolean = false) {
        val count = participantDao.getParticipantsForGroupOnce(groupId).size
        participantDao.insertParticipant(
            ParticipantEntity(
                groupId = groupId,
                name = name.trim(),
                isCurrentUser = isCurrentUser,
                avatarColorIndex = count % 6
            )
        )
    }

    suspend fun updateParticipant(participant: ParticipantEntity) {
        participantDao.updateParticipant(participant)
    }

    suspend fun deleteParticipant(participant: ParticipantEntity) {
        participantDao.deleteParticipant(participant)
    }

    suspend fun addExpense(
        groupId: Long,
        title: String,
        totalAmount: Double,
        paidByParticipantId: Long,
        splitType: String,
        category: String,
        note: String,
        dateMillis: Long,
        splits: Map<Long, Double>,
        percentages: Map<Long, Double> = emptyMap(),
        payers: Map<Long, Double> = emptyMap()
    ): Long {
        val primaryPayerId = if (payers.isNotEmpty()) {
            payers.keys.first()
        } else {
            paidByParticipantId
        }

        val expense = ExpenseEntity(
            groupId = groupId,
            title = title.trim(),
            totalAmount = ExpenseCalculator.roundTo2Decimals(totalAmount),
            paidByParticipantId = primaryPayerId,
            splitType = splitType,
            category = category,
            note = note.trim(),
            dateMillis = dateMillis
        )
        val expenseId = expenseDao.insertExpense(expense)

        // Insert Splits
        val splitEntities = splits.map { (participantId, shareAmount) ->
            ExpenseSplitEntity(
                expenseId = expenseId,
                participantId = participantId,
                shareAmount = ExpenseCalculator.roundTo2Decimals(shareAmount),
                percentage = percentages[participantId] ?: 0.0
            )
        }
        expenseSplitDao.insertSplits(splitEntities)

        // Insert Payers
        val payerEntities = if (payers.isNotEmpty()) {
            payers.map { (participantId, paidAmt) ->
                ExpensePayerEntity(
                    expenseId = expenseId,
                    participantId = participantId,
                    amount = ExpenseCalculator.roundTo2Decimals(paidAmt)
                )
            }
        } else {
            listOf(
                ExpensePayerEntity(
                    expenseId = expenseId,
                    participantId = primaryPayerId,
                    amount = ExpenseCalculator.roundTo2Decimals(totalAmount)
                )
            )
        }
        expensePayerDao.insertPayers(payerEntities)

        return expenseId
    }

    suspend fun updateExpense(
        expense: ExpenseEntity,
        splits: Map<Long, Double>,
        percentages: Map<Long, Double> = emptyMap(),
        payers: Map<Long, Double> = emptyMap()
    ) {
        val primaryPayerId = if (payers.isNotEmpty()) payers.keys.first() else expense.paidByParticipantId
        val updatedExpense = expense.copy(paidByParticipantId = primaryPayerId)

        expenseDao.updateExpense(updatedExpense)

        // Update Splits
        expenseSplitDao.deleteSplitsForExpense(expense.id)
        val splitEntities = splits.map { (participantId, shareAmount) ->
            ExpenseSplitEntity(
                expenseId = expense.id,
                participantId = participantId,
                shareAmount = ExpenseCalculator.roundTo2Decimals(shareAmount),
                percentage = percentages[participantId] ?: 0.0
            )
        }
        expenseSplitDao.insertSplits(splitEntities)

        // Update Payers
        expensePayerDao.deletePayersForExpense(expense.id)
        val payerEntities = if (payers.isNotEmpty()) {
            payers.map { (participantId, paidAmt) ->
                ExpensePayerEntity(
                    expenseId = expense.id,
                    participantId = participantId,
                    amount = ExpenseCalculator.roundTo2Decimals(paidAmt)
                )
            }
        } else {
            listOf(
                ExpensePayerEntity(
                    expenseId = expense.id,
                    participantId = primaryPayerId,
                    amount = ExpenseCalculator.roundTo2Decimals(expense.totalAmount)
                )
            )
        }
        expensePayerDao.insertPayers(payerEntities)
    }

    suspend fun deleteExpense(expenseId: Long) {
        expensePayerDao.deletePayersForExpense(expenseId)
        expenseSplitDao.deleteSplitsForExpense(expenseId)
        expenseDao.deleteExpenseById(expenseId)
    }

    suspend fun addSettlement(
        groupId: Long,
        payerId: Long,
        receiverId: Long,
        amount: Double,
        note: String,
        dateMillis: Long = System.currentTimeMillis()
    ): Long {
        val settlement = SettlementEntity(
            groupId = groupId,
            payerParticipantId = payerId,
            receiverParticipantId = receiverId,
            amount = ExpenseCalculator.roundTo2Decimals(amount),
            note = note.trim(),
            dateMillis = dateMillis
        )
        return settlementDao.insertSettlement(settlement)
    }

    suspend fun deleteSettlement(settlementId: Long) {
        settlementDao.deleteSettlementById(settlementId)
    }

    suspend fun exportBackupJson(): String = backupManager.exportToJson()

    suspend fun importBackupJson(jsonString: String): Pair<Boolean, String> =
        backupManager.importFromJson(jsonString)
}
