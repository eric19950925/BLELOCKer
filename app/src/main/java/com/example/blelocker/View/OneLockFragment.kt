package com.example.blelocker.View

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import com.example.blelocker.*
import com.example.blelocker.BroadcastReceiver.BleScanBroadcastReceiver
import com.example.blelocker.MainActivity.Companion.DATA
import com.example.blelocker.MainActivity.Companion.MY_LOCK_QRCODE
import com.example.blelocker.entity.DeviceToken
import com.example.blelocker.entity.LockConnectionInformation
import com.example.blelocker.entity.LockStatus.LOCKED
import com.example.blelocker.entity.LockStatus.UNLOCKED
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.fragment_onelock.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class OneLockFragment: BaseFragment() {
    val oneLockViewModel by activityViewModels<OneLockViewModel>()
    private lateinit var mSharedPreferences: SharedPreferences

    private var mHandler: Handler? = null
    var mPermanentToken: String?=null

    override fun getLayoutRes(): Int = R.layout.fragment_onelock

    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothDevice: BluetoothDevice? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null

    var randomNumberOne : ByteArray? = null
    var keyTwo : ByteArray? = null
    var isLockFromSharing: Boolean?=null
    var mQRcode: String?=null
    private val rotateAnimation = RotateAnimation(
        0f, 359f,
        Animation.RELATIVE_TO_SELF, 0.5f,
        Animation.RELATIVE_TO_SELF, 0.5f
    ).apply {
        this.duration = 2000
        this.repeatCount = ValueAnimator.INFINITE
    }

    override fun onViewHasCreated() {

        readSharedPreference()

        tv_add_lock.setPaintFlags(tv_add_lock.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG)

        tv_add_lock.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_onelock_to_scan)
        }

        iv_add_btn.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_onelock_to_scan)
        }

        log_tv.movementMethod = ScrollingMovementMethod.getInstance()

        oneLockViewModel.mLockConnectionInfo.observe(this){
            if(it == null)return@observe
            tv_add_lock.visibility = View.GONE
            tv_my_lock_mac.setText(it.macAddress)
            tv_my_lock_tk.setText(mPermanentToken)
        }

        oneLockViewModel.mLockBleStatus.observe(this){
            Log.d("TAG",it.toString())
            if(it != true){
                iv_my_lock_ble_status.visibility = View.VISIBLE
                btn_lock.clearAnimation()
                btn_lock.visibility = View.GONE
            }else{
                iv_my_lock_ble_status.visibility = View.GONE
                btn_lock.visibility = View.VISIBLE
                btn_lock.setBackgroundResource(R.drawable.ic_loading_main)
                btn_lock.startAnimation(rotateAnimation)
            }
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        iv_my_lock_ble_status.setOnClickListener {
            if(checkPermissions()!=true)return@setOnClickListener
            if(checkBTenable()!=true)return@setOnClickListener
            bleScan()
        }
        btn_lock.setOnClickListener {
            sendD7()
        }























    }
    private fun bleScan() {
        mBluetoothManager = requireContext().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothLeScanner = (mBluetoothManager?:return).adapter.bluetoothLeScanner

        mBluetoothLeScanner?.startScan(mScanCallback) // 開始搜尋
        oneLockViewModel.mLockBleStatus.value = true
        mHandler = Handler()
        mHandler?.postDelayed({
            mBluetoothLeScanner?.stopScan(mScanCallback)
            invalidateOptionsMenu(requireActivity())
//            oneLockViewModel.mLockBleStatus.value = false
        }, 5000)


    }
    private val mScanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result?.device?.address == oneLockViewModel.mLockConnectionInfo.value?.macAddress) {

                showLog("Find device: ${result?.device?.name}")

                mBluetoothGatt = result?.device?.connectGatt(requireActivity(), false, mBluetoothGattCallback)

                pauseScan()
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

    }

    private fun pauseScan() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner?.stopScan(mScanCallback)
        }
    }

    override fun onPause() {
        closeBLEGatt()
        super.onPause()
    }

    override fun onStop() {
        closeBLEGatt()
        super.onStop()
    }

    override fun onDestroyView() {
        closeBLEGatt()
        super.onDestroyView()
    }

    override fun onDestroy() {
        closeBLEGatt()
        super.onDestroy()
    }

    override fun onDetach() {
        closeBLEGatt()
        super.onDetach()
    }

    private fun closeBLEGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt?.disconnect()
            mBluetoothGatt?.close()
            mBluetoothAdapter?.cancelDiscovery()
            mBluetoothLeScanner?.stopScan(mScanCallback)
