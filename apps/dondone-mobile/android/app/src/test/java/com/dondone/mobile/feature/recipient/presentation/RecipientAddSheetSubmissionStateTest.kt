package com.dondone.mobile.feature.recipient.presentation

import com.dondone.mobile.app.session.RemittanceActionUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipientAddSheetSubmissionStateTest {

    @Test
    fun `awaiting error keeps inline message visible and sheet open`() {
        val actionUiState = RemittanceActionUiState(
            isSubmitting = false,
            message = "수신 지갑을 추가하지 못했어요.",
            isError = true
        )

        assertEquals(
            "수신 지갑을 추가하지 못했어요.",
            resolveRecipientSheetErrorMessage(
                isAwaitingResult = true,
                actionUiState = actionUiState
            )
        )
        assertFalse(shouldCloseRecipientSheetAfterResult(true, actionUiState))
    }

    @Test
    fun `awaiting success closes sheet without inline error`() {
        val actionUiState = RemittanceActionUiState(
            isSubmitting = false,
            message = "수신 지갑을 추가했어요.",
            isError = false
        )

        assertNull(resolveRecipientSheetErrorMessage(true, actionUiState))
        assertTrue(shouldCloseRecipientSheetAfterResult(true, actionUiState))
    }
}
