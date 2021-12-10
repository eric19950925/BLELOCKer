package com.example.blelocker.entity

data class LockSetting(
    val config: LockConfig,
    val status: Int,
    val battery: Int,
    val batteryStatus: Int,
    val timestamp: Long
)
