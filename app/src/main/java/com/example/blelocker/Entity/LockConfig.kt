package com.example.blelocker.Entity

data class LockConfig(
    val orientation: LockOrientation,
    val isSoundOn: Boolean,
    val isVacationModeOn: Boolean,
    val isAutoLock: Boolean,
    val autoLockTime: Int,
    val latitude: Double? = null,
    val longitude: Double? = null
)
