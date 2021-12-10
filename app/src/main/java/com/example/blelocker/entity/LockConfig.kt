package com.example.blelocker.entity

data class LockConfig(
//    val orientation: LockOrientation,
    val orientation: String,
    val isSoundOn: Boolean,
    val isVacationModeOn: Boolean,
    val isAutoLock: Boolean,
    val autoLockTime: Int,
    val latitude: Double? = null,
    val longitude: Double? = null
)
