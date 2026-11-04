package dev.rakib.mitmat.domain.model

import dev.rakib.mitmat.data.entity.ExpenseEntity
import dev.rakib.mitmat.data.entity.ExpensePayerEntity
import dev.rakib.mitmat.data.entity.ExpenseSplitEntity
import dev.rakib.mitmat.data.entity.GroupEntity
import dev.rakib.mitmat.data.entity.ParticipantEntity
import dev.rakib.mitmat.data.entity.SettlementEntity

data class ParticipantBalance(
    val participantId: Long,
    val name: String,
    val isCurrentUser: Boolean,
    val avatarColorIndex: Int,
    val totalPaid: Double,
    val totalShare: Double,
    val settledPaid: Double,
    val settledReceived: Double,
    val netBalance: Double
) {
    // netBalance > 0 means participant is owed money (receives)
    // netBalance < 0 means participant owes money
    val isSettled: Boolean get() = kotlin.math.abs(netBalance) < 0.01
}

data class DebtTransaction(
    val fromParticipantId: Long,
    val fromName: String,
    val toParticipantId: Long,
    val toName: String,
    val amount: Double
)

data class GroupSummary(
    val group: GroupEntity,
    val participants: List<ParticipantEntity>,
    val totalExpenses: Double,
    val userNetBalance: Double,
    val totalSimplifiedDebtsCount: Int
)

data class ExpensePayerWithParticipant(
    val payer: ExpensePayerEntity,
    val participant: ParticipantEntity
)

data class ExpenseSplitWithParticipant(
    val split: ExpenseSplitEntity,
    val participant: ParticipantEntity
)

data class ExpenseWithDetails(
    val expense: ExpenseEntity,
    val payerName: String,
    val payers: List<ExpensePayerWithParticipant> = emptyList(),
    val splits: List<ExpenseSplitWithParticipant> = emptyList()
)

data class SettlementWithDetails(
    val settlement: SettlementEntity,
    val payerName: String,
    val receiverName: String
)

sealed interface ActivityFeedItem {
    val id: String
    val groupId: Long
    val groupName: String
    val currency: String
    val dateMillis: Long
    val title: String
    val subtitle: String
    val amount: Double

    data class ExpenseFeed(
        override val id: String,
        override val groupId: Long,
        override val groupName: String,
        override val currency: String,
        override val dateMillis: Long,
        override val title: String,
        override val subtitle: String,
        override val amount: Double,
        val category: String,
        val payerName: String,
        val expenseId: Long
    ) : ActivityFeedItem

    data class SettlementFeed(
        override val id: String,
        override val groupId: Long,
        override val groupName: String,
        override val currency: String,
        override val dateMillis: Long,
        override val title: String,
        override val subtitle: String,
        override val amount: Double,
        val payerName: String,
        val receiverName: String,
        val settlementId: Long
    ) : ActivityFeedItem
}
