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

object MqttStatus {
    const val UNCONNECT = 0
    const val CONNECTTING = 1
    const val CONNECTED = 2
    const val CONNECTION_LOST = 3
    const val RECONNECTING = 4
}

object AdminCodeDialog {
    const val INSERT = 0
    const val UPDATE = 1
    const val FACTORY_RESET = 2
}