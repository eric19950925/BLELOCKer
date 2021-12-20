package com.example.blelocker

import android.util.Base64
import android.util.Log
import androidx.lifecycle.*
import com.example.blelocker.Model.LockConnInfoRepository
import com.example.blelocker.entity.*
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class OneLockViewModel(private val repository: LockConnInfoRepository): ViewModel() {
    val mLockConnectionInfo = MutableLiveData<LockConnectionInformation>()
    val mLockBleStatus = MutableLiveData<Boolean>()
    val isRefreshing = MutableLiveData<Boolean>()
    val allLocks : LiveData<List<LockConnectionInformation>> = repository.allLockInfo.asLiveData()

    fun insertLock(lockConnectionInformation: LockConnectionInformation) = viewModelScope.launch {
        repository.LockInsert(lockConnectionInformation)
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
    fun stringCodeToHex(code: String): ByteArray {
        return code.takeIf { it.isNotBlank() }
            ?.filter { it.isDigit() }
            ?.map { Character.getNumericValue(it).toByte() }
            ?.toByteArray()
            ?: throw IllegalArgumentException("Invalid user code string")
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

    fun resolveD6(aesKeyTwo: ByteArray, notification: ByteArray): LockSetting {
        return aesKeyTwo.let { keyTwo ->
            decrypt(keyTwo, notification)?.let { decrypted ->
//                Timber.d("[D6] decrypted: ${decrypted.toHex()}")
                if (decrypted.component3().unSignedInt() == 0xD6) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                        val autoLockTime = if (bytes[4].unSignedInt() !in 1..90) {
                            1
                        } else {
                            bytes[4].unSignedInt()
                        }
//                        Timber.d("autoLockTime from lock: $autoLockTime")
                        val lockSetting = LockSetting(
                            config = LockConfig(
//                                orientation = when (bytes[0].unSignedInt()) {
//                                    0xA0 -> LockOrientation.Right
//                                    0xA1 -> LockOrientation.Left
//                                    0xA2 -> LockOrientation.NotDetermined
//                                    else -> throw LockStatusException.LockOrientationException()
//                                },
                                orientation = "",
                                isSoundOn = bytes[1].unSignedInt() == 0x01,
                                isVacationModeOn = bytes[2].unSignedInt() == 0x01,
                                isAutoLock = bytes[3].unSignedInt() == 0x01,
                                autoLockTime = autoLockTime
                            ),
                            status = when (bytes[5].unSignedInt()) {
                                0 -> LockStatus.UNLOCKED
                                1 -> LockStatus.LOCKED
                                else -> LockStatus.UNKNOWN
                            },
                            battery = bytes[6].unSignedInt(),
                            batteryStatus = when (bytes[7].unSignedInt()) {
                                0 -> LockStatus.BATTERY_GOOD
                                1 -> LockStatus.BATTERY_LOW
                                else -> LockStatus.BATTERY_ALERT
                            },
                            timestamp = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(8, 12)).int).toLong()
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
    fun decryptQRcode(scanString: String, function: () -> Unit) {
        val base64Decoded = Base64.decode(scanString, Base64.DEFAULT)
        val decrypted = decrypt(
            MainActivity.BARCODE_KEY.toByteArray(),
            pad(base64Decoded, true)
        )
        decrypted?.let { result ->
            val data = String(result).replace(Regex("\\P{Print}"), "")
//            Timber.d("decrypted qr code: $data")
//            showLog("\ndecrypted qr code: $data")
            mLockConnectionInfo.value = LockConnectionInfo(data)
            function.invoke()
            Log.d("TAG",mLockConnectionInfo.value.toString())
        } ?: throw IllegalArgumentException("Decrypted string is null")
    }

    private val parser = JsonParser()
    fun LockConnectionInfo(jsonString: String): LockConnectionInformation {
//        if (parser.parse(jsonString).isJsonObject) {
        val root = parser.parse(jsonString)
        val oneTimeToken = root.asJsonObject?.get("T")?.asString
            ?: throw IllegalArgumentException("Invalid Token")
        val keyOne = root.asJsonObject?.get("K")?.asString
            ?: throw IllegalArgumentException("Invalid AES_Key")
        val macAddress =
            root.asJsonObject?.get("A")?.asString?.chunked(2)?.joinToString(":") { it }
                ?: throw IllegalArgumentException("Invalid MAC_Address")
        val isOwnerToken = root.asJsonObject?.has("F") == false
        val isFrom =
            if (!isOwnerToken) root.asJsonObject?.get("F")?.asString ?: "" else ""
        val lockName = root.asJsonObject?.get("L")?.asString ?: "New_Lock"
        return LockConnectionInformation(
            macAddress = macAddress,
            displayName = lockName,
            keyOne = Base64.encodeToString(
                hexToBytes(keyOne),
                Base64.DEFAULT
            ),
            keyTwo = "",
            oneTimeToken = Base64.encodeToString(
                hexToBytes(oneTimeToken),
                Base64.DEFAULT
            ),
            permanentToken = "",
            isOwnerToken = isOwnerToken,
            tokenName = "T",
            sharedFrom = isFrom,
            index = 0
        )


//        } else {
////            Toast.makeText(requireContext(), "getString(R.string.global_please_try_again)", Toast.LENGTH_SHORT).show()
//            throw IllegalArgumentException("Invalid QR Code")
//        }
    }
    fun decrypt(key: ByteArray, data: ByteArray): ByteArray? {
        return try {
            val cipher: Cipher = Cipher.getInstance(MainActivity.CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val original: ByteArray = cipher.doFinal(data)
//            showLog("\ndecrypted: \n${original.toHex()}")
            original
        } catch (exception: Exception) {
//            showLog(exception.toString())
            null
        }
    }
    //加密,the size of key should be 16 bytes
    fun encrypt(key: ByteArray, data: ByteArray): ByteArray? {
        return try {
            val cipher: Cipher = Cipher.getInstance(MainActivity.CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted: ByteArray = cipher.doFinal(data)
            Log.d("TAG","encrypted:\n${encrypted.toHex()}")
            encrypted
        } catch (exception: Exception) {
//            Timber.d(exception)
            Log.d("TAG",exception.toString())
//            java.security.InvalidKeyException: Unsupported key size: 13 bytes
            null
        }
    }
    fun pad(data: ByteArray, padZero: Boolean = false): ByteArray {
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
    private val commandSerial = AtomicInteger()

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
//            0xC8 -> c8(serialIncrementAndGet(), key, data)
//            0xCC -> cc(serialIncrementAndGet(), key)
            0xCE -> ce(serialIncrementAndGet(), key, data)
//            0xD0 -> d0(serialIncrementAndGet(), key)
//            0xD1 -> d1(serialIncrementAndGet(), key, data)
//            0xD2 -> d2(serialIncrementAndGet(), key)
//            0xD3 -> d3(serialIncrementAndGet(), key, data)
//            0xD4 -> d4(serialIncrementAndGet(), key)
//            0xD5 -> d5(serialIncrementAndGet(), key, data)
            0xD6 -> d6(serialIncrementAndGet(), key)
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
        sendByte[0] = 0xC0.toByte() // function name
        sendByte[1] = 0x10 // len = 16
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

}
fun Byte.unSignedInt() = this.toInt() and 0xFF