//            mBluetoothGatt = null
        }
    }

    private fun checkBTenable(): Boolean {
        mBluetoothManager = requireContext().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothManager!!.adapter?.takeIf { !it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 124)
        }
        return mBluetoothManager!!.adapter?.isEnabled?:false
    }

    private fun checkPermissions() : Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1)
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH_SCAN
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

    }

    val mBluetoothGattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when(status){
                0 -> when(newState){
                    2 -> requireActivity().runOnUiThread {showLog("GATT連線成功")}
                    else -> requireActivity().runOnUiThread {showLog("GATT連線中斷")}
                }
                else -> {
                    oneLockViewModel.mLockBleStatus.value = false
                    requireActivity().runOnUiThread {showLog("GATT連線出錯: ${status}")}
                }
            }
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            when(status){
                BluetoothGatt.GATT_SUCCESS -> {
//                    this@MainActivity.runOnUiThread {showLog("Service discovery Success.")}
//                    for (service in gatt?.services?:return) {
//                        val allUUIDs = StringBuilder(
//                            """
//              UUIDs={
//              S=${service.uuid}
//              """.trimIndent()
//                        )
//                        for (characteristic in service.characteristics) {
//                            allUUIDs.append(",\nC=").append(characteristic.uuid)
//                            for (descriptor in characteristic.descriptors) allUUIDs.append(",\nD=")
//                                .append(descriptor.uuid)
//                        }
//                        allUUIDs.append("}")
//                        this@MainActivity.runOnUiThread {showLog("onServicesDiscovered:$allUUIDs")}
//                    }
                    setup()
                }
                BluetoothGatt.GATT_FAILURE -> requireActivity().runOnUiThread {/*showLog("Service discovery Failure.")*/}
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
//            this@MainActivity.runOnUiThread {showLog("\nwrited: ${characteristic?.value}")}
//            val keyOne = Base64.decode(mLockConnectionInfo?.keyOne, Base64.DEFAULT)
//            randomNumberOne = resolveC0(keyOne,characteristic?.value?:return)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val keyOne = Base64.decode(oneLockViewModel.mLockConnectionInfo.value?.keyOne, Base64.DEFAULT)
            val decrypted = oneLockViewModel.decrypt(
                keyOne,
                characteristic?.value?:return
            )
            when(decrypted?.component3()?.unSignedInt()){
                0xC0 -> {
                    Log.d("TAG","C0 notyfy ramNum2")
                    generateKeyTwo(randomNumberOne?:return,oneLockViewModel.resolveC0(keyOne,characteristic.value)){
                        keyTwo = it
//                        showLog("C0 notyfy ramNum2\nApp use it to generateKeyTwo = $keyTwo")
                        if((mPermanentToken?:return@generateKeyTwo).isBlank())sendC1withOTToken(it)
                        else sendC1(it)
                    }

                }
            }
            val decrypted2 = oneLockViewModel.decrypt(
                keyTwo?:return,
                characteristic.value
            )
            when(decrypted2?.component3()?.unSignedInt()){
                0xC1 -> {
                    val dataFromDevice = oneLockViewModel.resolveC1(keyTwo?:return, characteristic.value)
                    if(dataFromDevice.toHex().length > 10){
                        Log.d("TAG","one time token : ${dataFromDevice.toHex()}")
                    }
                    else {
//                    val deviceToken = determineTokenState(tokenStateFromDevice, isLockFromSharing?:return)
                        val deviceToken = oneLockViewModel.determineTokenState(dataFromDevice, false)
                        val permission = oneLockViewModel.determineTokenPermission(dataFromDevice)
                        Log.d("TAG", "C1 notyfy token state : ${dataFromDevice.toHex()}")
                        Log.d("TAG", "token permission: $permission")
                    }
                }
                0xC7 -> {
                    val dataFromDevice = oneLockViewModel.resolveC7(keyTwo?:return, characteristic.value)
                    if(dataFromDevice == true){
                        Log.d("TAG","admin pincode had been set.")
                    }
                    else {
                        Log.d("TAG", "admin pincode had not been set.")
                    }
                }

                0xD6 -> {
                    val mLockSetting = oneLockViewModel.resolveD6(keyTwo?:return, characteristic.value)
                    val islocked = if(mLockSetting.status==0)"locked" else "unlock"
                    requireActivity().runOnUiThread {
                        showLog("D6 notyfy Lock's setting: ${mLockSetting}")
                        btn_lock.clearAnimation()
                        when (mLockSetting.status) {
                            LOCKED -> {
                                btn_lock.setBackgroundResource(R.drawable.ic_lock_main)
                            }
                            UNLOCKED -> {
                                btn_lock.setBackgroundResource(R.drawable.ic_auto_unlock)
                            }
                        }
                    }
                }
                0xE5 -> {
                    oneLockViewModel.decrypt(keyTwo?:return, characteristic.value)?.let { bytes ->
                        val permanentToken = oneLockViewModel.extractToken(oneLockViewModel.resolveE5(bytes))
                        mPermanentToken = (permanentToken as DeviceToken.PermanentToken).token
                    }
                    saveData()
                }
                0xEF -> {
//                        decrypt(keyTwo?:return, characteristic.value)?.let { bytes ->
//                            val permanentToken = extractToken(resolveE5(bytes))
//                            mPermanentToken = (permanentToken as DeviceToken.PermanentToken).token
//                        }
                    requireActivity().runOnUiThread {showLog("EF")}

                }
            }

        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            requireActivity().runOnUiThread {
                showLog(
                    "\nonDescriptorWrite \n" +
                            "--->in Characteristic\n" +
                            "--->[${descriptor?.uuid}]\n" +
                            "--->:${descriptor?.value}"
                )
            }
            sendC0()
        }
    }
    private fun setup() {
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        mBluetoothGatt?.setCharacteristicNotification(notify_characteristic,true)

        val descriptor = notify_characteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        mBluetoothGatt?.writeDescriptor(descriptor)
    }
    private fun sendC0() {
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        val keyOne = Base64.decode(oneLockViewModel.mLockConnectionInfo.value?.keyOne, Base64.DEFAULT)
//        val permanentToken = Base64.decode(mLockConnectionInfo?.permanentToken, Base64.DEFAULT)
        (notify_characteristic?:return).value = oneLockViewModel.createCommand(0xC0, keyOne)
//        showLog("\napp writeC0: ${notify_characteristic.value}")
        Log.d("TAG","\napp writeC0: ${notify_characteristic.value}")
        randomNumberOne = oneLockViewModel.resolveC0(keyOne,notify_characteristic.value)
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    private fun sendC1(keyTwo: ByteArray) {
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        val permanentToken = Base64.decode(mPermanentToken, Base64.DEFAULT)
        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = oneLockViewModel.createCommand(0xC1, keyTwo,permanentToken)
        Log.d("TAG","\napp writeC1: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    private fun sendC1withOTToken(keyTwo: ByteArray) {
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        val keyOne = Base64.decode(oneLockViewModel.mLockConnectionInfo.value?.keyOne, Base64.DEFAULT)
        val permanentToken = Base64.decode(oneLockViewModel.mLockConnectionInfo.value?.oneTimeToken, Base64.DEFAULT)
        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = oneLockViewModel.createCommand(0xC1, keyTwo, permanentToken)
//        showLog("\napp writeC1: ${notify_characteristic?.value}")
        Log.d("TAG","\napp writeC1_OT: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    private fun sendC7() {
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = oneLockViewModel.createCommand(0xC7, keyTwo?:return, oneLockViewModel.stringCodeToHex("0000"))
//        showLog("\napp writeC1: ${notify_characteristic?.value}")
        Log.d("TAG","\napp writeC7: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    private fun sendD6() {
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        notify_characteristic?.value = oneLockViewModel.createCommand(0xD6, keyTwo?:return)
//        showLog("\napp writeD6: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    private fun sendD7() {
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        notify_characteristic?.value = oneLockViewModel.createCommand(0xD7, keyTwo?:return, byteArrayOf(0x01))
//        showLog("\napp writeD7: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    private fun generateKeyTwo(
        randomNumberOne: ByteArray,
        randomNumberTwo: ByteArray,
        function: (ByteArray) -> Unit
    ) {
        val keyTwo = ByteArray(16)
        for (i in 0..15) keyTwo[i] =
            ((randomNumberOne[i].unSignedInt()) xor (randomNumberTwo[i].unSignedInt())).toByte()
        function.invoke(keyTwo)
    }

    private fun readSharedPreference() {
        mSharedPreferences = requireActivity().getSharedPreferences(DATA, 0)
        val mQRcode = mSharedPreferences.getString(MY_LOCK_QRCODE, "")
        mPermanentToken = mSharedPreferences.getString(MainActivity.MY_LOCK_TOKEN, "")
        if(mQRcode.isNullOrBlank())return
        oneLockViewModel.decryptQRcode(mQRcode){
            tv_my_lock_mac.setText(oneLockViewModel.mLockConnectionInfo.value?.macAddress)
            tv_my_lock_tk.setText(mSharedPreferences.getString(MainActivity.MY_LOCK_TOKEN, ""))
        }
    }


    private fun showLog(logText: String) {
        try {
            var log = log_tv.text.toString()

            log = log +"${logText}\n"

            log_tv.text = log
        } catch (e: IOException) {
        }
    }
    private fun cleanLog() {
        log_tv.text = ""
    }
    private fun saveData() {
        mSharedPreferences = requireActivity().getSharedPreferences(DATA, 0)
        mSharedPreferences.edit()
            .putString(MY_LOCK_QRCODE, mQRcode)
            .putString(MainActivity.MY_LOCK_TOKEN, mPermanentToken)
            .commit()
        println("store k2: ${keyTwo?.toHex()}")
    }

}