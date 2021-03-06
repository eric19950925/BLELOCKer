package com.sunionrd.blelocker.View.AddLock

import android.content.Intent
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.sunionrd.blelocker.*
import com.sunionrd.blelocker.Entity.LockConnectionInformation
import com.sunionrd.blelocker.databinding.FragmentScanBinding
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import com.google.gson.JsonParser
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.camera.CameraSettings
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ScanFragment: BaseFragment() {
    override fun getLayoutRes(): Int? = null
    private lateinit var currentBinding: FragmentScanBinding
    val oneLockViewModel by sharedViewModel<OneLockViewModel>()
    private lateinit var mQrResultLauncher : ActivityResultLauncher<Intent>
    var mQRcode: String?=null
    private val parser = JsonParser()
    var scanDisposable: Disposable? = null

    var scanResultDisposable: Disposable? = null
    val scanResultSubject = PublishSubject.create<String>()

    override fun onResume() {
        super.onResume()
        val cameraSettings = CameraSettings()
        cameraSettings.requestedCameraId = 0
        currentBinding.scanner.barcodeView.cameraSettings = cameraSettings
        currentBinding.scanner.resume()
        currentBinding.scanner.setStatusText("")
        currentBinding.scanner.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.text?.let { scanString ->
                    try {
//                        Timber.d("Scan result: $scanString")
                        scanResultSubject.onNext(scanString)
                    } catch (error: Throwable) {
//                        Timber.d(error)
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
            }
        })
    }

    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        currentBinding = FragmentScanBinding.inflate(inflater, container, false)
        return currentBinding
    }

    override fun onViewHasCreated() {
        scanResultDisposable?.isDisposed
        scanResultDisposable = scanResultSubject
            .map { scanString ->
                mQRcode = scanString
                decryptQRcode(scanString) {
                    Navigation.findNavController(requireView()).navigate(R.id.action_scanFragment_to_installationFragment)
                }
            }.subscribe()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        scanResultDisposable?.dispose()
//        scanner.pause()
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

            //check if had bind todo

            requireActivity().runOnUiThread {
                //store data
                oneLockViewModel.mLockConnectionInfo.value = LockConnectionInfo(data)

            }

            oneLockViewModel.insertNewLock(LockConnectionInfo(data))
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
            Log.d("TAG","root= "+root.toString())
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
}