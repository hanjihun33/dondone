package com.dondone.mobile.app.session

import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.data.workproof.WorkproofRemotePayload
import com.dondone.mobile.data.workproof.WorkproofRecordPayload
import com.dondone.mobile.data.workproof.WorkproofWorkplacePayload
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DemoSessionMappersTest {
    @Test
    fun `reflected record is not treated as modified when backend modified flag is false`() {
        val state = DemoSeedFactory.create()
        val payload = WorkproofRemotePayload(
            workplace = WorkproofWorkplacePayload(
                workplaceId = 1L,
                name = "DonDone Cafe",
                address = "서울시 강남구 테헤란로",
                latitude = 37.5013,
                longitude = 127.0396,
                allowedRadiusMeters = 100
            ),
            records = listOf(
                WorkproofRecordPayload(
                    recordId = 101L,
                    workDate = LocalDate.of(2026, 3, 20),
                    status = "COMPLETED",
                    checkInDeviceAt = LocalDateTime.of(2026, 3, 20, 9, 0),
                    checkOutDeviceAt = LocalDateTime.of(2026, 3, 20, 17, 0),
                    recognizedClockInAt = LocalDateTime.of(2026, 3, 20, 9, 0),
                    recognizedClockOutAt = LocalDateTime.of(2026, 3, 20, 17, 0),
                    workedMinutes = 480L,
                    modified = false,
                    reflectionStatus = "REFLECTED",
                    decisionMemo = null,
                    riskFlags = emptyList()
                )
            )
        )

        val synced = state.syncRemoteWorkproof(payload)

        assertFalse(synced.workproof.records.single().modified)
    }

    @Test
    fun `backend modified flag still marks record as modified`() {
        val state = DemoSeedFactory.create()
        val payload = WorkproofRemotePayload(
            workplace = WorkproofWorkplacePayload(
                workplaceId = 1L,
                name = "DonDone Cafe",
                address = "서울시 강남구 테헤란로",
                latitude = 37.5013,
                longitude = 127.0396,
                allowedRadiusMeters = 100
            ),
            records = listOf(
                WorkproofRecordPayload(
                    recordId = 102L,
                    workDate = LocalDate.of(2026, 3, 21),
                    status = "COMPLETED",
                    checkInDeviceAt = LocalDateTime.of(2026, 3, 21, 9, 0),
                    checkOutDeviceAt = LocalDateTime.of(2026, 3, 21, 17, 0),
                    recognizedClockInAt = LocalDateTime.of(2026, 3, 21, 9, 0),
                    recognizedClockOutAt = LocalDateTime.of(2026, 3, 21, 17, 0),
                    workedMinutes = 480L,
                    modified = true,
                    reflectionStatus = "REFLECTED",
                    decisionMemo = null,
                    riskFlags = emptyList()
                )
            )
        )

        val synced = state.syncRemoteWorkproof(payload)

        assertTrue(synced.workproof.records.single().modified)
    }
}
