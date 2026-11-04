package dev.rakib.mitmat.data.repository

import dev.rakib.mitmat.data.backup.DataBackupManager
import dev.rakib.mitmat.data.dao.ExpenseDao
import dev.rakib.mitmat.data.dao.ExpensePayerDao
import dev.rakib.mitmat.data.dao.ExpenseSplitDao
import dev.rakib.mitmat.data.dao.GroupDao
import dev.rakib.mitmat.data.dao.ParticipantDao
import dev.rakib.mitmat.data.dao.SettlementDao
import dev.rakib.mitmat.data.entity.ExpenseEntity
import dev.rakib.mitmat.data.entity.ExpensePayerEntity
import dev.rakib.mitmat.data.entity.ExpenseSplitEntity
import dev.rakib.mitmat.data.entity.GroupEntity
import dev.rakib.mitmat.data.entity.ParticipantEntity
import dev.rakib.mitmat.data.entity.SettlementEntity
import dev.rakib.mitmat.domain.calculator.ExpenseCalculator
import dev.rakib.mitmat.domain.model.ActivityFeedItem
import dev.rakib.mitmat.domain.model.ExpensePayerWithParticipant
import dev.rakib.mitmat.domain.model.ExpenseSplitWithParticipant
import dev.rakib.mitmat.domain.model.ExpenseWithDetails
import dev.rakib.mitmat.domain.model.GroupSummary
import dev.rakib.mitmat.domain.model.SettlementWithDetails
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

    suspend fun exportBackupJson(selectedGroupIds: Set<Long>? = null): String =
        backupManager.exportToJson(selectedGroupIds)

    suspend fun importBackupJson(jsonString: String): Pair<Boolean, String> =
        backupManager.importFromJson(jsonString)
}
