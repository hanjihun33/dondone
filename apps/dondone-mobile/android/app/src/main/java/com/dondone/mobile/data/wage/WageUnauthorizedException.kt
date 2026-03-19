package com.dondone.mobile.data.wage

import java.io.IOException

class WageUnauthorizedException(
    message: String = "세션이 만료되어 다시 로그인해 주세요."
) : IOException(message)
