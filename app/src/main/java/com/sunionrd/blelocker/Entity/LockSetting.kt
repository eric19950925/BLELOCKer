package com.sunionrd.blelocker.Entity

data class LockSetting(
    val config: LockConfig,
    val status: Int,
    val battery: Int,
    val batteryStatus: Int,
    val timestamp: Long
)
