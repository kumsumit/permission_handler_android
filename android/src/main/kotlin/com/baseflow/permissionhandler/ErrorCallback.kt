package com.baseflow.permissionhandler

fun interface ErrorCallback {
    fun onError(errorCode: String, errorDescription: String)
}