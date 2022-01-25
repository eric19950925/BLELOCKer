package com.example.blelocker.BluetoothUtils

import android.util.Base64
import android.util.Log
import com.example.blelocker.MainActivity
import com.example.blelocker.Entity.*
import com.example.blelocker.Entity.LockStatus.BATTERY_ALERT
import com.example.blelocker.Entity.LockStatus.BATTERY_GOOD
import com.example.blelocker.Entity.LockStatus.BATTERY_LOW
import com.example.blelocker.Entity.LockStatus.LOCKED
import com.example.blelocker.Entity.LockStatus.UNKNOWN
import com.example.blelocker.Entity.LockStatus.UNLOCKED
import com.example.blelocker.Exception.LockStatusException
import com.example.blelocker.toHex
import com.example.blelocker.unSignedInt
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class BleCmdRepository {

    private val commandSerial = AtomicInteger()

    private fun encrypt(key: ByteArray, data: ByteArray): ByteArray? {
        return try {
            val cipher: Cipher = Cipher.getInstance(MainActivity.CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted: ByteArray = cipher.doFinal(data)
//            Timber.d("encrypted:\n${encrypted.toHex()}")
            encrypted
        } catch (exception: Exception) {
//            Timber.d(exception)
            null
        }
    }

    fun decrypt(key: ByteArray, data: ByteArray): ByteArray? {
        return try {
            val cipher: Cipher = Cipher.getInstance(MainActivity.CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val original: ByteArray = cipher.doFinal(data)
//            Timber.d("decrypted: \n${original.toHex()}")
            original
        } catch (exception: Exception) {
//            Timber.d(exception)
            null
        }
    }

    private fun pad(data: ByteArray, padZero: Boolean = false): ByteArray {
        if (data.isEmpty()) throw IllegalArgumentException("Invalid command.")
        val padNumber = 16 - (data.size) % 16
        val padBytes = if (padZero) ByteArray(padNumber) else Random.nextBytes(padNumber)
//        println(padBytes.toHex())
        return if (data.size % 16 == 0) {
            data
        } else {
            data + padBytes
        }
    }

    private fun serialIncrementAndGet(): ByteArray {
        val serial = commandSerial.incrementAndGet()
        val array = ByteArray(2)
        val byteBuffer = ByteBuffer.allocate(4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putInt(serial)
        byteBuffer.flip()
        byteBuffer.get(array)
        return array
    }
    enum class D6Features(val byte: Int){
        SIZE(13),
        LOCK_ORIENTATION (0),
        KEYPRESS_BEEP (1),
        VACATION_MODE (2),
        AUTOLOCK (3),
        AUTOLOCK_DELAY ( 4),
        PREAMBLE (5),
        LOCK_STATUS (6),
        BATTERY (7),
        LOW_BATTERY (8),
        TIMESTAMP (9)
    }
    enum class Config(val byte: Int){
        SIZE(22),
        LOCK_ORIENTATION (0),
        KEYPRESS_BEEP (1),
        VACATION_MODE (2),
        AUTOLOCK (3),
        AUTOLOCK_DELAY ( 4),
        PREAMBLE (5),
        LATITUDE_INTEGER (6),
        LATITUDE_DECIMAL (10),
        LONGITUDE_INTEGER (14),
        LONGITUDE_DECIMAL (18)
    }
    fun createCommand(
        function: Int,
        key: ByteArray,
        data: ByteArray = byteArrayOf()
    ): ByteArray {
//        Timber.d("create command: [${String.format("%2x", function)}]")
        return when (function) {
            0xC0 -> {
                commandSerial.set(0)
                c0(serialIncrementAndGet(), key)
            }
            0xC1 -> c1(serialIncrementAndGet(), key, data)
            0xC7 -> c7(serialIncrementAndGet(), key, data)
            0xC8 -> c8(serialIncrementAndGet(), key, data)
            0xCC -> cc(serialIncrementAndGet(), key)
            0xCE -> ce(serialIncrementAndGet(), key, data)
//            0xD0 -> d0(serialIncrementAndGet(), key)
//            0xD1 -> d1(serialIncrementAndGet(), key, data)
//            0xD2 -> d2(serialIncrementAndGet(), key)
//            0xD3 -> d3(serialIncrementAndGet(), key, data)
            0xD4 -> d4(serialIncrementAndGet(), key)
            0xD5 -> d5(serialIncrementAndGet(), key, data)
//            0xD6 -> d6(serialIncrementAndGet(), key)
            0xD7 -> d7(serialIncrementAndGet(), key, data)
//            0xD8 -> d8(serialIncrementAndGet(), key)
//            0xD9 -> d9(serialIncrementAndGet(), key, data)
//            0xE0 -> e0(serialIncrementAndGet(), key)
//            0xE1 -> e1(serialIncrementAndGet(), key, data)
//            0xE4 -> e4(serialIncrementAndGet(), key)
//            0xE5 -> e5(serialIncrementAndGet(), key, data)
//            0xE6 -> e6(serialIncrementAndGet(), key, data)
//            0xE7 -> e7(serialIncrementAndGet(), key, data)
//            0xE8 -> e8(serialIncrementAndGet(), key, data)
//            0xEA -> ea(serialIncrementAndGet(), key)
//            0xEB -> eb(serialIncrementAndGet(), key, data)
//            0xEC -> ec(serialIncrementAndGet(), key, data)
//            0xED -> ed(serialIncrementAndGet(), key, data)
//            0xEE -> ee(serialIncrementAndGet(), key, data)
//            0xEF -> ef(serialIncrementAndGet(), key)
            else -> throw IllegalArgumentException("Unknown function")
        }
    }

    fun generateRandomBytes(size: Int): ByteArray = Random.nextBytes(size)

    /**
     * ByteArray [C0] data command, length 16 of random number.
     *
     * @return An encrypted byte array.
     * */
    fun c0(serial: ByteArray, aesKeyOne: ByteArray): ByteArray {
        if (serial.size != 2) throw IllegalArgumentException("Invalid serial")
        val sendByte = ByteArray(2)
        sendByte[0] = 0xC0.toByte() // function
        sendByte[1] = 0x10 // len
        return encrypt(aesKeyOne, pad(serial + sendByte + generateRandomBytes(0x10)))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }
    /**
     * ByteArray [C1] data command. To retrieve the token state.
     *
     * @return An encoded byte array of [C1] command.
     * */
    fun c1(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        token: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xC1.toByte() // function
        sendByte[1] = 0x08 // len=8
        return encrypt(aesKeyTwo, pad(serial + sendByte + token)) ?: throw IllegalArgumentException(
            "bytes cannot be null"
        )
    }
    fun c7(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(3)
        sendByte[0] = 0xC7.toByte() // function
        sendByte[1] = (code.size + 1).toByte() // len
        sendByte[2] = (code.size).toByte() // code size
//        Timber.d("c7: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }
    /**
     * ByteArray [C8] data command. Add admin code
     *
     * @return An encoded byte array of [C8] command.
     * */
    fun c8(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xC8.toByte() // function
        sendByte[1] = (code.size).toByte() // len
//        Timber.d("c8: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }
    /**
     * ByteArray [CC] data command. To bolt the lock orientation.
     *
     * @return An encoded byte array of [CC] command.
     * */
    fun cc(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xCC.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte)) ?: throw IllegalArgumentException(
            "bytes cannot be null"
        )
    }

    /**
     * ByteArray [CE] data command. Add admin code
     *
     * @return An encoded byte array of [CE] command.
     * */
    fun ce(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xCE.toByte() // function
        sendByte[1] = (code.size).toByte() // len
//        Timber.d("ce: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    /**
     * ByteArray [D4] data command. Get the lock setting.
     *
     * @return An encoded byte array of [D4] command.
     * */
    fun d4(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD4.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }
    /**
     * ByteArray [D5] data command. Send the lock setting.
     *
     * @return An encoded byte array of [D5] command.
     * */
    fun d5(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        data: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD5.toByte() // function
        sendByte[1] = 21.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte + data))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }
    fun d6(
        serial: ByteArray,
        aesKeyTwo: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xD6.toByte() // function
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }
    fun d7(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        state: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(3)
        sendByte[0] = 0xD7.toByte() // function
        sendByte[1] = 0x01.toByte() // len
        sendByte[2] = state.first()
        return encrypt(aesKeyTwo, pad(serial + sendByte))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }
    fun resolveC0(keyOne: ByteArray, notification: ByteArray): ByteArray {
        return decrypt(keyOne, notification)?.let { decrypted ->
//            Timber.d("[C0] decrypted: ${decrypted.toHex()}")
            if (decrypted.component3().unSignedInt() == 0xC0) {
                return decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
            } else {
                throw IllegalArgumentException("Return function byte is not [C0]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveC1(aesKeyTwo: ByteArray, notification: ByteArray): ByteArray {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
//            Timber.d("[C1] decrypted: ${decrypted.toHex()}")
            if (decrypted.component3().unSignedInt() == 0xC1) {
                return decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
            } else {
                throw IllegalArgumentException("Return function byte is not [C1]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveC7(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xC7) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [C7]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    /**
     * Resolve [C8] token generated from device.
     *
     * @param notification Data return from device.
     * @return ByteArray represent token.
     *
     * */
    fun resolveC8(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xC8) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [C8]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveCE(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xCE) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [C8]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveD4(aesKeyTwo: ByteArray, notification: ByteArray): LockConfig {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xD4) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
//                        Timber.d("[D4] ${bytes.toHex()}")

                        val latIntPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(Config.LATITUDE_INTEGER.byte, Config.LATITUDE_DECIMAL.byte)).int)
//                        Timber.d("latIntPart: $latIntPart")
                        val latDecimalPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(Config.LATITUDE_DECIMAL.byte, Config.LONGITUDE_INTEGER.byte)).int)
                        val latDecimal = latDecimalPart.toBigDecimal().movePointLeft(9)
//                        Timber.d("latDecimalPart: $latIntPart, latDecimal: $latDecimal")
                        val lat = latIntPart.toBigDecimal().plus(latDecimal)
//                        Timber.d("lat: $lat, ${lat.toPlainString()}")

                        val lngIntPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(Config.LONGITUDE_INTEGER.byte, Config.LONGITUDE_DECIMAL.byte)).int)
//                        Timber.d("lngIntPart: $lngIntPart")
                        val lngDecimalPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(Config.LONGITUDE_DECIMAL.byte, Config.SIZE.byte)).int)
                        val lngDecimal = lngDecimalPart.toBigDecimal().movePointLeft(9)
//                        Timber.d("lngIntPart: $lngIntPart, lngDecimal: $lngDecimal")
                        val lng = lngIntPart.toBigDecimal().plus(lngDecimal)
//                        Timber.d("lng: $lng, ${lng.toPlainString()}")

                        val lockConfig = LockConfig(
                            orientation = when (bytes[Config.LOCK_ORIENTATION.byte].unSignedInt()) {
                                0xA0 -> LockOrientation.Right
                                0xA1 -> LockOrientation.Left
                                0xA2 -> LockOrientation.NotDetermined
                                else -> throw LockStatusException.LockOrientationException()
                            },
                            isSoundOn = bytes[Config.KEYPRESS_BEEP.byte].unSignedInt() == 0x01,
                            isVacationModeOn = bytes[Config.VACATION_MODE.byte].unSignedInt() == 0x01,
                            isAutoLock = bytes[Config.AUTOLOCK.byte].unSignedInt() == 0x01,
                            autoLockTime = bytes[Config.AUTOLOCK_DELAY.byte].unSignedInt(),
                            isPreamble = bytes[Config.PREAMBLE.byte].unSignedInt() == 0x01,
                            latitude = lat.toDouble(),
                            longitude = lng.toDouble()
                        )
//                        Timber.d("[D4] lockConfig: $lockConfig")
                        return lockConfig
                    }
                } else {
                    throw IllegalArgumentException("Return function byte is not [D4]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveD5(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xD5) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [D5]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    fun resolveD6(aesKeyTwo: ByteArray, notification: ByteArray): LockSetting {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
//                Timber.d("[D6] decrypted: ${decrypted.toHex()}")
                if (decrypted.component3().unSignedInt() == 0xD6) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                        val autoLockTime = if (bytes[D6Features.AUTOLOCK_DELAY.byte].unSignedInt() !in 1..90) {
                            1
                        } else {
                            bytes[D6Features.AUTOLOCK_DELAY.byte].unSignedInt()
                        }
//                        Timber.d("autoLockTime from lock: $autoLockTime")
                        val lockSetting = LockSetting(
                            config = LockConfig(
                                orientation = when (bytes[D6Features.LOCK_ORIENTATION.byte].unSignedInt()) {
                                    0xA0 -> LockOrientation.Right
                                    0xA1 -> LockOrientation.Left
                                    0xA2 -> LockOrientation.NotDetermined
                                    else -> throw LockStatusException.LockOrientationException()
                                },
                                isSoundOn = bytes[D6Features.KEYPRESS_BEEP.byte].unSignedInt() == 0x01,
                                isVacationModeOn = bytes[D6Features.VACATION_MODE.byte].unSignedInt() == 0x01,
                                isAutoLock = bytes[D6Features.AUTOLOCK.byte].unSignedInt() == 0x01,
                                autoLockTime = autoLockTime,
                                isPreamble = bytes[D6Features.PREAMBLE.byte].unSignedInt() == 0x01
                            ),
                            status = when (bytes[D6Features.LOCK_STATUS.byte].unSignedInt()) {
                                0 -> UNLOCKED
                                1 -> LOCKED
                                else -> UNKNOWN
                            },
                            battery = bytes[D6Features.BATTERY.byte].unSignedInt(),
                            batteryStatus = when (bytes[D6Features.LOW_BATTERY.byte].unSignedInt()) {
                                0 -> BATTERY_GOOD
                                1 -> BATTERY_LOW
                                else -> BATTERY_ALERT
                            },
                            timestamp = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(D6Features.TIMESTAMP.byte, D6Features.SIZE.byte)).int).toLong()
                        )
