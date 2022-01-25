package com.example.blelocker.BluetoothUtils

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
import com.example.blelocker.Entity.*
import com.example.blelocker.Entity.DeviceToken.DeviceTokenState.VALID_TOKEN
import com.example.blelocker.MainActivity
import com.example.blelocker.MainActivity.Companion.NOTIFICATION_CHARACTERISTIC
import com.example.blelocker.toHex
import com.example.blelocker.unSignedInt
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleConnection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import kotlinx.android.synthetic.main.fragment_onelock.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import org.koin.core.KoinComponent
import org.koin.core.inject

class BleControlViewModel(
    @SuppressLint("StaticFieldLeak") val context: Context,
    private val mBleCmdRepository: BleCmdRepository,
    private val bleScanUseCase: BleScanUseCase
    ): ViewModel(), KoinComponent {
    private val statefulConnection: StatefulConnection by inject()
    private var mBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var mBluetoothLeScanner = mBluetoothManager.adapter.bluetoothLeScanner
    private var mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mBluetoothGatt: BluetoothGatt? = null

    private var bleScanScope: Job? = null
    private var bleGattScope: Job? = null
    private var mKeyOne: ByteArray? = null
    private var mKeyTwo: ByteArray? = null
    private var randomNumberOne : ByteArray? = null
    private var mBleDeviceMacAddress: String? = null
    private var connectionDisposable: CompositeDisposable? = null
    private var sunion_service: BluetoothGattService? = null
    private var notify_characteristic: BluetoothGattCharacteristic? = null
    var mRxBleConnection = MutableLiveData<RxBleConnection>()
    var mMacAddress = MutableLiveData<String>()

    var mCharacteristicValue = MutableLiveData<Pair<String, Any>>()
    val mLockBleStatus = MutableLiveData<Int?>()
    var mLockSetting = MutableLiveData<LockSetting>()
    var mGattStatus = MutableLiveData<Boolean>()
    var mLogText = MutableLiveData<String>()


    fun rxBleConnectWithLock(mLock: LockConnectionInformation, success:() -> Unit, failure:(e:Throwable) -> Unit){
        val disposable = bleScanUseCase
            .connectDevice(mLock.macAddress)
            .flatMap { rxConnect ->
                viewModelScope.launch {
                    mRxBleConnection.value = rxConnect
                }
                connectWithToken(mLock, rxConnect)
            }
            .subscribe({
                success.invoke()
            },{
                failure(it)
            })
        connectionDisposable?.add(disposable)
    }

    private fun connectWithToken(mLock: LockConnectionInformation, rxBleConnection: RxBleConnection): Observable<String> {
        val keyOne = Base64.decode(mLock.keyOne, Base64.DEFAULT)
        val token = if (mLock.permanentToken.isBlank()) {
            Base64.decode(mLock.oneTimeToken, Base64.DEFAULT)
        } else {
            Base64.decode(mLock.permanentToken, Base64.DEFAULT)
        }
        val isLockFromSharing =
            mLock.sharedFrom != null && mLock.sharedFrom.isNotBlank() ?: false

        return if (mLock.permanentToken.isBlank()) {
            connectWithOneTimeToken(mLock, rxBleConnection, keyOne, token, isLockFromSharing)
        } else {
            connectWithPermanentToken(keyOne, token, mLock, rxBleConnection, isLockFromSharing)
        }
    }


    private fun connectWithPermanentToken(
        keyOne: ByteArray,
        token: ByteArray,
        mLock: LockConnectionInformation,
        rxBleConnection: RxBleConnection,
        isLockFromSharing: Boolean)
    : Observable<String>{
        return  sendC0(rxBleConnection, keyOne, token)
            .flatMap { keyTwo ->
                Log.d("TAG","get k2")
                bleScanUseCase.rxSendC1(rxBleConnection, keyTwo, token, isLockFromSharing)
                    .filter { it.first == VALID_TOKEN }
                    .flatMap { stateAndPermission ->
                        rxBleConnection
                            .setupNotification(
                                NOTIFICATION_CHARACTERISTIC,
                                NotificationSetupMode.DEFAULT
                            ) // receive [C1], [D6] only
                            .flatMap { it }
                            .map { keyTwo to it }
                            .take(1)
                            .doOnNext { pair ->
//                                Timber.d("received receive [C1], [D6] in exchange permanent token")
                                viewModelScope.launch { mCharacteristicValue.value = "PermanentToken" to pair }
                            }
                            .flatMap { Observable.just(stateAndPermission.second) }
                    }
            }

    }

    fun sendC00(
        rxConnection: RxBleConnection,
        keyOne: ByteArray,
        token: ByteArray
    ): Observable<ByteArray>  {
//    ): Disposable  {
        val writeC0 = mBleCmdRepository.createCommand(0xC0, keyOne, token)
        Log.d("TAG", "writeC0 ${writeC0.toHex()}")
        return rxConnection.writeCharacteristic(NOTIFICATION_CHARACTERISTIC, writeC0)
            .toObservable()
//            .subscribe{
//                Log.d("TAG", "written C0 ${it.toHex()}")
//            }
    }

    fun setupC0Notify(
        rxConnection: RxBleConnection,
        keyOne: ByteArray
    ): Observable<ByteArray> {
        return rxConnection.setupNotification(
            NOTIFICATION_CHARACTERISTIC
        )
            .flatMap {
                notification -> notification
        }.filter { notification ->
            val decrypted = mBleCmdRepository.decrypt(
                keyOne,
                notification
            )
            println("filter [C0] decrypted: ${decrypted?.toHex()}")
            decrypted?.component3()?.unSignedInt() == 0xC0
        }
    }
    fun sendC0(
        rxConnection: RxBleConnection,
        keyOne: ByteArray,
        token: ByteArray
    ): Observable<ByteArray> {
        return Observables.zip(
            setupC0Notify(rxConnection, keyOne),
            sendC00(rxConnection, keyOne, token)
        ) { notification: ByteArray, written: ByteArray ->
            val randomNumberOne = mBleCmdRepository.resolveC0(keyOne, written)
            Log.d("TAG", "[C0] has written: ${written.toHex()}")
            Log.d("TAG", "[C0] has notified: ${notification.toHex()}")
            val randomNumberTwo = mBleCmdRepository.resolveC0(keyOne, notification)
            Log.d("TAG", "randomNumberTwo: ${randomNumberTwo.toHex()}")
            val keyTwo = mBleCmdRepository.generateKeyTwo(
                randomNumberOne = randomNumberOne,
                randomNumberTwo = randomNumberTwo
            )
            Log.d("TAG", "keyTwo: ${keyTwo.toHex()}")
            keyTwo
        }


    }
    private fun writeAndReadOnNotification(
        rxBleConnection: RxBleConnection,
        keyOne: ByteArray,
        token: ByteArray
    ): Observable<ByteArray?> {
        val notifObservable = rxBleConnection.setupNotification(NOTIFICATION_CHARACTERISTIC, NotificationSetupMode.DEFAULT)
        return notifObservable.flatMap { notificationObservable: Observable<ByteArray?> ->
            Observable.combineLatest(
                rxBleConnection.writeCharacteristic(NOTIFICATION_CHARACTERISTIC, mBleCmdRepository.createCommand(0xC0, keyOne, token)).toObservable(),
                notificationObservable.take(1),
                { writtenBytes: ByteArray, responseBytes: ByteArray ->
                        responseBytes
                }
            )
        }
    }

    private fun connectWithOneTimeToken(
        mLock: LockConnectionInformation,
        rxBleConnection: RxBleConnection,
        keyOne: ByteArray,
        oneTimeToken: ByteArray,
        isLockFromSharing: Boolean
    ): Observable<String>{
        return sendC0(rxBleConnection, keyOne, oneTimeToken)
            .flatMap { keyTwo ->
                Log.d("TAG","get k2")
                bleScanUseCase.rxSendC1(rxBleConnection, keyTwo, oneTimeToken, isLockFromSharing)
                    .take(1)
                    .filter { it.first == DeviceToken.ONE_TIME_TOKEN || it.first == DeviceToken.VALID_TOKEN }
                    .flatMap { stateAndPermission ->
                        rxBleConnection
                            .setupNotification(
                                NOTIFICATION_CHARACTERISTIC,
                                NotificationSetupMode.DEFAULT
                            )
                            .flatMap { it }
                            .filter { notification -> // [E5] will sent from device
                                mBleCmdRepository.decrypt(keyTwo, notification)?.let { bytes ->
                                    bytes.component3().unSignedInt() == 0xE5
                                } ?: false
                            }
                            .distinct { notification ->
                                mBleCmdRepository.decrypt(keyTwo, notification)?.component3()
                                    ?.unSignedInt() ?: 0xE5
                            }
                            .map { notification ->
                                val token =
                                    mBleCmdRepository.decrypt(keyTwo, notification)?.let { bytes ->
                                        val permanentToken = mBleCmdRepository.extractToken(mBleCmdRepository.resolveE5(bytes))
                                        permanentToken
                                    } ?: throw NotConnectedException()
                                keyTwo to token
                            }
                            .doOnNext { pair ->
//                                Timber.d("received receive [C1], [E5], [D6] in exchange one time token")
                                val token = pair.second
                                if (token is DeviceToken.PermanentToken) {
                                    viewModelScope.launch {
                                        mCharacteristicValue.value = "OneTimeToken" to pair
                                        mLockBleStatus.value = BleStatus.CONNECT
                                    }
                                }
                            }
                            .flatMap { pair ->
                                val token = pair.second
                                if (token is DeviceToken.PermanentToken) Observable.just(token.permission) else Observable.never()
                            }
                    }
            }
    }

    fun disposeConnection(){
        connectionDisposable?.dispose()
    }

    fun clearConnection(){
        connectionDisposable?.clear()
    }



    fun bleScan(macAddress: String, keyOne: String){
        bleScanScope = viewModelScope.launch(Dispatchers.Main){
            mBluetoothLeScanner?.startScan(mScanCallback) // 開始搜尋
            mLockBleStatus.value = BleStatus.CONNECTTING
            try{
                var timestamp_ = 0
                while(timestamp_<30) {
                    delay(1000)
                    timestamp_ += 1
                    updateLogText("Had scanned for $timestamp_ seconds.")
                }
            }finally {
                updateLogText("Stop ble scan and counting.")
                pauseScan()
                //若沒有進入Gatt連線流程，將Connecting Status設為空值
                if (mBluetoothGatt == null) {
                    mLockBleStatus.value = null
                }
            }
        }
        mBleDeviceMacAddress = macAddress
        mKeyOne = Base64.decode(keyOne, Base64.DEFAULT)
    }
    private val mScanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result?.device?.address == mBleDeviceMacAddress) {

                updateLogText("Find device: ${result?.device?.name}")

                mBluetoothGatt = result?.device?.connectGatt(context, false, mBluetoothGattCallback)
                bleGattScope = viewModelScope.launch(Dispatchers.Main){
                    try{
                        var timestamp_ = 0
                        while(true){
                            delay(10000)
                            timestamp_ += 10
                            Log.d("TAG","bleGatt has connect for $timestamp_ sec.")
                        }
                    }finally {
                        Log.d("TAG","Stop bleGatt connection.")
                        closeBLEGatt()
                    }
                }
                CloseBleScanScope()
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
                    viewModelScope.launch {mLockBleStatus.value = BleStatus.UNCONNECT}
                    updateLogText("GATT連線出錯: ${status}")
                    CloseBleScanScope()
                    CloseGattScope()
                    //133通常需要重啟手機藍芽
                    pauseScan()
                }
            }
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
                viewModelScope.launch {
                    delay(10000)
                    //gatt探索逾時
                    if(mGattStatus.value == null){
                        mLockBleStatus.value = BleStatus.UNCONNECT
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
            sendC0()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val decrypted = mBleCmdRepository.decrypt(mKeyOne?:return, characteristic?.value?:return)
            when(decrypted?.component3()?.unSignedInt()){
                0xC0 -> {
//                    Log.d("TAG","C0 notify ramNum2")
                    mBleCmdRepository.generateKeyTwoThen(randomNumberOne?:return, mBleCmdRepository.resolveC0(mKeyOne?:return, characteristic.value)){
                        mKeyTwo = it
                        viewModelScope.launch { mCharacteristicValue.value = "C0" to true }
                    }

                }
                else -> {
                    Log.d("TAG","else")
                }
            }
            val decrypted2 = mBleCmdRepository.decrypt(mKeyTwo?:return, characteristic.value)
            when(decrypted2?.component3()?.unSignedInt()){
                0xC1 -> {
                    val dataFromDevice = mBleCmdRepository.resolveC1(mKeyTwo?:return, characteristic.value)
                    if(dataFromDevice.toHex().length > 10){
                        Log.d("TAG","one time token : ${dataFromDevice.toHex()}")
                    }
                    else {
                        viewModelScope.launch { mCharacteristicValue.value = "C1" to dataFromDevice }
                    }
                }
                0xC7 -> {
                    val dataFromDevice = mBleCmdRepository.resolveC7(mKeyTwo?:return, characteristic.value)
                    if(dataFromDevice){
                        viewModelScope.launch { mCharacteristicValue.value = "C7" to true }
                        Log.d("TAG","admin pincode had been set.")
                        updateLogText("\nadmin pincode had been set.")
                    }
                    else {
                        Log.d("TAG", "admin pincode had not been set.")
                    }
                }
                0xC8 -> {
                    val dataFromDevice = mBleCmdRepository.resolveC8(mKeyTwo?:return, characteristic.value)
                    if(dataFromDevice == true){
                        viewModelScope.launch { mCharacteristicValue.value = "C7" to true }
                        Log.d("TAG","admin pincode had been set.")
                        updateLogText("\nadmin pincode had been set.")
                    }
                    else {
                        Log.d("TAG", "admin pincode had not been set.")
                    }
                }
                0xCE -> {
                    val dataFromDevice = mBleCmdRepository.resolveCE(mKeyTwo?:return, characteristic.value)
                    if(dataFromDevice){
                        CloseGattScope()
                        mLockBleStatus.value = BleStatus.UNCONNECT
                    }
                    viewModelScope.launch { mCharacteristicValue.value = "CE" to dataFromDevice }
                }

                0xD4 -> {
                    val current = mBleCmdRepository.resolveD4(mKeyTwo?:return, characteristic.value)
                    viewModelScope.launch { mCharacteristicValue.value = "D4" to current }
                    updateLogText("\nD4 notify Lock's setting: ${current}")

                }

                0xD5 -> {
                    val current = mBleCmdRepository.resolveD5(mKeyTwo?:return, characteristic.value)
                    viewModelScope.launch { mCharacteristicValue.value = "D5" to current }
                    if (current) {
                        updateLogText("\nD5 notify Set cfg success.")
                    } else {
                        updateLogText("\nD5 notify Set cfg failure.")
                    }
                }

                0xD6 -> {
                    val current = mBleCmdRepository.resolveD6(mKeyTwo?:return, characteristic.value)
                    viewModelScope.launch {
                        mLockSetting.value = current
                        mLockBleStatus.value = BleStatus.CONNECT
                    }
                    updateLogText("\nD6 notify Lock's setting: ${current}")

                }
                0xE5 -> {
                    mBleCmdRepository.decrypt(mKeyTwo?:return, characteristic.value)?.let { bytes ->
                        viewModelScope.launch { mCharacteristicValue.value = "E5" to mBleCmdRepository.extractToken(mBleCmdRepository.resolveE5(bytes)) }
//                        sendC7()//有了token就設admin code
                    }

                }
                0xEF -> {

                    updateLogText("\nEF")

                }
            }

        }

    }

    fun sendC0() = viewModelScope.launch {
        (notify_characteristic?:return@launch).value = mBleCmdRepository.createCommand(0xC0, mKeyOne?:return@launch)
        updateLogText("\napp writeC0: ${notify_characteristic?.value}")
        randomNumberOne = mBleCmdRepository.resolveC0(mKeyOne?:return@launch,notify_characteristic?.value?:return@launch)
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    fun sendC1(init_permanentToken: String) {
        val permanentToken = Base64.decode(init_permanentToken, Base64.DEFAULT)
        notify_characteristic?.value = mBleCmdRepository.createCommand(0xC1, mKeyTwo?:return, permanentToken)
        updateLogText("\napp writeC1: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    fun sendC1withOTToken(init_oneTimeToken: String) {
        val permanentToken = Base64.decode(init_oneTimeToken, Base64.DEFAULT)
        notify_characteristic?.value = mBleCmdRepository.createCommand(0xC1, mKeyTwo?:return, permanentToken)
        updateLogText("\napp writeC1_OT: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    fun sendC7(code: String) {
//        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = mBleCmdRepository.createCommand(0xC7, mKeyTwo?:return, mBleCmdRepository.stringCodeToHex(code))
        updateLogText("\napp writeC7: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    fun sendC8(code: String) {
//        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = mBleCmdRepository.createCommand(0xC8, mKeyTwo?:return, mBleCmdRepository.stringCodeToHex(code))
        updateLogText("\napp writeC8: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    fun sendCE(toString: String) {
        val adminCode = mBleCmdRepository.stringCodeToHex(toString)
        val sendBytes = byteArrayOf(adminCode.size.toByte()) + adminCode
//        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = mBleCmdRepository.createCommand(0xCE, mKeyTwo?:return, sendBytes)
        updateLogText("\napp writeCE: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    fun getLockStatus(){
        notify_characteristic?.value = mBleCmdRepository.createCommand(0xD4, mKeyTwo?:return)
        updateLogText("\napp writeD4: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    fun setAutoLock(setting: LockConfig){
        val setting_bytes = mBleCmdRepository.settingBytes(setting)
        notify_characteristic?.value = mBleCmdRepository.createCommand(0xD5, mKeyTwo?:return, setting_bytes)
        updateLogText("\napp writeD5: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    fun checkOrientation(){
        notify_characteristic?.value = mBleCmdRepository.createCommand(0xCC, mKeyTwo?:return)
        updateLogText("\napp writeCC: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    private fun sendD6() {
        notify_characteristic?.value = mBleCmdRepository.createCommand(0xD6, mKeyTwo?:return)
//        showLog("\napp writeD6: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    fun sendD7(toLock: Boolean) {
        notify_characteristic?.value = mBleCmdRepository.createCommand(
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
            mGattStatus.value = false
        }
    }

    //do when leave the one lock page
    fun CloseGattScope(){
        bleGattScope?.cancel()
    }

    //do when leave the one lock page
    fun CloseBleScanScope(){
        bleScanScope?.cancel()
    }

    //do when leave app
    override fun onCleared() {
        CloseGattScope()
        mLockBleStatus.value = null
        CloseBleScanScope()
        super.onCleared()
    }
}