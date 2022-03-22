package com.sunionrd.blelocker.Entity

data class HomeLocks(
    val macAddress: String,

    val displayName: String? = "",

    val deviceName: String? = "",

    var lockStatus: Int
)