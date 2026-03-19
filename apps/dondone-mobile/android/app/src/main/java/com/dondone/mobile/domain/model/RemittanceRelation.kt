package com.dondone.mobile.domain.model

fun remittanceRelationCodeToLabel(code: String): String = when (code.trim().uppercase()) {
    "FAMILY" -> "가족"
    "SPOUSE" -> "배우자"
    "PARENT" -> "부모"
    "CHILD" -> "자녀"
    "SIBLING" -> "형제자매"
    "RELATIVE" -> "친척"
    "FRIEND" -> "친구"
    else -> "기타"
}

fun remittanceRelationLabelToCode(label: String): String = when (label.trim()) {
    "가족" -> "FAMILY"
    "배우자" -> "SPOUSE"
    "부모" -> "PARENT"
    "자녀" -> "CHILD"
    "형제자매" -> "SIBLING"
    "친척" -> "RELATIVE"
    "친구" -> "FRIEND"
    else -> "OTHER"
}
