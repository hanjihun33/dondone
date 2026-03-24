package com.dondone.mobile.app.session

enum class WorkproofLaunchTarget {
    PDF_CREATION
}

data class WorkproofLaunchRequest(
    val target: WorkproofLaunchTarget,
    val requestId: Long
)
