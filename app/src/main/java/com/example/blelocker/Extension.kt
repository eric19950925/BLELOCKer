package com.example.blelocker

import java.util.*


fun ByteArray.toHex(): String {
    return joinToString(", ") { "%02x".format(it).uppercase(Locale.getDefault()) }
}
fun hexToBytes(hexString: String): ByteArray? {
    val hex: CharArray = hexString.toCharArray()
    val length = hex.size / 2
    val rawData = ByteArray(length)
    for (i in 0 until length) {
        val high = Character.digit(hex[i * 2], 16)
        val low = Character.digit(hex[i * 2 + 1], 16)
        var value = high shl 4 or low
        if (value > 127) value -= 256
        rawData[i] = value.toByte()
    }
    return rawData
}