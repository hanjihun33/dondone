package com.dondone.mobile.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthSessionTest {

    @Test
    fun `resolve expiresAt treats short values as seconds`() {
        val now = 1_000L

        val expiresAt = AuthSession.resolveExpiresAtEpochMillis(
            expiresIn = 86_400L,
            nowEpochMillis = now
        )

        assertEquals(now + 86_400_000L, expiresAt)
    }

    @Test
    fun `resolve expiresAt keeps large values as millis`() {
        val now = 5_000L

        val expiresAt = AuthSession.resolveExpiresAtEpochMillis(
            expiresIn = 86_400_000L,
            nowEpochMillis = now
        )

        assertEquals(now + 86_400_000L, expiresAt)
    }
}
