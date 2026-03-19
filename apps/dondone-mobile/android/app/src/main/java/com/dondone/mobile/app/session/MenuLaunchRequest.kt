package com.dondone.mobile.app.session

enum class MenuLaunchTarget {
    PROOF_DOCUMENT,
    CLAIM_DOCUMENT,
    CLAIM_SHEET
}

data class MenuLaunchRequest(
    val target: MenuLaunchTarget,
    val requestId: Long
)
