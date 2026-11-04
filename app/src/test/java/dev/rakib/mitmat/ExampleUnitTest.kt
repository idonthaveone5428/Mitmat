package dev.rakib.mitmat

import dev.rakib.mitmat.data.entity.ExpenseEntity
import dev.rakib.mitmat.data.entity.ExpensePayerEntity
import dev.rakib.mitmat.data.entity.ExpenseSplitEntity
import dev.rakib.mitmat.data.entity.ParticipantEntity
import dev.rakib.mitmat.domain.calculator.ExpenseCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test exact prompt example balance calculation`() {
        // Three friends share an expense of ৳1,500
        // Person A pays ৳1,000, Person B pays ৳500, Person C pays ৳0
        // Each person's share is ৳500
        // Expected: A receives 500, B settled (0), C owes 500

        val pA = ParticipantEntity(id = 1, groupId = 1, name = "Person A")
        val pB = ParticipantEntity(id = 2, groupId = 1, name = "Person B")
        val pC = ParticipantEntity(id = 3, groupId = 1, name = "Person C")

        val expense1 = ExpenseEntity(
            id = 10,
            groupId = 1,
            title = "Shared Bill 1",
            totalAmount = 1000.0,
            paidByParticipantId = 1,
            splitType = "EQUAL"
        )
        val expense2 = ExpenseEntity(
            id = 11,
            groupId = 1,
            title = "Shared Bill 2",
            totalAmount = 500.0,
            paidByParticipantId = 2,
            splitType = "EQUAL"
        )

        val splits = listOf(
            ExpenseSplitEntity(id = 1, expenseId = 10, participantId = 1, shareAmount = 333.34),
            ExpenseSplitEntity(id = 2, expenseId = 10, participantId = 2, shareAmount = 333.33),
            ExpenseSplitEntity(id = 3, expenseId = 10, participantId = 3, shareAmount = 333.33),
            ExpenseSplitEntity(id = 4, expenseId = 11, participantId = 1, shareAmount = 166.66),
            ExpenseSplitEntity(id = 5, expenseId = 11, participantId = 2, shareAmount = 166.67),
            ExpenseSplitEntity(id = 6, expenseId = 11, participantId = 3, shareAmount = 166.67)
        )

        val balances = ExpenseCalculator.calculateBalances(
            participants = listOf(pA, pB, pC),
            expenses = listOf(expense1, expense2),
            splits = splits,
            settlements = emptyList()
        )

        val balanceA = balances.first { it.participantId == 1L }
        val balanceB = balances.first { it.participantId == 2L }
        val balanceC = balances.first { it.participantId == 3L }

        // Person A: paid 1000, share 500 -> net +500
        assertEquals(500.0, balanceA.netBalance, 0.01)
        // Person B: paid 500, share 500 -> net 0
        assertEquals(0.0, balanceB.netBalance, 0.01)
        // Person C: paid 0, share 500 -> net -500
        assertEquals(-500.0, balanceC.netBalance, 0.01)

        val simplified = ExpenseCalculator.simplifyDebts(balances)
        assertEquals(1, simplified.size)
        assertEquals(3L, simplified[0].fromParticipantId) // Person C owes
        assertEquals(1L, simplified[0].toParticipantId)   // Person A receives
        assertEquals(500.0, simplified[0].amount, 0.01)
    }

    @Test
    fun `test multiple payers for single expense`() {
        // User scenario: A paid 200 and B paid 400 for a 600 total expense.
        // Split equally among A, B, and C (200 each).
        val pA = ParticipantEntity(id = 1, groupId = 1, name = "A")
        val pB = ParticipantEntity(id = 2, groupId = 1, name = "B")
        val pC = ParticipantEntity(id = 3, groupId = 1, name = "C")

        val expense = ExpenseEntity(
            id = 20,
            groupId = 1,
            title = "Dinner with multiple payers",
            totalAmount = 600.0,
            paidByParticipantId = 1,
            splitType = "EQUAL"
        )

        val payers = listOf(
            ExpensePayerEntity(id = 1, expenseId = 20, participantId = 1, amount = 200.0),
            ExpensePayerEntity(id = 2, expenseId = 20, participantId = 2, amount = 400.0)
        )

        val splits = listOf(
            ExpenseSplitEntity(id = 1, expenseId = 20, participantId = 1, shareAmount = 200.0),
            ExpenseSplitEntity(id = 2, expenseId = 20, participantId = 2, shareAmount = 200.0),
            ExpenseSplitEntity(id = 3, expenseId = 20, participantId = 3, shareAmount = 200.0)
        )

        val balances = ExpenseCalculator.calculateBalances(
            participants = listOf(pA, pB, pC),
            expenses = listOf(expense),
            splits = splits,
            settlements = emptyList(),
            payers = payers
        )

        val balanceA = balances.first { it.participantId == 1L }
        val balanceB = balances.first { it.participantId == 2L }
        val balanceC = balances.first { it.participantId == 3L }

        // A paid 200, share 200 -> net 0 (settled)
        assertEquals(0.0, balanceA.netBalance, 0.01)
        // B paid 400, share 200 -> net +200 (is owed 200)
        assertEquals(200.0, balanceB.netBalance, 0.01)
        // C paid 0, share 200 -> net -200 (owes 200)
        assertEquals(-200.0, balanceC.netBalance, 0.01)

        val simplified = ExpenseCalculator.simplifyDebts(balances)
        assertEquals(1, simplified.size)
        // C owes B 200
        assertEquals(3L, simplified[0].fromParticipantId)
        assertEquals(2L, simplified[0].toParticipantId)
        assertEquals(200.0, simplified[0].amount, 0.01)
    }

    @Test
    fun `test auto sum multiple payers and equal split`() {
        val payerAmounts = mapOf(1L to 200.0, 2L to 400.0)
        val autoSummedTotal = ExpenseCalculator.roundTo2Decimals(payerAmounts.values.sum())
        assertEquals(600.0, autoSummedTotal, 0.001)

        val splits = ExpenseCalculator.calculateEqualSplits(autoSummedTotal, listOf(1L, 2L, 3L))
        assertEquals(200.0, splits[1L]!!, 0.01)
        assertEquals(200.0, splits[2L]!!, 0.01)
        assertEquals(200.0, splits[3L]!!, 0.01)
    }

    @Test
    fun `test equal split cent remainder distribution`() {
        val splits = ExpenseCalculator.calculateEqualSplits(100.0, listOf(1L, 2L, 3L))
        assertEquals(3, splits.size)
        val sum = splits.values.sum()
        assertEquals(100.0, sum, 0.001)
        assertTrue(splits.values.all { it == 33.33 || it == 33.34 })
    }

    @Test
    fun `test percentage split rounding adjustment`() {
        val percentages = mapOf(1L to 33.33, 2L to 33.33, 3L to 33.34)
        val splits = ExpenseCalculator.calculatePercentageSplits(100.0, percentages)
        val sum = splits.values.sum()
        assertEquals(100.0, sum, 0.001)
    }
}
