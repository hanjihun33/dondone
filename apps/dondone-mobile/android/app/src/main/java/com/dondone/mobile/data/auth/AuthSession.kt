package com.dondone.mobile.data.auth

private const val EXPIRES_IN_MILLIS_THRESHOLD = 31_536_000L

data class AuthSession(
    val accessToken: String,
    val tokenType: String,
    val expiresAtEpochMillis: Long,
    val userId: Long,
    val email: String,
    val name: String,
    val phoneNumber: String?,
    val companyCode: String?
) {
    fun isExpired(nowEpochMillis: Long = System.currentTimeMillis()): Boolean {
        return nowEpochMillis >= expiresAtEpochMillis
    }

    companion object {
        fun resolveExpiresAtEpochMillis(
            expiresIn: Long,
            nowEpochMillis: Long = System.currentTimeMillis()
        ): Long {
            val durationMillis = if (expiresIn > EXPIRES_IN_MILLIS_THRESHOLD) {
                expiresIn
            } else {
                expiresIn * 1_000L
            }
            return nowEpochMillis + durationMillis
        }
    }
}
