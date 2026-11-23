package com.anand8290.splitlite.data

import com.anand8290.splitlite.domain.Expense
import com.anand8290.splitlite.domain.GroupDetail
import com.anand8290.splitlite.domain.GroupInfo
import com.anand8290.splitlite.domain.GroupPreview
import com.anand8290.splitlite.domain.GroupSummary
import com.anand8290.splitlite.domain.Member
import com.anand8290.splitlite.domain.Split
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Everything the app reads or writes on the server. Screens observe the StateFlows. */
class SplitRepository(
    private val client: SupabaseClient,
    private val cache: LocalCache,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val signInLock = Mutex()

    private val _groups = MutableStateFlow(cache.readGroups())
    val groups: StateFlow<List<GroupSummary>> = _groups.asStateFlow()

    private val details = mutableMapOf<String, MutableStateFlow<GroupDetail?>>()

    fun group(id: String): StateFlow<GroupDetail?> = groupFlow(id).asStateFlow()

    private fun groupFlow(id: String) = synchronized(details) {
        details.getOrPut(id) { MutableStateFlow(cache.readGroup(id)) }
    }

    /** Signs in anonymously the first time; the session is then kept on the device. */
    suspend fun ensureSignedIn() = signInLock.withLock {
        client.auth.awaitInitialization()
        if (client.auth.currentSessionOrNull() == null) client.auth.signInAnonymously()
    }

    suspend fun refreshGroups() {
        ensureSignedIn()
        val list = client.postgrest.rpc("my_groups").decodeList<GroupSummary>()
        _groups.value = list
        cache.writeGroups(list)
    }

    suspend fun refreshGroup(id: String) {
        ensureSignedIn()
        val me = client.auth.currentUserOrNull()?.id
        val info = client.from("groups").select { filter { eq("id", id) } }.decodeSingle<GroupInfo>()
        val members = client.from("members").select {
            filter { eq("group_id", id) }
            order("created_at", Order.ASCENDING)
        }.decodeList<Member>()
        val expenses = client.from("expenses").select(Columns.raw("*, expense_splits(member_id, amount_minor)")) {
            filter { eq("group_id", id) }
        }.decodeList<Expense>()
            .filter { it.deletedAt == null }
            .sortedWith(compareByDescending<Expense> { it.spentOn }.thenByDescending { it.createdAt })
        val detail = GroupDetail(info, members, expenses, members.firstOrNull { it.claimedBy == me }?.id)
        groupFlow(id).value = detail
        cache.writeGroup(detail)
    }

    /** Creates a group. [myIndex] is which of [memberNames] is the current user. */
    suspend fun createGroup(name: String, currency: String, memberNames: List<String>, myIndex: Int): String {
        ensureSignedIn()
        val id = client.postgrest.rpc("create_group", buildJsonObject {
            put("p_name", name)
            put("p_currency", currency)
            putJsonArray("p_member_names") { memberNames.forEach { add(it) } }
            put("p_my_index", myIndex)
        }).decodeAs<String>()
        refreshGroups()
        return id
    }

    suspend fun saveExpense(
        groupId: String,
        expenseId: String?,
        title: String,
        amountMinor: Long,
        payerId: String,
        splits: List<Split>,
        spentOn: String,
        isSettlement: Boolean = false,
    ) {
        ensureSignedIn()
        client.postgrest.rpc("save_expense", buildJsonObject {
            put("p_id", expenseId)
            put("p_group_id", groupId)
            put("p_title", title)
            put("p_amount", amountMinor)
            put("p_payer", payerId)
            put("p_is_settlement", isSettlement)
            put("p_spent_on", spentOn)
            putJsonArray("p_splits") {
                splits.forEach { s ->
                    addJsonObject {
                        put("member_id", s.memberId)
                        put("amount_minor", s.amountMinor)
                    }
                }
            }
        })
        refreshAfterChange(groupId)
    }

    suspend fun deleteExpense(groupId: String, expenseId: String) {
        ensureSignedIn()
        client.postgrest.rpc("delete_expense", buildJsonObject { put("p_id", expenseId) })
        refreshAfterChange(groupId)
    }

    /** Returns null when the invite code doesn't exist. */
    suspend fun preview(code: String): GroupPreview? {
        ensureSignedIn()
        val body = client.postgrest.rpc("group_preview", buildJsonObject { put("p_code", code) }).data
        if (body.isBlank() || body == "null") return null
        return json.decodeFromString<GroupPreview>(body)
    }

    /** Joins as [memberId], or as a new member called [newName] when memberId is null. */
    suspend fun join(code: String, memberId: String?, newName: String?): String {
        ensureSignedIn()
        val id = client.postgrest.rpc("join_group", buildJsonObject {
            put("p_code", code)
            put("p_member_id", memberId)
            put("p_new_name", newName)
        }).decodeAs<String>()
        refreshGroups()
        return id
    }

    suspend fun leaveGroup(groupId: String) {
        ensureSignedIn()
        client.postgrest.rpc("leave_group", buildJsonObject { put("p_group_id", groupId) })
        cache.deleteGroup(groupId)
        synchronized(details) { details.remove(groupId) }
        refreshGroups()
    }

    /** Deletes everything this device has on the server and starts fresh. */
    suspend fun deleteMyData() {
        ensureSignedIn()
        client.postgrest.rpc("delete_my_data")
        runCatching { client.auth.signOut() }
        cache.clear()
        synchronized(details) { details.clear() }
        _groups.value = emptyList()
    }

    private suspend fun refreshAfterChange(groupId: String) {
        refreshGroup(groupId)
        runCatching { refreshGroups() }
    }
}
