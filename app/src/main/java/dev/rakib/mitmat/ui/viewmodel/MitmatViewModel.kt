package dev.rakib.mitmat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.rakib.mitmat.data.db.MitmatDatabase
import dev.rakib.mitmat.data.entity.ExpenseEntity
import dev.rakib.mitmat.data.entity.ExpensePayerEntity
import dev.rakib.mitmat.data.entity.ExpenseSplitEntity
import dev.rakib.mitmat.data.entity.GroupEntity
import dev.rakib.mitmat.data.entity.ParticipantEntity
import dev.rakib.mitmat.data.entity.SettlementEntity
import dev.rakib.mitmat.data.preferences.AppearanceMode
import dev.rakib.mitmat.data.preferences.AppPreferences
import dev.rakib.mitmat.data.preferences.PreferencesManager
import dev.rakib.mitmat.data.repository.MitmatRepository
import dev.rakib.mitmat.domain.calculator.ExpenseCalculator
import dev.rakib.mitmat.domain.model.ActivityFeedItem
import dev.rakib.mitmat.domain.model.DebtTransaction
import dev.rakib.mitmat.domain.model.ExpensePayerWithParticipant
import dev.rakib.mitmat.domain.model.ExpenseSplitWithParticipant
import dev.rakib.mitmat.domain.model.ExpenseWithDetails
import dev.rakib.mitmat.domain.model.GroupSummary
import dev.rakib.mitmat.domain.model.ParticipantBalance
import dev.rakib.mitmat.domain.model.SettlementWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val group: GroupEntity? = null,
    val participants: List<ParticipantEntity> = emptyList(),
    val expenses: List<ExpenseWithDetails> = emptyList(),
    val settlements: List<SettlementWithDetails> = emptyList(),
    val balances: List<ParticipantBalance> = emptyList(),
    val simplifiedDebts: List<DebtTransaction> = emptyList(),
    val totalExpenseAmount: Double = 0.0,
    val isLoading: Boolean = false
)

data class DashboardUiState(
    val totalYouOwe: Double = 0.0,
    val totalYouAreOwed: Double = 0.0,
    val netBalance: Double = 0.0,
    val defaultCurrency: String = "৳"
)

class MitmatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MitmatDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    val repository = MitmatRepository(
        groupDao = database.groupDao(),
        participantDao = database.participantDao(),
        expenseDao = database.expenseDao(),
        expensePayerDao = database.expensePayerDao(),
        expenseSplitDao = database.expenseSplitDao(),
        settlementDao = database.settlementDao()
    )

    init {
        viewModelScope.launch {
            // Automatically purge trashed groups older than 7 days
            repository.purgeOldTrashedGroups()
            // Clean up any remaining demo groups and expenses
            repository.purgeDemoDataIfPresent()
        }
    }

    val preferences: StateFlow<AppPreferences> = preferencesManager.preferencesFlow

    val allGroups: StateFlow<List<GroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedGroups: StateFlow<List<GroupEntity>> = repository.archivedGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedGroups: StateFlow<List<GroupEntity>> = repository.trashedGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupSummaries: StateFlow<List<GroupSummary>> = repository.groupSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityFeed: StateFlow<List<ActivityFeedItem>> = repository.activityFeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall dashboard statistics
    val dashboardState: StateFlow<DashboardUiState> = combine(
        groupSummaries,
        preferences
    ) { summaries, prefs ->
        var owe = 0.0
        var owed = 0.0

        for (summary in summaries) {
            if (summary.userNetBalance < -0.01) {
                owe += kotlin.math.abs(summary.userNetBalance)
            } else if (summary.userNetBalance > 0.01) {
                owed += summary.userNetBalance
            }
        }

        DashboardUiState(
            totalYouOwe = ExpenseCalculator.roundTo2Decimals(owe),
            totalYouAreOwed = ExpenseCalculator.roundTo2Decimals(owed),
            netBalance = ExpenseCalculator.roundTo2Decimals(owed - owe),
            defaultCurrency = prefs.defaultCurrency
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    // Currently selected group detail state
    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId.asStateFlow()

    fun selectGroup(groupId: Long?) {
        _selectedGroupId.value = groupId
    }

    private data class ExpenseSnapshot(
        val expenses: List<ExpenseEntity>,
        val splits: List<ExpenseSplitEntity>,
        val payers: List<ExpensePayerEntity>
    )

    private val expenseSnapshotFlow = combine(
        repository.allExpenses,
        repository.allSplits,
        repository.allExpensePayers
    ) { exp, splits, payers ->
        ExpenseSnapshot(exp, splits, payers)
    }

    private data class DatabaseSnapshot(
        val groups: List<GroupEntity>,
        val participants: List<ParticipantEntity>,
        val expenses: List<ExpenseEntity>,
        val splits: List<ExpenseSplitEntity>,
        val payers: List<ExpensePayerEntity>,
        val settlements: List<SettlementEntity>
    )

    private val databaseSnapshotFlow = combine(
        database.groupDao().getAllActiveAndArchivedGroups(),
        repository.allParticipants,
        expenseSnapshotFlow,
        repository.allSettlements
    ) { groups, participants, expSnap, settlements ->
        DatabaseSnapshot(
            groups = groups,
            participants = participants,
            expenses = expSnap.expenses,
            splits = expSnap.splits,
            payers = expSnap.payers,
            settlements = settlements
        )
    }

    val selectedGroupDetail: StateFlow<GroupDetailUiState> = combine(
        _selectedGroupId,
        databaseSnapshotFlow
    ) { groupId, snapshot ->
        if (groupId == null) return@combine GroupDetailUiState()

        val group = snapshot.groups.firstOrNull { it.id == groupId }
        val groupParticipants = snapshot.participants.filter { it.groupId == groupId }
        val groupExpenses = snapshot.expenses.filter { it.groupId == groupId }
        val groupSettlements = snapshot.settlements.filter { it.groupId == groupId }

        val expenseIds = groupExpenses.map { it.id }.toSet()
        val groupSplits = snapshot.splits.filter { it.expenseId in expenseIds }
        val groupPayers = snapshot.payers.filter { it.expenseId in expenseIds }

        val balances = ExpenseCalculator.calculateBalances(
            participants = groupParticipants,
            expenses = groupExpenses,
            splits = groupSplits,
            settlements = groupSettlements,
            payers = groupPayers
        )

        val simplified = ExpenseCalculator.simplifyDebts(balances)

        val participantMap = groupParticipants.associateBy { it.id }
        val splitsByExpense = groupSplits.groupBy { it.expenseId }
        val payersByExpense = groupPayers.groupBy { it.expenseId }

        val expensesWithDetails = groupExpenses.map { exp ->
            val expSplits = (splitsByExpense[exp.id] ?: emptyList()).mapNotNull { s ->
                val p = participantMap[s.participantId]
                if (p != null) ExpenseSplitWithParticipant(s, p) else null
            }
            val expPayers = (payersByExpense[exp.id] ?: emptyList()).mapNotNull { py ->
                val p = participantMap[py.participantId]
                if (p != null) ExpensePayerWithParticipant(py, p) else null
            }
            val payerName = if (expPayers.size > 1) {
                expPayers.joinToString(", ") { "${it.participant.name} (${group?.defaultCurrency ?: "৳"}${it.payer.amount.toInt()})" }
            } else {
                val singleId = expPayers.firstOrNull()?.payer?.participantId ?: exp.paidByParticipantId
                participantMap[singleId]?.name ?: "Unknown"
            }
            ExpenseWithDetails(
                expense = exp,
                payerName = payerName,
                payers = expPayers,
                splits = expSplits
            )
        }

        val settlementsWithDetails = groupSettlements.map { set ->
            SettlementWithDetails(
                settlement = set,
                payerName = participantMap[set.payerParticipantId]?.name ?: "Unknown",
                receiverName = participantMap[set.receiverParticipantId]?.name ?: "Unknown"
            )
        }

        GroupDetailUiState(
            group = group,
            participants = groupParticipants,
            expenses = expensesWithDetails,
            settlements = settlementsWithDetails,
            balances = balances,
            simplifiedDebts = simplified,
            totalExpenseAmount = ExpenseCalculator.roundTo2Decimals(groupExpenses.sumOf { it.totalAmount }),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupDetailUiState(isLoading = true))

    fun createGroup(name: String, currency: String, colorIndex: Int, participants: List<String>) {
        viewModelScope.launch {
            repository.createGroup(name, currency, colorIndex, participants)
        }
    }

    fun updateGroup(group: GroupEntity) {
        viewModelScope.launch {
            repository.updateGroup(group)
        }
    }

    // Move group to trash bin
    fun trashGroup(groupId: Long) {
        viewModelScope.launch {
            repository.trashGroup(groupId)
            if (_selectedGroupId.value == groupId) {
                _selectedGroupId.value = null
            }
        }
    }

    // Restore group from trash bin
    fun restoreGroup(groupId: Long) {
        viewModelScope.launch {
            repository.restoreGroup(groupId)
        }
    }

    // Toggle archive status
    fun archiveGroup(groupId: Long, archive: Boolean) {
        viewModelScope.launch {
            repository.archiveGroup(groupId, archive)
        }
    }

    // Permanently delete immediately
    fun permanentlyDeleteGroup(groupId: Long) {
        viewModelScope.launch {
            repository.permanentlyDeleteGroup(groupId)
            if (_selectedGroupId.value == groupId) {
                _selectedGroupId.value = null
            }
        }
    }

    // Empty entire trash bin
    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun addParticipant(groupId: Long, name: String) {
        viewModelScope.launch {
            repository.addParticipant(groupId, name)
        }
    }

    fun updateParticipant(participant: ParticipantEntity) {
        viewModelScope.launch {
            repository.updateParticipant(participant)
        }
    }

    fun deleteParticipant(participant: ParticipantEntity) {
        viewModelScope.launch {
            repository.deleteParticipant(participant)
        }
    }

    fun addExpense(
        groupId: Long,
        title: String,
        amount: Double,
        payerId: Long,
        splitType: String,
        category: String,
        note: String,
        splits: Map<Long, Double>,
        percentages: Map<Long, Double> = emptyMap(),
        payers: Map<Long, Double> = emptyMap(),
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.addExpense(
                groupId = groupId,
                title = title,
                totalAmount = amount,
                paidByParticipantId = payerId,
                splitType = splitType,
                category = category,
                note = note,
                dateMillis = dateMillis,
                splits = splits,
                percentages = percentages,
                payers = payers
            )
        }
    }

    fun updateExpense(
        expense: ExpenseEntity,
        splits: Map<Long, Double>,
        percentages: Map<Long, Double> = emptyMap(),
        payers: Map<Long, Double> = emptyMap()
    ) {
        viewModelScope.launch {
            repository.updateExpense(expense, splits, percentages, payers)
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    fun addSettlement(
        groupId: Long,
        payerId: Long,
        receiverId: Long,
        amount: Double,
        note: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.addSettlement(groupId, payerId, receiverId, amount, note, dateMillis)
        }
    }

    fun deleteSettlement(settlementId: Long) {
        viewModelScope.launch {
            repository.deleteSettlement(settlementId)
        }
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        preferencesManager.setAppearanceMode(mode)
    }

    fun setIsAmoled(isAmoled: Boolean) {
        preferencesManager.setIsAmoled(isAmoled)
    }

    fun deleteGroup(groupId: Long) {
        trashGroup(groupId)
    }

    fun setCustomDevAvatar(uri: String?) {
        preferencesManager.setCustomDevAvatar(uri)
    }

    fun exportBackupJson(selectedGroupIds: Set<Long>? = null, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson(selectedGroupIds)
            onResult(json)
        }
    }

    fun importBackupJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = repository.importBackupJson(jsonString)
            onResult(success, message)
        }
    }
}
