package com.superwall.sdk.utilities

fun <T> createSuccessResult(value: T): Result<T> = Result.success(value)

fun <T : Throwable> createFailureResult(error: T): Result<Nothing> = Result.failure(error)
