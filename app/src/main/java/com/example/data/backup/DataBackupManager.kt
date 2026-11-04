package com.example.data.backup

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
import org.json.JSONArray
import org.json.JSONObject

class DataBackupManager(
    private val groupDao: GroupDao,
    private val participantDao: ParticipantDao,
    private val expenseDao: ExpenseDao,
    private val expensePayerDao: ExpensePayerDao,
    private val expenseSplitDao: ExpenseSplitDao,
    private val settlementDao: SettlementDao
) {

    suspend fun exportToJson(): String {
        val root = JSONObject()
        root.put("app", "Mitmat")
        root.put("version", 3)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Groups
        val groups = groupDao.getAllGroupsRawOnce()
        val groupsArray = JSONArray()
        for (g in groups) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("defaultCurrency", g.defaultCurrency)
            obj.put("colorIndex", g.colorIndex)
            obj.put("createdAt", g.createdAt)
            obj.put("isArchived", g.isArchived)
            obj.put("isTrashed", g.isTrashed)
            if (g.trashedAt != null) obj.put("trashedAt", g.trashedAt)
            groupsArray.put(obj)
        }
        root.put("groups", groupsArray)

        // 2. Participants
        val participants = participantDao.getAllParticipantsOnce()
        val participantsArray = JSONArray()
        for (p in participants) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("groupId", p.groupId)
            obj.put("name", p.name)
            obj.put("isCurrentUser", p.isCurrentUser)
            obj.put("avatarColorIndex", p.avatarColorIndex)
            participantsArray.put(obj)
        }
        root.put("participants", participantsArray)

        // 3. Expenses
        val expenses = expenseDao.getAllExpensesOnce()
        val expensesArray = JSONArray()
        for (e in expenses) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("groupId", e.groupId)
            obj.put("title", e.title)
            obj.put("totalAmount", e.totalAmount)
            obj.put("paidByParticipantId", e.paidByParticipantId)
            obj.put("splitType", e.splitType)
            obj.put("category", e.category)
            obj.put("note", e.note)
            obj.put("dateMillis", e.dateMillis)
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        // 4. Expense Payers
        val payers = expensePayerDao.getAllExpensePayersOnce()
        val payersArray = JSONArray()
        for (py in payers) {
            val obj = JSONObject()
            obj.put("id", py.id)
            obj.put("expenseId", py.expenseId)
            obj.put("participantId", py.participantId)
            obj.put("amount", py.amount)
            payersArray.put(obj)
        }
        root.put("expensePayers", payersArray)

        // 5. Expense Splits
        val splits = expenseSplitDao.getAllSplitsOnce()
        val splitsArray = JSONArray()
        for (sp in splits) {
            val obj = JSONObject()
            obj.put("id", sp.id)
            obj.put("expenseId", sp.expenseId)
            obj.put("participantId", sp.participantId)
            obj.put("shareAmount", sp.shareAmount)
            obj.put("percentage", sp.percentage)
            splitsArray.put(obj)
        }
        root.put("expenseSplits", splitsArray)

        // 6. Settlements
        val settlements = settlementDao.getAllSettlementsOnce()
        val settlementsArray = JSONArray()
        for (s in settlements) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("groupId", s.groupId)
            obj.put("payerParticipantId", s.payerParticipantId)
            obj.put("receiverParticipantId", s.receiverParticipantId)
            obj.put("amount", s.amount)
            obj.put("note", s.note)
            obj.put("dateMillis", s.dateMillis)
            settlementsArray.put(obj)
        }
        root.put("settlements", settlementsArray)

        return root.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Pair<Boolean, String> {
        return try {
            val trimmed = jsonString.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                return Pair(false, "Invalid backup format: Must be a valid JSON object.")
            }

            val root = JSONObject(trimmed)
            if (!root.has("groups")) {
                return Pair(false, "Invalid Mitmat backup: No groups found in data.")
            }

            val groupsArray = root.getJSONArray("groups")
            val groupEntities = mutableListOf<GroupEntity>()
            for (i in 0 until groupsArray.length()) {
                val obj = groupsArray.getJSONObject(i)
                groupEntities.add(
                    GroupEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        defaultCurrency = obj.optString("defaultCurrency", "৳"),
                        colorIndex = obj.optInt("colorIndex", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isArchived = obj.optBoolean("isArchived", false),
                        isTrashed = obj.optBoolean("isTrashed", false),
                        trashedAt = if (obj.has("trashedAt")) obj.optLong("trashedAt") else null
                    )
                )
            }

            val participantsArray = root.optJSONArray("participants") ?: JSONArray()
            val participantEntities = mutableListOf<ParticipantEntity>()
            for (i in 0 until participantsArray.length()) {
                val obj = participantsArray.getJSONObject(i)
                participantEntities.add(
                    ParticipantEntity(
                        id = obj.optLong("id", 0L),
                        groupId = obj.getLong("groupId"),
                        name = obj.getString("name"),
                        isCurrentUser = obj.optBoolean("isCurrentUser", false),
                        avatarColorIndex = obj.optInt("avatarColorIndex", 0)
                    )
                )
            }

            val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
            val expenseEntities = mutableListOf<ExpenseEntity>()
            for (i in 0 until expensesArray.length()) {
                val obj = expensesArray.getJSONObject(i)
                expenseEntities.add(
                    ExpenseEntity(
                        id = obj.optLong("id", 0L),
                        groupId = obj.getLong("groupId"),
                        title = obj.getString("title"),
                        totalAmount = obj.getDouble("totalAmount"),
                        paidByParticipantId = obj.getLong("paidByParticipantId"),
                        splitType = obj.optString("splitType", "EQUAL"),
                        category = obj.optString("category", "General"),
                        note = obj.optString("note", ""),
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis())
                    )
                )
            }

            val payersArray = root.optJSONArray("expensePayers") ?: JSONArray()
            val payerEntities = mutableListOf<ExpensePayerEntity>()
            for (i in 0 until payersArray.length()) {
                val obj = payersArray.getJSONObject(i)
                payerEntities.add(
                    ExpensePayerEntity(
                        id = obj.optLong("id", 0L),
                        expenseId = obj.getLong("expenseId"),
                        participantId = obj.getLong("participantId"),
                        amount = obj.getDouble("amount")
                    )
                )
            }

            val splitsArray = root.optJSONArray("expenseSplits") ?: JSONArray()
            val splitEntities = mutableListOf<ExpenseSplitEntity>()
            for (i in 0 until splitsArray.length()) {
                val obj = splitsArray.getJSONObject(i)
                splitEntities.add(
                    ExpenseSplitEntity(
                        id = obj.optLong("id", 0L),
                        expenseId = obj.getLong("expenseId"),
                        participantId = obj.getLong("participantId"),
                        shareAmount = obj.getDouble("shareAmount"),
                        percentage = obj.optDouble("percentage", 0.0)
                    )
                )
            }

            val settlementsArray = root.optJSONArray("settlements") ?: JSONArray()
            val settlementEntities = mutableListOf<SettlementEntity>()
            for (i in 0 until settlementsArray.length()) {
                val obj = settlementsArray.getJSONObject(i)
                settlementEntities.add(
                    SettlementEntity(
                        id = obj.optLong("id", 0L),
                        groupId = obj.getLong("groupId"),
                        payerParticipantId = obj.getLong("payerParticipantId"),
                        receiverParticipantId = obj.getLong("receiverParticipantId"),
                        amount = obj.getDouble("amount"),
                        note = obj.optString("note", ""),
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis())
                    )
                )
            }

            // Insert restored entities into Room with REPLACE strategy
            groupDao.insertGroups(groupEntities)
            participantDao.insertParticipants(participantEntities)
            expenseDao.insertExpenses(expenseEntities)
            expensePayerDao.insertPayers(payerEntities)
            expenseSplitDao.insertSplits(splitEntities)
            settlementDao.insertSettlements(settlementEntities)

            Pair(
                true,
                "Restored ${groupEntities.size} groups, ${participantEntities.size} members, ${expenseEntities.size} expenses."
            )
        } catch (e: Exception) {
            Pair(false, "Failed to import backup: ${e.localizedMessage ?: "Unknown parse error"}")
        }
    }
}
