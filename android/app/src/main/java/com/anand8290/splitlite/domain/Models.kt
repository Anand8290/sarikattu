package com.anand8290.splitlite.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Field names match the Postgres columns in /supabase/migrations.
// All money is stored as Long minor units (paise, cents) — never floating point.

@Serializable
data class GroupSummary(
    val id: String,
    val name: String,
    val currency: String,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("my_balance") val myBalance: Long,
)

@Serializable
data class GroupInfo(
    val id: String,
    val name: String,
    val currency: String,
    @SerialName("invite_code") val inviteCode: String,
)

@Serializable
data class Member(
    val id: String,
    val name: String,
    @SerialName("claimed_by") val claimedBy: String? = null,
)

@Serializable
data class Split(
    @SerialName("member_id") val memberId: String,
    @SerialName("amount_minor") val amountMinor: Long,
)

@Serializable
data class Expense(
    val id: String,
    val title: String,
    @SerialName("amount_minor") val amountMinor: Long,
    @SerialName("payer_id") val payerId: String,
    @SerialName("is_settlement") val isSettlement: Boolean = false,
    @SerialName("spent_on") val spentOn: String,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("expense_splits") val splits: List<Split> = emptyList(),
)

@Serializable
data class GroupDetail(
    val group: GroupInfo,
    val members: List<Member>,
    val expenses: List<Expense>,
    val myMemberId: String?,
)

@Serializable
data class GroupPreview(
    val id: String,
    val name: String,
    val members: List<PreviewMember>,
)

@Serializable
data class PreviewMember(val id: String, val name: String, val claimed: Boolean)

/** One suggested payment: [from] pays [to] the given amount. */
data class Transfer(val from: String, val to: String, val amountMinor: Long)
