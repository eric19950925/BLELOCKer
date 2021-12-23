package com.example.blelocker.View.AddLock

import android.app.Activity
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.Navigation
import com.example.blelocker.*
import com.example.blelocker.entity.LockConnectionInformation
import com.google.zxing.integration.android.IntentIntegrator
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import com.google.gson.JsonParser
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.os.Bundle

class ScanFragment: BaseFragment() {
    override fun getLayoutRes(): Int = R.layout.fragment_scan
    val oneLockViewModel by viewModel<OneLockViewModel>()
    private lateinit var mQrResultLauncher : ActivityResultLauncher<Intent>
    var mQRcode: String?=null
    private val parser = JsonParser()
    override fun onViewHasCreated() {

        mQrResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK) {
                val result = IntentIntegrator.parseActivityResult(it.resultCode, it.data)

                if(result.contents != null) {
                    Log.d("TAG",result.contents)
//                    cleanLog()
                    mQRcode = result.contents
                    //處理QRcode
                    decryptQRcode(result.contents) {
                        val bundle = Bundle()
                        bundle.putString("MAC_ADDRESS", it)
                        Navigation.findNavController(requireView()).navigate(R.id.action_back_to_alllocks,bundle)
                    }

                }
            }else Navigation.findNavController(requireView()).navigate(R.id.action_back_to_alllocks)
        }
        startScanner()
    }

    override fun onBackPressed() {
        Log.d("TAG","onBackPressed")
    }

    private fun decryptQRcode(scanString: String, function: (mac:String) -> Unit) {
        val base64Decoded = Base64.decode(scanString, Base64.DEFAULT)
        val decrypted = decrypt(
            MainActivity.BARCODE_KEY.toByteArray(),
            pad(base64Decoded, true)
        )
        decrypted?.let { result ->
            val data = String(result).replace(Regex("\\P{Print}"), "")
            requireActivity().runOnUiThread {
                //store data
                oneLockViewModel.mLockConnectionInfo.value = LockConnectionInfo(data)

            }

            oneLockViewModel.insertLock(LockConnectionInfo(data))
            function.invoke(LockConnectionInfo(data).macAddress)
            Log.d("TAG",oneLockViewModel.mLockConnectionInfo.value.toString())
        } ?: throw IllegalArgumentException("Decrypted string is null")
    }
    fun decrypt(key: ByteArray, data: ByteArray): ByteArray? {
        return try {
            val cipher: Cipher = Cipher.getInstance(MainActivity.CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val original: ByteArray = cipher.doFinal(data)
            original
        } catch (exception: Exception) {

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

    //QRcode data
    fun LockConnectionInfo(jsonString: String): LockConnectionInformation {
        if (parser.parse(jsonString).isJsonObject) {
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
        } else {
            throw IllegalArgumentException("Invalid QR Code")
        }
    }
    // Start the QR Scanner
    private fun startScanner() {
        val scanner = IntentIntegrator(requireActivity())
        // QR Code Format
        scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        // Set Text Prompt at Bottom of QR code Scanner Activity
        scanner.setPrompt("QR Code Scanner Prompt Text")
        // Start Scanner (don't use initiateScan() unless if you want to use OnActivityResult)
        mQrResultLauncher.launch(scanner.createScanIntent())
    }
}