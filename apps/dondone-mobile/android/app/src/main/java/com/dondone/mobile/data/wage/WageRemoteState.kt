package com.dondone.mobile.data.wage

enum class WageRemoteMode {
    LOADING,
    UNAUTHENTICATED,
    EMPTY,
    ERROR,
    CONTENT
}

data class WageRemoteState(
    val mode: WageRemoteMode,
    val payload: WageRemotePayload? = null,
    val errorMessage: String? = null
) {
    val isAuthenticated: Boolean
        get() = mode != WageRemoteMode.UNAUTHENTICATED

    val isLoading: Boolean
        get() = mode == WageRemoteMode.LOADING

    companion object {
        fun loading() = WageRemoteState(mode = WageRemoteMode.LOADING)

        fun unauthenticated(message: String) = WageRemoteState(
            mode = WageRemoteMode.UNAUTHENTICATED,
            errorMessage = message
        )

        fun empty(message: String) = WageRemoteState(
            mode = WageRemoteMode.EMPTY,
            errorMessage = message
        )

        fun error(message: String) = WageRemoteState(
            mode = WageRemoteMode.ERROR,
            errorMessage = message
        )

        fun content(payload: WageRemotePayload) = WageRemoteState(
            mode = WageRemoteMode.CONTENT,
            payload = payload
        )
    }
}
