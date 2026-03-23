package com.dondone.mobile.domain.advance

import com.dondone.mobile.data.advance.AdvanceEligibilityPayload
import com.dondone.mobile.data.advance.AdvanceRemoteState
import com.dondone.mobile.data.demo.DemoSeedFactory
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvanceContractStateTest {

    @Test
    fun `default seed keeps success state but exposes pending review reason`() {
        val state = DemoSeedFactory.create().toAdvanceContractState()

        assertEquals(AdvanceSurfaceState.SUCCESS, state.surfaceState)
        assertTrue(state.blockReasonCodes.isEmpty())
        assertTrue(state.canRequest)
        assertTrue(state.disclaimerText.contains("데모 시뮬레이션"))
    }

    @Test
    fun `clean reflected state becomes success and enables request`() {
        val seed = DemoSeedFactory.create()
        val cleaned = seed.copy(
            workproof = seed.workproof.copy(audit = emptyList()),
            wage = seed.wage.copy(workDays = 3)
        )

        val state = cleaned.toAdvanceContractState()

        assertEquals(AdvanceSurfaceState.SUCCESS, state.surfaceState)
        assertTrue(state.blockReasonCodes.isEmpty())
        assertTrue(state.canRequest)
        assertEquals("T0", state.repaymentTier)
    }

    @Test
    fun `insufficient verified work becomes blocked`() {
        val seed = DemoSeedFactory.create()
        val blocked = seed.copy(
            workproof = seed.workproof.copy(records = emptyList(), audit = emptyList()),
            wage = seed.wage.copy(workDays = 0, totalHours = 0, overtimeHours = 0, nightHours = 0),
            advance = seed.advance.copy(
                used = 0,
                selectedRequest = 0,
                previousRepaymentGood = false,
                bonusLimit = 0
            )
        )

        val state = blocked.toAdvanceContractState()

        assertEquals(AdvanceSurfaceState.BLOCKED, state.surfaceState)
        assertTrue(state.blockReasonCodes.contains("INSUFFICIENT_VERIFIED_WORK"))
        assertFalse(state.canRequest)
    }

    @Test
    fun `remote loading state disables request and hides demo amount`() {
        val state = DemoSeedFactory.create().toAdvanceContractState(AdvanceRemoteState.loading())

        assertEquals(AdvanceSurfaceState.LOADING, state.surfaceState)
        assertEquals(0L, state.availableAmountOverride)
        assertFalse(state.canRequest)
    }

    @Test
    fun `remote empty state does not fallback to demo success`() {
        val state = DemoSeedFactory.create().toAdvanceContractState(
            AdvanceRemoteState.empty("연결된 근무지가 없어 실연동 한도를 보여줄 수 없어요.")
        )

        assertEquals(AdvanceSurfaceState.EMPTY, state.surfaceState)
        assertEquals("실연동", state.sourceLabelText)
        assertFalse(state.canRequest)
    }

    @Test
    fun `remote payday closure uses closing title and message`() {
        val remoteState = AdvanceRemoteState.content(
            workplaceName = "실연동 · SSAFY",
            eligibility = AdvanceEligibilityPayload(
                workplaceId = 1L,
                availableAmount = 0L,
                repaymentTier = "C",
                blockReasonCodes = listOf("ADVANCE_WINDOW_CLOSED_TODAY"),
                noticeReasonCodes = emptyList(),
                estimatedRepaymentDate = "2026-03-25",
                disclaimer = "demo",
                needsReviewRecordCount = 0
            ),
            requests = emptyList()
        )

        val state = DemoSeedFactory.create().toAdvanceContractState(remoteState)

        assertEquals(AdvanceSurfaceState.BLOCKED, state.surfaceState)
        assertEquals("오늘은 신청이 마감됐어요", state.stateTitleText)
        assertTrue(state.blockReasonTexts.contains("오늘은 신청이 마감됐어요"))
        assertEquals("마감 이유 보기", state.actionText)
        assertFalse(state.canRequest)
    }

    @Test
    fun `remote next cycle guidance shows next cycle copy`() {
        val nextCycleDate = YearMonth.now().plusMonths(1).atDay(25).toString()
        val remoteState = AdvanceRemoteState.content(
            workplaceName = "실연동 · SSAFY",
            eligibility = AdvanceEligibilityPayload(
                workplaceId = 1L,
                availableAmount = 50_000L,
                repaymentTier = "C",
                blockReasonCodes = emptyList(),
                noticeReasonCodes = emptyList(),
                estimatedRepaymentDate = nextCycleDate,
                disclaimer = "demo",
                needsReviewRecordCount = 0
            ),
            requests = emptyList()
        )

        val state = DemoSeedFactory.create().toAdvanceContractState(remoteState)

        assertEquals(AdvanceSurfaceState.SUCCESS, state.surfaceState)
        assertEquals("다음 달 급여 회차 기준으로 확인했어요", state.stateTitleText)
        assertEquals("다음 달 급여 회차 기준으로 신청 가능 금액을 불러왔어요.", state.stateBodyText)
        assertEquals("다음 회차 보기", state.actionText)
        assertTrue(state.canRequest)
    }

    @Test
    fun `remote pending review lock shows review guidance copy`() {
        val remoteState = AdvanceRemoteState.content(
            workplaceName = "실연동 · SSAFY",
            eligibility = AdvanceEligibilityPayload(
                workplaceId = 1L,
                availableAmount = 0L,
                repaymentTier = "C",
                blockReasonCodes = emptyList(),
                noticeReasonCodes = listOf("PENDING_WORKPROOF_REVIEW"),
                estimatedRepaymentDate = YearMonth.now().atDay(25).toString(),
                disclaimer = "demo",
                needsReviewRecordCount = 1
            ),
            requests = emptyList()
        )

        val state = DemoSeedFactory.create().toAdvanceContractState(remoteState)

        assertEquals(AdvanceSurfaceState.BLOCKED, state.surfaceState)
        assertEquals("확인 후 신청이 가능해요", state.stateTitleText)
        assertEquals("확인 필요한 기록이 정리되면 바로 신청 가능 금액을 다시 확인할 수 있어요.", state.stateBodyText)
        assertEquals("기록 확인", state.actionText)
        assertFalse(state.canRequest)
    }
}
