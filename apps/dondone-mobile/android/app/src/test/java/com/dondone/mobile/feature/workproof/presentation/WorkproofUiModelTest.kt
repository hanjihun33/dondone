package com.dondone.mobile.feature.workproof.presentation

import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.domain.calculator.WorkproofCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

        assertEquals(WorkproofCalculator.verify(state).verifiedDays, uiModel.summary.verifiedDays)
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
    fun `today times stay null until recorded`() {
        val uiModel = DemoSeedFactory.create().toWorkproofUiModel()

        assertNull(uiModel.summary.todayInTime)
        assertNull(uiModel.summary.todayOutTime)
    }

    @Test
    fun `today times include date once recorded`() {
        val seed = DemoSeedFactory.create()
        val state = seed.copy(
            workproof = seed.workproof.copy(
                today = seed.workproof.today.copy(
                    clockIn = "09:00",
                    clockOut = "18:00"
                )
            )
        )

        val uiModel = state.toWorkproofUiModel()

        assertEquals("2026.03.28 · 09:00", uiModel.summary.todayInTime)
        assertEquals("2026.03.28 · 18:00", uiModel.summary.todayOutTime)
    }
}
