package com.dondone.mobile.data.remote

import com.dondone.mobile.BuildConfig
import org.json.JSONObject
import java.io.IOException

object BackendApiSupport {
    val baseUrl: String = BuildConfig.DONDONE_API_BASE_URL
}

class BackendApiException(message: String) : IOException(message)

fun parseBackendErrorMessage(
    responseBody: String,
    fallbackMessage: String
): String {
    return runCatching {
        JSONObject(responseBody.ifBlank { "{}" }).optString("message")
    }.getOrNull().orEmpty().ifBlank { fallbackMessage }
}
