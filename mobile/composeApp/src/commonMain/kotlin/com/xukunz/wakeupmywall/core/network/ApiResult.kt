package com.xukunz.wakeupmywall.core.network

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val reason: ApiFailure, val message: String) : ApiResult<Nothing>
}

enum class ApiFailure { TIMEOUT, UNAUTHORIZED, NOT_FOUND, SERVER, NETWORK, DECODING }
