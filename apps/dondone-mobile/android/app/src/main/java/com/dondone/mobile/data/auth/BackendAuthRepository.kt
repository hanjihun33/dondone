package com.dondone.mobile.data.auth

import com.dondone.mobile.data.remote.BackendApiException
import com.dondone.mobile.data.remote.BackendApiSupport
import com.dondone.mobile.data.remote.parseBackendErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

class BackendAuthRepository(
    private val sessionStore: AuthSessionStore,
    private val client: OkHttpClient = OkHttpClient()
) : AuthRepository {

    override suspend fun restore(): AuthSession? = withContext(Dispatchers.IO) {
        val session = sessionStore.read() ?: return@withContext null
        if (session.isExpired()) {
            sessionStore.clear()
            return@withContext null
        }
        session
    }

    override suspend fun signup(
        name: String,
        email: String,
        password: String,
        phoneNumber: String
    ): AuthSession = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("name", name)
            .put("phoneNumber", phoneNumber)
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/auth/signup")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw BackendApiException(
                    parseBackendErrorMessage(
                        responseBody = responseBody,
                        fallbackMessage = "회원가입에 실패했어요. 입력한 정보를 다시 확인해 주세요."
                    )
                )
            }
        }

        login(email, password)
    }

    override suspend fun login(email: String, password: String): AuthSession = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/auth/login")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw BackendApiException(
                    parseBackendErrorMessage(
                        responseBody = responseBody,
                        fallbackMessage = "로그인에 실패했어요. 이메일과 비밀번호를 다시 확인해 주세요."
                    )
                )
            }

            val json = JSONObject(responseBody.ifBlank { "{}" })
            val data = json.getJSONObject("data")
            val session = AuthSession(
                accessToken = data.getString("accessToken"),
                tokenType = data.optString("tokenType", "Bearer"),
                expiresAtEpochMillis = AuthSession.resolveExpiresAtEpochMillis(data.getLong("expiresIn")),
                userId = data.getLong("userId"),
                email = data.getString("email"),
                name = data.getString("name"),
                phoneNumber = data.optString("phoneNumber").ifBlank { null }
            )
            sessionStore.save(session)
            session
        }
    }

    override suspend fun updateProfile(
        session: AuthSession,
        name: String,
        phoneNumber: String
    ): AuthSession = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("name", name)
            .put("phoneNumber", phoneNumber)
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/auth/me")
            .header("Authorization", "Bearer ${session.accessToken}")
            .put(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = parseBackendErrorMessage(
                    responseBody = responseBody,
                    fallbackMessage = "내 정보를 수정하지 못했어요. 잠시 후 다시 시도해 주세요."
                )
                if (response.code == 401 || response.code == 403) {
                    throw AuthUnauthorizedException(message)
                }
                throw BackendApiException(message)
            }

            val json = JSONObject(responseBody.ifBlank { "{}" })
            val data = json.getJSONObject("data")
            val updatedSession = session.copy(
                name = data.getString("name"),
                phoneNumber = data.optString("phoneNumber").ifBlank { null }
            )
            sessionStore.save(updatedSession)
            updatedSession
        }
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        sessionStore.clear()
    }
}
