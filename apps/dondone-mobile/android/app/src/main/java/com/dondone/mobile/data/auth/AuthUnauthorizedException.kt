package com.dondone.mobile.data.auth

class AuthUnauthorizedException(
    message: String = "세션이 만료되어 다시 로그인해 주세요."
) : IllegalStateException(message)
