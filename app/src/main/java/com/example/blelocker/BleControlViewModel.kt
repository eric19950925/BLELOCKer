package com.example.blelocker

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blelocker.entity.DeviceToken
import com.example.blelocker.entity.LockSetting
import kotlinx.android.synthetic.main.fragment_onelock.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class BleControlViewModel(
    @SuppressLint("StaticFieldLeak") val context: Context,
    private val mBleCmdUtils: BleCmdUtils
    ): ViewModel() {
    private var mBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var mBluetoothLeScanner = mBluetoothManager.adapter.bluetoothLeScanner
    private var mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mBluetoothGatt: BluetoothGatt? = null

    private var bleGattScope: Job?=null
    private var mKeyOne: ByteArray? = null
    private var mKeyTwo: ByteArray? = null
    private var randomNumberOne : ByteArray? = null
    private var mBleDeviceMacAddress: String? = null
    private var sunion_service: BluetoothGattService? = null
    private var notify_characteristic: BluetoothGattCharacteristic? = null

    var mCharacteristicValue = MutableLiveData<String>()
    var mDescriptorValue = MutableLiveData<Boolean>()
    var mLockSetting = MutableLiveData<LockSetting>()
    var mDeviceToken = MutableLiveData<DeviceToken>()
    var mGattStatus = MutableLiveData<Boolean>()
    var mLogText = MutableLiveData<String>()


    fun bleScan(macAddress: String){
        mBluetoothLeScanner?.startScan(mScanCallback) // 開始搜尋
        mBleDeviceMacAddress = macAddress
    }
    private val mScanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result?.device?.address == mBleDeviceMacAddress) {

                updateLogText("Find device: ${result?.device?.name}")

                mBluetoothGatt = result?.device?.connectGatt(context, false, mBluetoothGattCallback)
                bleGattScope = viewModelScope.launch(Dispatchers.Main){
                    try{
                        while(true){
                            delay(10000)
                            Log.d("TAG","bleGatt connecting...")
                        }
                    }finally {
                        Log.d("TAG","Stop bleGatt connection.")
                        closeBLEGatt()
                    }
                }
                pauseScan()
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

    }

    fun pauseScan() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner?.stopScan(mScanCallback)
        }
    }

    val mBluetoothGattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when(status){
                0 -> when(newState){
                    2 -> updateLogText("GATT連線成功")
                    else -> updateLogText("GATT連線中斷")
                }
                else -> {
//                    requireActivity().runOnUiThread {
//                    oneLockViewModel.mLockBleStatus.value = false
                    pauseScan()
                    //133通常需要重啟手機藍芽
                    bleGattScope?.cancel()
                    updateLogText("GATT連線出錯: ${status}")
//                    }
                }
            }
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
                viewModelScope.launch {
                    delay(10000)
                    //gatt探索逾時
                    if(mGattStatus.value == null){
//                            oneLockViewModel.mLockBleStatus.value = false
                        closeBLEGatt()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            when(status){
                BluetoothGatt.GATT_SUCCESS -> {
                    viewModelScope.launch {
                        mGattStatus.value = true
                    }
                    sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
                    notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
                    mBluetoothGatt?.setCharacteristicNotification(notify_characteristic,true)
                    val descriptor = notify_characteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    mBluetoothGatt?.writeDescriptor(descriptor)
                }
                BluetoothGatt.GATT_FAILURE -> updateLogText("Service discovery Failure.")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
//            updateLogText("\nwrited: ${characteristic?.value}")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            updateLogText("\nSetup Notification")
            viewModelScope.launch { mDescriptorValue.value = true }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val decrypted = mBleCmdUtils.decrypt(mKeyOne?:return, characteristic?.value?:return)
            when(decrypted?.component3()?.unSignedInt()){
                0xC0 -> {
//                    Log.d("TAG","C0 notify ramNum2")
                    mBleCmdUtils.generateKeyTwo(randomNumberOne?:return, mBleCmdUtils.resolveC0(mKeyOne?:return, characteristic.value)){
                        mKeyTwo = it
                        viewModelScope.launch { mCharacteristicValue.value = "C0" }
                    }

                }
                else -> {
                    Log.d("TAG","else")
                }
            }
            val decrypted2 = mBleCmdUtils.decrypt(mKeyTwo?:return, characteristic.value)
            when(decrypted2?.component3()?.unSignedInt()){
                0xC1 -> {
                    val dataFromDevice = mBleCmdUtils.resolveC1(mKeyTwo?:return, characteristic.value)
                    if(dataFromDevice.toHex().length > 10){
                        Log.d("TAG","one time token : ${dataFromDevice.toHex()}")
                    }
                    else {
//                    val deviceToken = determineTokenState(tokenStateFromDevice, isLockFromSharing?:return)
                        val deviceToken = mBleCmdUtils.determineTokenState(dataFromDevice, false)
                        val permission = mBleCmdUtils.determineTokenPermission(dataFromDevice)
                        Log.d("TAG", "C1 notify token state : ${dataFromDevice.toHex()}")
                        Log.d("TAG", "token permission: $permission")
                    }
                }
                0xC7 -> {
                    val dataFromDevice = mBleCmdUtils.resolveC7(mKeyTwo?:return, characteristic.value)
                    if(dataFromDevice == true){
                        viewModelScope.launch { mCharacteristicValue.value = "C7" }
                        Log.d("TAG","admin pincode had been set.")
                        updateLogText("\nadmin pincode had been set.")
                    }
                    else {
                        Log.d("TAG", "admin pincode had not been set.")
                    }
                }
                0xCE -> {
                    val dataFromDevice = mBleCmdUtils.resolveCE(mKeyTwo?:return, characteristic.value)
//                    if(dataFromDevice){
//                        requireActivity().runOnUiThread {
//                            //clean data
//                            oneLockViewModel.mLockConnectionInfo.value = null
//                            //close gatt
//                            closeBLEGatt()
//                            //set ble scan btn not clickable
//                            iv_my_lock_ble_status.isClickable = false
//                            showLog("CE notify 重置成功")
//                        }
//                    }else {
//                        requireActivity().runOnUiThread {showLog("CE notify 重置失敗")}
//                    }
                }

                0xD6 -> {
                    viewModelScope.launch {
                        mLockSetting.value = mBleCmdUtils.resolveD6(mKeyTwo?:return@launch, characteristic.value)
                    }
                    updateLogText("\nD6 notify Lock's setting: ${mLockSetting}")

                }
                0xE5 -> {
                    mBleCmdUtils.decrypt(mKeyTwo?:return, characteristic.value)?.let { bytes ->
                        mDeviceToken.value = mBleCmdUtils.extractToken(mBleCmdUtils.resolveE5(bytes))
                        sendC7()//有了token就設admin code
                    }

                }
                0xEF -> {

                    updateLogText("\nEF")

                }
            }

        }

    }

    fun sendC0(init_keyOne: String) {
        mKeyOne = Base64.decode(init_keyOne, Base64.DEFAULT)
        viewModelScope.launch {
            (notify_characteristic?:return@launch).value = mBleCmdUtils.createCommand(0xC0, mKeyOne?:return@launch)
            updateLogText("\napp writeC0: ${notify_characteristic?.value}")
            randomNumberOne = mBleCmdUtils.resolveC0(mKeyOne?:return@launch,notify_characteristic?.value?:return@launch)
            mBluetoothGatt?.writeCharacteristic(notify_characteristic)
        }
    }
    fun sendC1(init_permanentToken: String) {
        val permanentToken = Base64.decode(init_permanentToken, Base64.DEFAULT)
//        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = mBleCmdUtils.createCommand(0xC1, mKeyTwo?:return, permanentToken)
        updateLogText("\napp writeC1: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    fun sendC1withOTToken(init_oneTimeToken: String) {
        val permanentToken = Base64.decode(init_oneTimeToken, Base64.DEFAULT)
//        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = mBleCmdUtils.createCommand(0xC1, mKeyTwo?:return, permanentToken)
        updateLogText("\napp writeC1_OT: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    private fun sendC7() {
//        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = mBleCmdUtils.createCommand(0xC7, mKeyTwo?:return, mBleCmdUtils.stringCodeToHex("0000"))
//        showLog("\napp writeC1: ${notify_characteristic?.value}")
        updateLogText("\napp writeC7: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    private fun sendCE(){
        val adminCode = mBleCmdUtils.stringCodeToHex("0000")
        val sendBytes = byteArrayOf(adminCode.size.toByte()) + adminCode
//        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = mBleCmdUtils.createCommand(0xCE, mKeyTwo?:return, sendBytes)
//        showLog("\napp writeC1: ${notify_characteristic?.value}")
        updateLogText("\napp writeCE: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    private fun sendD6() {
        notify_characteristic?.value = mBleCmdUtils.createCommand(0xD6, mKeyTwo?:return)
//        showLog("\napp writeD6: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    fun sendD7(toLock: Boolean) {
        notify_characteristic?.value = mBleCmdUtils.createCommand(
            0xD7,
            mKeyTwo?:return,
            if(toLock)byteArrayOf(0x00)else byteArrayOf(0x01))
        updateLogText("\napp writeD7: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    private fun updateLogText(log: String) = viewModelScope.launch {
        mLogText.value = log
    }

    private fun closeBLEGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt?.disconnect()
            mBluetoothGatt?.close()
            mBluetoothAdapter?.cancelDiscovery()
            mBluetoothLeScanner?.stopScan(mScanCallback)
            updateLogText("Stop bleGatt connection.")
            viewModelScope.launch { mGattStatus.value = false }
//            oneLockViewModel.mLockBleStatus.value = false
//            mLockSetting = null
        }
    }

    //do when leave the one lock page
    fun CloseGattScope(){
        bleGattScope?.cancel()
    }

    //do when leave app
    override fun onCleared() {
        bleGattScope?.cancel()
        super.onCleared()
    }
}