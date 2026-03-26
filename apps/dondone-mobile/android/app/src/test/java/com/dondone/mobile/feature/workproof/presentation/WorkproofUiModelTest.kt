package com.dondone.mobile.feature.workproof.presentation

import com.dondone.mobile.app.session.WorkproofCurrentLocationStatus
import com.dondone.mobile.app.session.WorkproofCurrentLocationUiState
import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.data.workproof.WorkproofRemotePayload
import com.dondone.mobile.data.workproof.WorkproofRemoteState
import com.dondone.mobile.data.workproof.WorkproofWorkplacePayload
import com.dondone.mobile.domain.calculator.WorkproofCalculator
import com.dondone.mobile.domain.model.WorkRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class WorkproofUiModelTest {
    @Test
    fun `future days are marked unavailable instead of missing`() {
        val uiModel = DemoSeedFactory.create().toWorkproofUiModel()

        assertEquals(WorkproofCalendarTone.UNAVAILABLE, uiModel.calendarDayTones[29])
        assertEquals(WorkproofCalendarTone.UNAVAILABLE, uiModel.calendarDayTones[31])
    }

    @Test
    fun `verified day count matches shared calculator snapshot`() {
        val state = DemoSeedFactory.create()

        val uiModel = state.toWorkproofUiModel()

        assertEquals(
            WorkproofCalculator.visibleRecords(state).size,
            uiModel.recentRecords.size
        )
    }

    @Test
    fun `non base month cells stay unavailable instead of missing`() {
        val state = DemoSeedFactory.create()
        val uiModel = state.toWorkproofUiModel()
        val baseMonth = YearMonth.of(uiModel.calendarBaseYear, uiModel.calendarBaseMonth)

        val nextMonthCells = buildWorkproofCalendarCells(
            displayedMonth = baseMonth.plusMonths(1),
            baseMonth = baseMonth,
            currentDay = uiModel.calendarCurrentDay,
            dayTones = uiModel.calendarDayTones
        )

        assertTrue(nextMonthCells.filter { it.dayLabel.isNotBlank() }.all { it.tone == WorkproofCalendarTone.UNAVAILABLE })
    }

    @Test
    fun `clock in is available until recorded`() {
        val uiModel = DemoSeedFactory.create().toWorkproofUiModel()

        assertTrue(uiModel.summary.canClockIn)
        assertTrue(!uiModel.summary.canClockOut)
    }

    @Test
    fun `clock out becomes available after clock in`() {
        val seed = DemoSeedFactory.create()
        val state = seed.copy(
            workproof = seed.workproof.copy(
                today = seed.workproof.today.copy(
                    clockIn = "09:00",
                    clockOut = null
                )
            )
        )

        val uiModel = state.toWorkproofUiModel()

        assertTrue(!uiModel.summary.canClockIn)
        assertTrue(uiModel.summary.canClockOut)
    }

    @Test
    fun `fallback workproof shows demo data notice and disables remote punch actions`() {
        val uiModel = DemoSeedFactory.create().toWorkproofUiModel(
            remoteState = WorkproofRemoteState.empty("연결된 근무지가 없습니다."),
            isAuthenticated = true
        )

        assertEquals("가상 예시 데이터", uiModel.fallbackNoticeTitle)
        assertFalse(uiModel.summary.canClockIn)
        assertFalse(uiModel.summary.canClockOut)
    }

    @Test
    fun `authenticated remote workproof disables punch actions until current location is ready`() {
        val uiModel = DemoSeedFactory.create().toWorkproofUiModel(
            remoteState = remoteContentState(),
            isAuthenticated = true,
            currentLocationUiState = WorkproofCurrentLocationUiState(
                status = WorkproofCurrentLocationStatus.PERMISSION_REQUIRED
            )
        )

        assertFalse(uiModel.summary.canClockIn)
        assertFalse(uiModel.summary.canClockOut)
        assertEquals(WorkproofCurrentLocationStatus.PERMISSION_REQUIRED, uiModel.summary.currentLocationStatus)
        assertFalse(uiModel.summary.isWithinWorkplaceRadius)
    }

    @Test
    fun `authenticated remote workproof enables punch action when current location is ready`() {
        val uiModel = DemoSeedFactory.create().toWorkproofUiModel(
            remoteState = remoteContentState(),
            isAuthenticated = true,
            currentLocationUiState = WorkproofCurrentLocationUiState(
                status = WorkproofCurrentLocationStatus.READY
            )
        )

        assertTrue(uiModel.summary.canClockIn)
        assertEquals(null, uiModel.summary.currentLocationStatus)
    }

    @Test
    fun `excluded record is labeled rejected`() {
        val seed = DemoSeedFactory.create()
        val state = seed.copy(
            workproof = seed.workproof.copy(
                records = listOf(
                    WorkRecord(
                        id = "WP-REJECTED",
                        workDate = LocalDate.of(2026, 3, 27),
                        day = 27,
                        inTime = "09:00",
                        outTime = "18:00",
                        modified = true,
                        attachments = 0,
                        reflectionStatus = "EXCLUDED"
                    )
                )
            )
        )

        val uiModel = state.toWorkproofUiModel()

        assertEquals("반려됨", uiModel.recentRecords.first().statusText)
        assertEquals(null, uiModel.recentRecords.first().detailText)
    }

    @Test
    fun `excluded record shows rejection memo in detail text`() {
        val seed = DemoSeedFactory.create()
        val state = seed.copy(
            workproof = seed.workproof.copy(
                records = listOf(
                    WorkRecord(
                        id = "WP-REJECTED-MEMO",
                        workDate = LocalDate.of(2026, 3, 27),
                        day = 27,
                        inTime = "09:00",
                        outTime = "18:00",
                        modified = true,
                        attachments = 0,
                        reflectionStatus = "EXCLUDED",
                        decisionMemo = "증빙이 부족합니다."
                    )
                )
            )
        )

        val uiModel = state.toWorkproofUiModel()

        assertEquals(
            "반려 사유: 증빙이 부족합니다.",
            uiModel.recentRecords.first().detailText
        )
    }

    @Test
    fun `needs review record hides duplicated status detail`() {
        val seed = DemoSeedFactory.create()
        val state = seed.copy(
            workproof = seed.workproof.copy(
                records = listOf(
                    WorkRecord(
                        id = "WP-REVIEW",
                        workDate = LocalDate.of(2026, 3, 27),
                        day = 27,
                        inTime = "09:00",
                        outTime = "18:00",
                        modified = true,
                        attachments = 0,
                        reflectionStatus = "NEEDS_REVIEW"
                    )
                )
            )
        )

        val uiModel = state.toWorkproofUiModel()

        assertEquals("검토 중", uiModel.recentRecords.first().statusText)
        assertEquals(null, uiModel.recentRecords.first().detailText)
    }

    private fun remoteContentState(): WorkproofRemoteState {
        return WorkproofRemoteState.content(
            WorkproofRemotePayload(
                workplace = WorkproofWorkplacePayload(
                    workplaceId = 1L,
                    name = "DonDone Cafe",
                    address = "서울시 강남구 테헤란로",
                    latitude = 37.5013,
                    longitude = 127.0396,
                    allowedRadiusMeters = 100
                ),
                records = emptyList()
            )
        )
    }
}
