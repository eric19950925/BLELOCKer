package com.sunionrd.blelocker.View.Settings.AutoUnLock

import android.app.*
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.sunionrd.blelocker.BluetoothUtils.BleCmdRepository
import com.sunionrd.blelocker.Entity.LockConnectionInformation
import com.sunionrd.blelocker.MainActivity
import com.sunionrd.blelocker.Model.LockConnInfoRepository
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.toHex
import com.sunionrd.blelocker.unSignedInt
import kotlinx.coroutines.*
import org.jetbrains.anko.powerManager
import org.koin.android.ext.android.inject
import java.util.*

class AutoUnlockService: Service() {

    private val mBinder: IBinder = LocalBinder()
    private val mBleCmdRepository: BleCmdRepository by inject()
    private val context: Context by inject()
    private val repository: LockConnInfoRepository by inject()

    private var bleScanScope: Job? = null
    private var mBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var mBluetoothLeScanner = mBluetoothManager.adapter.bluetoothLeScanner
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var sunion_service: BluetoothGattService? = null
    private var notify_characteristic: BluetoothGattCharacteristic? = null
    private var bleGattScope: Job? = null
    private var mGattStatus: Boolean? = null
    private var mLockData: LockConnectionInformation? = null
    private var mKeyOne: ByteArray? = null
    private var mKeyTwo: ByteArray? = null
    private var randomNumberOne : ByteArray? = null

    companion object {
        private const val CLOSE_BY_CLICK_NOTIFICATION = "123"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("TAG","onCreate")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TAG","onStartCommand")

//            manager.notify(0, notification)
        startForeground(1, getNotification())
        val startedFromNotification = intent?.getBooleanExtra(
            CLOSE_BY_CLICK_NOTIFICATION,
            false
        )

        // We got here because the user decided to remove location updates from the notification.
        if (startedFromNotification == true) {
            //click notify to close it will do this service again, so if finish unlock should stop Service!! todo
                // find that before scan and stop service also do it again(still do one time)
                    //the service will start again re-enter onStartCommand todo
            stopForeground(true)
            stopSelf()
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                Log.d("TAG","CoroutineScope")
                mLockData = repository.getLockConnectInformation("58:8E:81:A5:61:74")
                mKeyOne = Base64.decode(mLockData?.keyOne, Base64.DEFAULT)
            }
            //ble scan
            bleScanScope = CoroutineScope(Dispatchers.Main).launch { //must be main todo
                Log.d("TAG","bleScanScope ${mBluetoothLeScanner}")
                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                    .build()
                val filters : MutableList<ScanFilter> = arrayListOf()

                if (powerManager.isInteractive) {
                    Log.d("TAG","Scanning of Lock started, the device screen is on: ${powerManager.isInteractive}")
                    //need to open the app , then it will actuation.
                    mBluetoothLeScanner?.startScan(/*filters, scanSettings,*/ mScanCallback) // if use the filter it won't scan todo
//                    .scanBleDevices(scanSettings, ScanFilter.empty())
//                    .filter { it.bleDevice.name?.contains("BT_Lock") ?: false }
                } else {

                    Log.d("TAG","Scanning of Lock started, the device screen is on: ${powerManager.isInteractive}")
//                lockInformationRepository.get(macAddress)
//                    .flatMapObservable { lockInfo ->
//                        Timber.d("scan device name of ${lockInfo.deviceName} started, macAddress: $macAddress")
                    if (mLockData?.deviceName?.isNotBlank() == true) {
                        val filter = ScanFilter.Builder().setDeviceName(mLockData?.deviceName).build()
                        filters.add(filter)
                        mBluetoothLeScanner?.startScan(filters, ScanSettings.Builder().build(), mScanCallback)
                    } else {
                        val filter = ScanFilter.Builder().setDeviceAddress("58:8E:81:A5:61:74").build()
                        filters.add(filter)
                        mBluetoothLeScanner?.startScan(filters, ScanSettings.Builder().build(), mScanCallback)
                    }
//                    }
                }
                try {
                    var timestamp_ = 0
                    while(timestamp_<1800) { //scan for 30 min
                        delay(1000)
                        timestamp_ += 1
                    }
                }finally {
                    Log.d("TAG","bleScanScope stop")
                    stopScan()
                }
            }
        }

        return START_NOT_STICKY

    }

    private fun getNotification(): Notification? {
        val serviceIntent = Intent(this, AutoUnlockService::class.java)
        val text = getString(R.string.app_name)

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        serviceIntent.putExtra(CLOSE_BY_CLICK_NOTIFICATION, true)

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        val servicePendingIntent = PendingIntent.getService(
            this, 0, serviceIntent,
            MainActivity.MY_PENDING_INTENT_FLAG
        )
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, MainActivity.MY_PENDING_INTENT_FLAG)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("Day15", "Day15", NotificationManager.IMPORTANCE_HIGH)
            val longText = "Now Scan for your lock to unlock it.\nIf you want to stop this service, click the right button to stop.\nThis service will operate for 30 min."
            val builder = NotificationCompat.Builder(
                context,
                "Day15"
            )
            builder.setContentTitle("BTLocker - Auto Unlock")
                .setSmallIcon(R.drawable.ic_lock_main)//must have
                .setColor(Color.BLUE)
                .setContentText("Geofencing")//will be covered by big text
                .setSubText("Smart Notify!")
                .setStyle(NotificationCompat.BigTextStyle().bigText(longText))
