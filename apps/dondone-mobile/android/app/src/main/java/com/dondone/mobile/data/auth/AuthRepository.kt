package com.dondone.mobile.data.auth

interface AuthRepository {
    suspend fun restore(): AuthSession?
    suspend fun signup(name: String, email: String, password: String, phoneNumber: String): AuthSession
    suspend fun login(email: String, password: String): AuthSession
    suspend fun updateProfile(session: AuthSession, name: String, phoneNumber: String): AuthSession
    suspend fun logout()
}
