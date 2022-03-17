package com.example.blelocker.Entity

data class HomeLocks(
    val macAddress: String,

    val displayName: String? = "",

    val deviceName: String? = "",

    var lockStatus: Int
)