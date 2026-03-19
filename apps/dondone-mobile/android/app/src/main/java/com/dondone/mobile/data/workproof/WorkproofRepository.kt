package com.dondone.mobile.data.workproof

import com.dondone.mobile.domain.model.WorkproofData
import java.io.IOException

interface WorkproofRepository {
    suspend fun load(accessToken: String): WorkproofRemoteState

    suspend fun clockIn(accessToken: String, workproof: WorkproofData): WorkproofRemoteState

    suspend fun clockOut(accessToken: String, workproof: WorkproofData): WorkproofRemoteState
}

class WorkproofUnauthorizedException(message: String = "세션이 만료되어 다시 로그인해 주세요.") : IOException(message)
