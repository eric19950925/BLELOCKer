package com.example.blelocker.Entity

object LockStatus {
    const val LOCKED = 0
    const val UNLOCKED = 1
    const val UNKNOWN = 2
    const val LOADING = 4

    const val BATTERY_GOOD = 0
    const val BATTERY_LOW = 1
    const val BATTERY_ALERT = 2
}

object BleStatus {
    const val UNCONNECT = 0
    const val CONNECTTING = 1
    const val CONNECT = 2
}