//                .setLargeIcon(
//                    BitmapFactory.decodeResource(
//                        Resources.getSystem(),
//                    R.drawable.ic_auto_unlock
//                ))
//                .setAutoCancel(true)
//                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_lock_main, "Launch app", pendingIntent)
                .addAction(R.drawable.ic_pause_circle_outline, "Stop auto-unlock", servicePendingIntent)

            val notification : Notification = builder.build()
            val manager = (context.getSystemService(Context.NOTIFICATION_SERVICE)) as NotificationManager
            manager.createNotificationChannel(channel)
            return notification

        } else {
            Log.d(TAG, "${Build.VERSION.SDK_INT} < O(API 26) ")
            return null
        }
    }

    private val mScanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            //stop scan and do gatt connect
            if (result?.device?.address == mLockData?.macAddress) {
                Log.d("TAG","onScanResult : found ${mLockData?.macAddress}")
                mBluetoothGatt = result?.device?.connectGatt(context, false, mBluetoothGattCallback)
                bleGattScope = CoroutineScope(Dispatchers.Default).launch {
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
                stopScan()
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

    }

    val mBluetoothGattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when(status){
                0 -> when(newState){
                    2 -> Log.d("TAG","GATT連線成功")
                    else -> Log.d("TAG","GATT連線中斷")
                }
                else -> {
                    Log.d("TAG","GATT連線出錯: ${status}")
                    CloseBleScanScope()
                    CloseGattScope()
                    //133通常需要重啟手機藍芽
                    stopScan()
                }
            }
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
                CoroutineScope(Dispatchers.Default).launch {
                    delay(10000)
                    //gatt探索逾時
                    if(mGattStatus == null){
//                        mLockBleStatus.value = BleStatus.UNCONNECT
                        closeBLEGatt()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            when(status){
                BluetoothGatt.GATT_SUCCESS -> {
                    mGattStatus = true
                    sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
                    notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
                    mBluetoothGatt?.setCharacteristicNotification(notify_characteristic,true)
                    val descriptor = notify_characteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    mBluetoothGatt?.writeDescriptor(descriptor)
                }
                BluetoothGatt.GATT_FAILURE -> Log.d("TAG","Service discovery Failure.")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
//            updateLogText("\nwrited: ${characteristic?.value}")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            Log.d("TAG","\nSetup Notification")
            sendC0()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {

            val decrypted = mBleCmdRepository.decrypt(mKeyOne?:return, characteristic?.value?:return)
            when(decrypted?.component3()?.unSignedInt()){
                0xC0 -> {
//                    Log.d("TAG","C0 notify ramNum2")
                    mBleCmdRepository.generateKeyTwoThen(randomNumberOne?:return, mBleCmdRepository.resolveC0(mKeyOne?:return, characteristic.value)){
                        mKeyTwo = it
                        sendC1(mLockData?.permanentToken?:return@generateKeyTwoThen)
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
                        Log.d("TAG","Connect success, start to auto unlock")
                        sendD7(true)
                    }
                }
                0xD6 -> {
                    val current = mBleCmdRepository.resolveD6(mKeyTwo?:return, characteristic.value)
//                    viewModelScope.launch {
//                        mLockSetting.value = current
//                        mLockBleStatus.value = BleStatus.CONNECT
//                    }
                    Log.d("TAG","\nD6 notify Lock's setting: ${current}")
                    if (current.status == 1){
                        CloseGattScope()
                        stopForeground(true)
                        stopSelf()
                    }
                }
                0xEF -> {
                    Log.d("TAG","\nEF")
                }
            }

        }

    }

    fun sendC0() = CoroutineScope(Dispatchers.Default).launch {
        (notify_characteristic?:return@launch).value = mBleCmdRepository.createCommand(0xC0, mKeyOne?:return@launch)
        Log.d("TAG","\napp writeC0: ${notify_characteristic?.value}")
        randomNumberOne = mBleCmdRepository.resolveC0(mKeyOne?:return@launch,notify_characteristic?.value?:return@launch)
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    fun sendC1(init_permanentToken: String) {
        val permanentToken = Base64.decode(init_permanentToken, Base64.DEFAULT)
        notify_characteristic?.value = mBleCmdRepository.createCommand(0xC1, mKeyTwo?:return, permanentToken)
        Log.d("TAG","\napp writeC1: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    fun sendD7(toUnLock: Boolean) {
        notify_characteristic?.value = mBleCmdRepository.createCommand(
            0xD7,
            mKeyTwo?:return,
            if(toUnLock)byteArrayOf(0x00)else byteArrayOf(0x01))
        Log.d("TAG","\napp writeD7: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    fun stopScan(){
        mBluetoothLeScanner?.stopScan(mScanCallback)
    }
    //do when leave the one lock page
    fun CloseBleScanScope(){
        bleScanScope?.cancel()
    }
    //do when leave the one lock page
    fun CloseGattScope(){
        bleGattScope?.cancel()
    }

    private fun closeBLEGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt?.disconnect()
            mBluetoothGatt?.close()
            mBluetoothAdapter?.cancelDiscovery()
            mBluetoothLeScanner?.stopScan(mScanCallback)
            mGattStatus = false
        }
    }
    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }
    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        val service: AutoUnlockService
            get() = this@AutoUnlockService
    }

}