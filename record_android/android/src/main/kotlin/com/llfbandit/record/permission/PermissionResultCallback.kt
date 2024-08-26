package com.qiaovc.record.permission

fun interface PermissionResultCallback {
    fun onResult(granted: Boolean)
}