package com.example.domain.calculator

import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpensePayerEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.ParticipantEntity
import com.example.data.entity.SettlementEntity
import com.example.domain.model.DebtTransaction
import com.example.domain.model.ParticipantBalance
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToLong

object ExpenseCalculator {

    fun roundTo2Decimals(value: Double): Double {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Calculates net balances for all participants in a group.
     * Supports single or multiple payers per expense.
     */
    fun calculateBalances(
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>,
        settlements: List<SettlementEntity>,
        payers: List<ExpensePayerEntity> = emptyList()
    ): List<ParticipantBalance> {
        val splitsByExpense = splits.groupBy { it.expenseId }
        val payersByExpense = payers.groupBy { it.expenseId }

        val paidExpensesMap = mutableMapOf<Long, Double>()
        val shareExpensesMap = mutableMapOf<Long, Double>()
        val paidSettlementsMap = mutableMapOf<Long, Double>()
        val receivedSettlementsMap = mutableMapOf<Long, Double>()

        // 1. Calculate expenses paid & shares
        for (expense in expenses) {
            val expensePayers = payersByExpense[expense.id]
            if (!expensePayers.isNullOrEmpty()) {
                for (payer in expensePayers) {
                    paidExpensesMap[payer.participantId] =
                        (paidExpensesMap[payer.participantId] ?: 0.0) + payer.amount
                }
            } else {
                val payerId = expense.paidByParticipantId
                paidExpensesMap[payerId] = (paidExpensesMap[payerId] ?: 0.0) + expense.totalAmount
            }

            val expenseSplits = splitsByExpense[expense.id] ?: emptyList()
            for (split in expenseSplits) {
                shareExpensesMap[split.participantId] =
                    (shareExpensesMap[split.participantId] ?: 0.0) + split.shareAmount
            }
        }

        // 2. Calculate settlements
        for (settlement in settlements) {
            val payerId = settlement.payerParticipantId
            val receiverId = settlement.receiverParticipantId
            paidSettlementsMap[payerId] = (paidSettlementsMap[payerId] ?: 0.0) + settlement.amount
            receivedSettlementsMap[receiverId] =
                (receivedSettlementsMap[receiverId] ?: 0.0) + settlement.amount
        }

        // 3. Assemble participant balances
        return participants.map { participant ->
            val paidExp = roundTo2Decimals(paidExpensesMap[participant.id] ?: 0.0)
            val shareExp = roundTo2Decimals(shareExpensesMap[participant.id] ?: 0.0)
            val paidSet = roundTo2Decimals(paidSettlementsMap[participant.id] ?: 0.0)
            val recSet = roundTo2Decimals(receivedSettlementsMap[participant.id] ?: 0.0)

            // Net balance:
            // Positive: This participant paid more than their share and is owed money.
            // Negative: This participant paid less than their share and owes money.
            val net = roundTo2Decimals((paidExp - shareExp) + (paidSet - recSet))

            ParticipantBalance(
                participantId = participant.id,
                name = participant.name,
                isCurrentUser = participant.isCurrentUser,
                avatarColorIndex = participant.avatarColorIndex,
                totalPaid = paidExp,
                totalShare = shareExp,
                settledPaid = paidSet,
                settledReceived = recSet,
                netBalance = net
            )
        }
    }

    /**
     * Minimizes debt transactions so that all debts can be settled with
     * the fewest possible direct payments between participants.
     */
    fun simplifyDebts(
        balances: List<ParticipantBalance>
    ): List<DebtTransaction> {
        data class Account(val id: Long, val name: String, var amount: Double)

        val creditors = mutableListOf<Account>()
        val debtors = mutableListOf<Account>()

        for (balance in balances) {
            val net = roundTo2Decimals(balance.netBalance)
            if (net > 0.005) {
                creditors.add(Account(balance.participantId, balance.name, net))
            } else if (net < -0.005) {
                debtors.add(Account(balance.participantId, balance.name, abs(net)))
            }
        }

        val simplified = mutableListOf<DebtTransaction>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            // Sort by largest debt and largest credit
            debtors.sortByDescending { it.amount }
            creditors.sortByDescending { it.amount }

            val debtor = debtors.first()
            val creditor = creditors.first()

            val transfer = roundTo2Decimals(min(debtor.amount, creditor.amount))

            if (transfer > 0.005) {
                simplified.add(
                    DebtTransaction(
                        fromParticipantId = debtor.id,
                        fromName = debtor.name,
                        toParticipantId = creditor.id,
                        toName = creditor.name,
                        amount = transfer
                    )
                )
            }

            debtor.amount = roundTo2Decimals(debtor.amount - transfer)
            creditor.amount = roundTo2Decimals(creditor.amount - transfer)

            if (debtor.amount < 0.005) {
                debtors.removeAt(0)
            }
            if (creditor.amount < 0.005) {
                creditors.removeAt(0)
            }
        }

        return simplified
    }

    /**
     * Calculates equal splits with penny remainder distribution so total equals exact sum.
     */
    fun calculateEqualSplits(
        totalAmount: Double,
        participantIds: List<Long>
    ): Map<Long, Double> {
        if (participantIds.isEmpty() || totalAmount <= 0.0) return emptyMap()

        val totalCents = (totalAmount * 100.0).roundToLong()
        val n = participantIds.size
        val baseCentsPerPerson = totalCents / n
        var remainderCents = totalCents % n

        val result = mutableMapOf<Long, Double>()
        for (id in participantIds) {
            val cents = baseCentsPerPerson + if (remainderCents > 0) {
                remainderCents--
                1
            } else {
                0
            }
            result[id] = cents / 100.0
        }
        return result
    }

    /**
     * Calculates percentage splits ensuring total shares sum exactly to totalAmount.
     */
    fun calculatePercentageSplits(
        totalAmount: Double,
        percentages: Map<Long, Double>
    ): Map<Long, Double> {
        if (percentages.isEmpty() || totalAmount <= 0.0) return emptyMap()

        val totalCents = (totalAmount * 100.0).roundToLong()
        val sharesInCents = mutableMapOf<Long, Long>()
        var allocatedCents = 0L

        // Initial allocation
        for ((id, pct) in percentages) {
            val cents = ((totalAmount * (pct / 100.0)) * 100.0).roundToLong()
            sharesInCents[id] = cents
            allocatedCents += cents
        }

        // Adjust rounding delta to the participant with largest percentage
        val delta = totalCents - allocatedCents
        if (delta != 0L && percentages.isNotEmpty()) {
            val maxEntry = percentages.maxByOrNull { it.value }
            if (maxEntry != null) {
                val current = sharesInCents[maxEntry.key] ?: 0L
                sharesInCents[maxEntry.key] = current + delta
            }
        }

        return sharesInCents.mapValues { it.value / 100.0 }
    }
}
