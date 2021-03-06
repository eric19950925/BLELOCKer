package com.sunionrd.blelocker

import android.util.Base64
import android.util.Log
import androidx.lifecycle.*
import com.sunionrd.blelocker.Model.LockConnInfoRepository
import com.sunionrd.blelocker.Entity.*
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class OneLockViewModel(private val repository: LockConnInfoRepository): ViewModel() {
    val mLockConnectionInfo = MutableLiveData<LockConnectionInformation>()
    val isRefreshing = MutableLiveData<Boolean>()
    val isPowerOn = MutableLiveData<Boolean>()
    val allLocks: LiveData<List<LockConnectionInformation>> = repository.allLockInfo.asLiveData()

    fun insertLock(lockConnectionInformation: LockConnectionInformation) = viewModelScope.launch {
        if (repository.getLockConnectInformation(lockConnectionInformation.macAddress).macAddress.isNotBlank()) return@launch
        repository.LockInsert(lockConnectionInformation)
    }

    fun insertNewLock(lockConnectionInformation: LockConnectionInformation) = viewModelScope.launch {
        repository.LockInsert(lockConnectionInformation)
    }

    fun deleteLocks() = viewModelScope.launch {
        repository.deleteAllLocks()
    }

    fun deleteOneLock(lockConnectionInformation: LockConnectionInformation) = viewModelScope.launch {
        repository.deleteOneLock(lockConnectionInformation)
    }

    fun getLockInfo(macAddress: String, function: () -> Unit) = viewModelScope.launch {
        mLockConnectionInfo.value =
            repository.getLockConnectInformation(macAddress)//pass by allLocks page
        function.invoke()
    }

    fun updateLockConnectInformation(lockConnectionInformation: LockConnectionInformation) =
        viewModelScope.launch {
            repository.LockInsert(lockConnectionInformation)
        }

    fun updateLockPermanentToken(token: DeviceToken.PermanentToken) = viewModelScope.launch {
        //update lockInfo with PermanentToken
        val newLockInfo = (mLockConnectionInfo.value ?: return@launch).let {
            LockConnectionInformation(
                macAddress = it.macAddress,
                displayName = it.displayName,
                keyOne = it.keyOne,
                keyTwo = it.keyTwo,
                oneTimeToken = it.oneTimeToken,
                permanentToken = token.token,
                isOwnerToken = token.isOwner,
                tokenName = token.name,
                permission = token.permission,
                sharedFrom = it.sharedFrom,
                index = 0,
                adminCode = it.adminCode//?????????????????????
            )
        }
        repository.LockInsert(newLockInfo)
        mLockConnectionInfo.value = newLockInfo
    }

    fun updateLockAdminCode(code: String) = viewModelScope.launch {
        val newLockInfo = (mLockConnectionInfo.value ?: return@launch).let {
            LockConnectionInformation(
                macAddress = it.macAddress,
                displayName = it.displayName,
                keyOne = it.keyOne,
                keyTwo = it.keyTwo,
                oneTimeToken = it.oneTimeToken,
                permanentToken = it.permanentToken,
                isOwnerToken = it.isOwnerToken,
                tokenName = "T",
                sharedFrom = it.sharedFrom,
                index = 0,
                adminCode = code
            )
        }
        repository.LockInsert(newLockInfo)
        mLockConnectionInfo.value = newLockInfo
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
            Log.d("TAG", mLockConnectionInfo.value.toString())
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

}
fun Byte.unSignedInt() = this.toInt() and 0xFF