package com.dondone.mobile.app.session

import com.dondone.mobile.data.auth.AuthSession

data class AuthUiState(
    val isRestoring: Boolean = false,
    val isSubmitting: Boolean = false,
    val session: AuthSession? = null,
    val errorMessage: String? = null
) {
    val isAuthenticated: Boolean
        get() = session != null

    companion object {
        fun restoring() = AuthUiState(isRestoring = true)

        fun unauthenticated(errorMessage: String? = null) = AuthUiState(
            isRestoring = false,
            isSubmitting = false,
            session = null,
            errorMessage = errorMessage
        )

        fun submitting(previousError: String? = null) = AuthUiState(
            isRestoring = false,
            isSubmitting = true,
            session = null,
            errorMessage = previousError
        )

        fun authenticated(session: AuthSession) = AuthUiState(
            isRestoring = false,
            isSubmitting = false,
            session = session,
            errorMessage = null
        )
    }
}