//                        Timber.d("[D6] LockSetting: $lockSetting, lockSetting: ${lockSetting.timestamp}")
                        return lockSetting
                    }
                } else {
                    throw IllegalArgumentException("Return function byte is not [D6]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    fun resolveE5(notification: ByteArray): ByteArray {
        return if (notification.component3().unSignedInt() == 0xE5) {
            notification.copyOfRange(4, 4 + notification.component4().unSignedInt())
        } else {
            throw IllegalArgumentException("Return function byte is not [E5]")
        }
    }

    fun extractToken(byteArray: ByteArray): DeviceToken {
        return if (byteArray.component1().unSignedInt() == 0) {
//            throw ConnectionTokenException.IllegalTokenException()
            throw Exception()
        } else {
            Log.d("TAG","[E5]: ${byteArray.toHex()}")
            val isPermanentToken = byteArray.component2().unSignedInt() == 1
            val isOwnerToken = byteArray.component3().unSignedInt() == 1
            val permission = String(byteArray.copyOfRange(3, 4))
            val token = byteArray.copyOfRange(4, 12)
            if (isPermanentToken) {

                val name = String(byteArray.copyOfRange(12, byteArray.size))
                //return below
                DeviceToken.PermanentToken(
                    Base64.encodeToString(token, Base64.DEFAULT),
                    isOwnerToken,
                    name,
                    permission
                )
            } else {
                DeviceToken.OneTimeToken(Base64.encodeToString(token, Base64.DEFAULT))
            }
        }
    }


    fun stringCodeToHex(code: String): ByteArray {
        return code.takeIf { it.isNotBlank() }
            ?.filter { it.isDigit() }
            ?.map { Character.getNumericValue(it).toByte() }
            ?.toByteArray()
            ?: throw IllegalArgumentException("Invalid user code string")
    }

    fun generateKeyTwoThen(
        randomNumberOne: ByteArray,
        randomNumberTwo: ByteArray,
        function: (ByteArray) -> Unit
    ) {
        val keyTwo = ByteArray(16)
        for (i in 0..15) keyTwo[i] =
            ((randomNumberOne[i].unSignedInt()) xor (randomNumberTwo[i].unSignedInt())).toByte()
        function.invoke(keyTwo)
    }

    fun generateKeyTwo(randomNumberOne: ByteArray, randomNumberTwo: ByteArray): ByteArray {
        val keyTwo = ByteArray(16)
        for (i in 0..15) keyTwo[i] =
            ((randomNumberOne[i].unSignedInt()) xor (randomNumberTwo[i].unSignedInt())).toByte()
        return keyTwo
    }

    fun determineTokenPermission(data: ByteArray): String {
        return String(data.copyOfRange(1, 2))
    }


    fun determineTokenState(data: ByteArray, isLockFromSharing: Boolean): Int {
        return when (data.component1().unSignedInt()) {
            //0 -> if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.IllegalTokenException()
            1 -> Log.d("TAG","VALID_TOKEN")
//                DeviceToken.VALID_TOKEN
            // according to documentation, 2 -> the token has been swapped inside the device,
            // hence the one time token no longer valid to connect.
            //2 -> if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.DeviceRefusedException()
            3 -> Log.d("TAG","ONE_TIME_TOKEN")
//                DeviceToken.ONE_TIME_TOKEN
            // 0, and else
            else -> Log.d("TAG","IllegalTokenStateException") //if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.IllegalTokenStateException()
        }
    }

    fun settingBytes(setting: LockConfig): ByteArray {
        val settingBytes = ByteArray(Config.SIZE.byte)
        settingBytes[Config.LOCK_ORIENTATION.byte] = 0xA3.toByte()
        settingBytes[Config.KEYPRESS_BEEP.byte] = (if (setting.isSoundOn) 0x01.toByte() else 0x00.toByte())
        settingBytes[Config.VACATION_MODE.byte] = if (setting.isVacationModeOn) 0x01.toByte() else 0x00.toByte()
        settingBytes[Config.AUTOLOCK.byte] = if (setting.isAutoLock) 0x01.toByte() else 0x00.toByte()
        settingBytes[Config.AUTOLOCK_DELAY.byte] = setting.autoLockTime.toByte()
        settingBytes[Config.PREAMBLE.byte] = (if (setting.isPreamble) 0x01.toByte() else 0x00.toByte())

        val latitudeBigDecimal = BigDecimal.valueOf(setting.latitude ?: 0.0)
        val latitudeIntPartBytes = intToLittleEndianBytes(latitudeBigDecimal.toInt())
        for (i in 0..latitudeIntPartBytes.lastIndex) settingBytes[Config.LATITUDE_INTEGER.byte + i] = latitudeIntPartBytes[i]
        val latitudeDecimalInt = latitudeBigDecimal.subtract(BigDecimal(latitudeBigDecimal.toInt())).scaleByPowerOfTen(9).toInt()
        val latitudeDecimalPartBytes = intToLittleEndianBytes(latitudeDecimalInt)
        for (i in 0..latitudeDecimalPartBytes.lastIndex) settingBytes[Config.LATITUDE_DECIMAL.byte + i] = latitudeDecimalPartBytes[i]
//        Timber.d("latitudeBigDecimal: $latitudeBigDecimal, latitudeIntPart: ${latitudeBigDecimal.toInt()}, latitudeDecimalInt: $latitudeDecimalInt")

        val longitudeBigDecimal = BigDecimal.valueOf(setting.longitude ?: 0.0)
        val longitudeIntPartBytes = intToLittleEndianBytes(longitudeBigDecimal.toInt())
        for (i in 0..longitudeIntPartBytes.lastIndex) settingBytes[Config.LONGITUDE_INTEGER.byte + i] = longitudeIntPartBytes[i]
        val longitudeDecimalInt = longitudeBigDecimal.subtract(BigDecimal(longitudeBigDecimal.toInt())).scaleByPowerOfTen(9).toInt()
        val longitudeDecimalPartBytes = intToLittleEndianBytes(longitudeDecimalInt)
        for (i in 0..longitudeDecimalPartBytes.lastIndex) settingBytes[Config.LONGITUDE_DECIMAL.byte + i] = longitudeDecimalPartBytes[i]
//        Timber.d("longitudeBigDecimal: $longitudeBigDecimal longitudeBigDecimal: ${longitudeBigDecimal.toInt()}, longitudeDecimalInt: $longitudeDecimalInt")

        return settingBytes
    }
    fun intToLittleEndianBytes(int: Int): ByteArray {
        val bytes = ByteArray(4)
        val byteBuffer = ByteBuffer.allocate(4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putInt(int)
        byteBuffer.flip()
        byteBuffer.get(bytes)
        return bytes
    }
}