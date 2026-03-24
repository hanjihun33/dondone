package com.dondone.mobile.feature.menu.presentation

import com.dondone.mobile.data.auth.AuthSession
import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfCreateUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuUiModelTest {
    @Test
    fun `menu documents exclude receipt and keep service receipt entry separate`() {
        val uiModel = DemoSeedFactory.create().toMenuUiModel(
            session = null,
            remittanceRemoteState = defaultRemoteState(),
            workproofPdfCreateUiState = WorkproofPdfCreateUiState()
        )

        assertEquals(2, uiModel.documents.size)
        assertTrue(uiModel.documents.none { it.title == "송금 영수증" })
        assertNotNull(uiModel.receipt)
    }

    @Test
    fun `submitted transfer exposes pending receipt session`() {
        val seed = DemoSeedFactory.create()
        val uiModel = seed
            .copy(remittance = seed.remittance.copy(status = TransferStatus.SUBMITTED))
            .toMenuUiModel(
                session = null,
                remittanceRemoteState = defaultRemoteState(),
                workproofPdfCreateUiState = WorkproofPdfCreateUiState()
            )

        val receipt = requireNotNull(uiModel.receipt)

        assertEquals(MenuReceiptStatus.Pending, receipt.status)
        assertEquals("확인 중", receipt.statusText)
        assertEquals("세폴리아", receipt.networkLabel)
        assertEquals(seed.remittance.txHash, receipt.txHashFullText)
        assertEquals(
            "네트워크 확인이 끝나면 영수증 상태가 자동으로 완료로 바뀝니다.",
            receipt.pendingNoticeText
        )
        assertTrue(receipt.shareText.contains("상태: 확인 중"))
        assertTrue(receipt.shareText.contains(receipt.explorerUrl))
    }

    @Test
    fun `idle state still exposes latest confirmed receipt session`() {
        val uiModel = DemoSeedFactory.create().toMenuUiModel(
            session = null,
            remittanceRemoteState = defaultRemoteState(),
            workproofPdfCreateUiState = WorkproofPdfCreateUiState()
        )
        val receipt = requireNotNull(uiModel.receipt)

        assertEquals(MenuReceiptStatus.Confirmed, receipt.status)
        assertEquals("완료", receipt.statusText)
        assertNull(receipt.pendingNoticeText)
        assertEquals("업데이트 2026-03-19 09:12", receipt.updatedAtText)
    }

    @Test
    fun `wallet mode receipt share text uses usdc amount`() {
        val seed = DemoSeedFactory.create()
        val uiModel = seed
            .copy(
                remittance = seed.remittance.copy(
                    status = TransferStatus.CONFIRMED,
                    destinationMode = TransferDestinationMode.WALLET
                )
            )
            .toMenuUiModel(
                session = null,
                remittanceRemoteState = defaultRemoteState(),
                workproofPdfCreateUiState = WorkproofPdfCreateUiState()
            )

        val receipt = requireNotNull(uiModel.receipt)

        assertTrue(receipt.shareText.contains("금액: ${'$'}360 USDC"))
    }

    @Test
    fun `session maps company and workplace names for profile sheet`() {
        val session = AuthSession(
            accessToken = "token",
            tokenType = "Bearer",
            expiresAtEpochMillis = Long.MAX_VALUE,
            userId = 1L,
            email = "menu@test.com",
            name = "메뉴 사용자",
            phoneNumber = "01012345678",
            companyCode = "C-001",
            companyName = "돈던건설",
            workplaceName = "광주 상단지점"
        )
        val uiModel = DemoSeedFactory.create().toMenuUiModel(
            session = session,
            remittanceRemoteState = defaultRemoteState(),
            workproofPdfCreateUiState = WorkproofPdfCreateUiState()
        )

        val menuSession = requireNotNull(uiModel.session)
        assertEquals("돈던건설", menuSession.companyName)
        assertEquals("광주 상단지점", menuSession.workplaceName)
        assertEquals("010-1234-5678", menuSession.phoneNumber)
    }
}

private fun defaultRemoteState(): RemittanceRemoteState =
    RemittanceRemoteState.unauthenticated("login required")
