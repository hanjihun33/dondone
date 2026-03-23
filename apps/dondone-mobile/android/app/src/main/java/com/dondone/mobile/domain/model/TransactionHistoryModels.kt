package com.dondone.mobile.domain.model

import java.time.LocalDateTime

enum class TransactionCategory(val label: String) {
    SALARY("급여"),
    FOOD("식비"),
    CAFE("카페/간식"),
    SHOPPING("쇼핑"),
    TRANSPORT("교통"),
    LIVING("생활"),
    TRANSFER("이체"),
    ETC("기타")
}

enum class TransactionDirection {
    INCOME,
    EXPENSE
}

data class TransactionRecord(
    val id: String,
    val walletId: String,
    val occurredAt: LocalDateTime,
    val amount: Int,
    val direction: TransactionDirection,
    val counterpartyName: String,
    val counterpartyAddress: String? = null,
    val category: TransactionCategory,
    val memo: String = "",
    val methodLabel: String = "지갑 송금",
    val feeAmount: Int = 0
)
