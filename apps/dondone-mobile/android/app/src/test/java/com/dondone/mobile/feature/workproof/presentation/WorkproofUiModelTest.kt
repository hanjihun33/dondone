package com.dondone.mobile.feature.workproof.presentation

import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.data.workproof.WorkproofRemoteState
import com.dondone.mobile.domain.calculator.WorkproofCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
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
}
