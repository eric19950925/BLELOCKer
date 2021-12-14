package com.example.blelocker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.util.Base64
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.gson.JsonParser
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import android.bluetooth.BluetoothGattDescriptor

import android.bluetooth.BluetoothGattCharacteristic

import android.bluetooth.BluetoothGattService
import android.content.*
import androidx.navigation.findNavController
import com.example.blelocker.entity.DeviceToken
import com.example.blelocker.entity.LockConfig
import com.example.blelocker.entity.LockConnectionInformation
import com.example.blelocker.entity.LockSetting
import com.example.blelocker.entity.LockStatus.BATTERY_ALERT
import com.example.blelocker.entity.LockStatus.BATTERY_GOOD
import com.example.blelocker.entity.LockStatus.BATTERY_LOW
import com.example.blelocker.entity.LockStatus.LOCKED
import com.example.blelocker.entity.LockStatus.UNKNOWN
import com.example.blelocker.entity.LockStatus.UNLOCKED
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger


class MainActivity : AppCompatActivity() {

    companion object {
        const val CIPHER_MODE = "AES/ECB/NoPadding"
        const val BARCODE_KEY = "SoftChefSunion65"
        val NOTIFICATION_CHARACTERISTIC = UUID.fromString("de915dce-3539-61ea-ade7-d44a2237601f")
        val SUNION_SERVICE_UUID = UUID.fromString("fc3d8cf8-4ddc-7ade-1dd9-2497851131d7")
        const val DATA = "DATA"
        const val MY_LOCK_QRCODE = "MY_LOCK_QRCODE"
        const val MY_LOCK_TOKEN = "MY_LOCK_KEYTWO"
    }
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    var mLockConnectionInfo : LockConnectionInformation? = null
    private val parser = JsonParser()
    private var mHandler: Handler? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothDevice: BluetoothDevice? = null
    var randomNumberOne : ByteArray? = null
    var keyTwo : ByteArray? = null
    var isLockFromSharing: Boolean?=null
    var mPermanentToken: String?=null
    var mQRcode: String?=null



        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            mHandler = Handler()
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            ble_scan_btn.setOnClickListener {
                if(checkPermissions()!=true)return@setOnClickListener
                if(checkBTenable()!=true)return@setOnClickListener
                checkToConnect()
            }
            ble_connect_btn.setOnClickListener {
                BluetoothDeviceConnByGatt(mBluetoothDevice)
            }
//            setup_btn.setOnClickListener {
//                setup ()
//            }
            sentC0_btn.setOnClickListener {
                sendC0()
            }

            sentD6_btn.setOnClickListener {
                sendD6()
            }
            sentD7_btn.setOnClickListener {
                sendD7()
            }
            sentC7_btn.setOnClickListener {
                sendC7()

            }


//        bt_connect_btn.setOnClickListener {
//            if(checkPermissions()!=true)return@setOnClickListener
//            if(checkBTenable()!=true)return@setOnClickListener
//            checkToConnectBT()
//        }

    }

    override fun onSupportNavigateUp(): Boolean {

        return findNavController(R.id.my_nav_host_fragment).navigateUp()
    }

    override fun onPause() {
        //跳開啟藍芽提示會進來，若關閉scan會出錯
        closeBLEGatt()
        super.onPause()
    }

    override fun onDestroy() {
        closeBLEGatt()
        pauseScan()
        super.onDestroy()
    }

    override fun onStop() {
        pauseScan()
        closeBLEGatt()
        super.onStop()
    }

    private fun setup() {
        val sunion_service = getGattService(SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(NOTIFICATION_CHARACTERISTIC)
        mBluetoothGatt?.setCharacteristicNotification(notify_characteristic,true)

        val descriptor = notify_characteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        mBluetoothGatt?.writeDescriptor(descriptor)
    }
    private fun sendC0() {
        val sunion_service = getGattService(SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(NOTIFICATION_CHARACTERISTIC)
        val keyOne = Base64.decode(mLockConnectionInfo?.keyOne, Base64.DEFAULT)
//        val permanentToken = Base64.decode(mLockConnectionInfo?.permanentToken, Base64.DEFAULT)
        (notify_characteristic?:return).value = createCommand(0xC0, keyOne)
//        showLog("\napp writeC0: ${notify_characteristic.value}")
        Log.d("TAG","\napp writeC0: ${notify_characteristic.value}")
        randomNumberOne = resolveC0(keyOne,notify_characteristic.value)
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    private fun sendC1(keyTwo: ByteArray) {
        val sunion_service = getGattService(SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(NOTIFICATION_CHARACTERISTIC)
        val permanentToken = Base64.decode(mPermanentToken, Base64.DEFAULT)
        isLockFromSharing = mLockConnectionInfo?.sharedFrom != null && mLockConnectionInfo?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = createCommand(0xC1, keyTwo,permanentToken)
        Log.d("TAG","\napp writeC1: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    private fun sendC1withOTToken(keyTwo: ByteArray) {
        val sunion_service = getGattService(SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(NOTIFICATION_CHARACTERISTIC)
        val keyOne = Base64.decode(mLockConnectionInfo?.keyOne, Base64.DEFAULT)
        val permanentToken = Base64.decode(mLockConnectionInfo?.oneTimeToken, Base64.DEFAULT)
        isLockFromSharing = mLockConnectionInfo?.sharedFrom != null && mLockConnectionInfo?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = createCommand(0xC1, keyTwo, permanentToken)
//        showLog("\napp writeC1: ${notify_characteristic?.value}")
        Log.d("TAG","\napp writeC1_OT: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    private fun sendC7() {
        val sunion_service = getGattService(SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(NOTIFICATION_CHARACTERISTIC)
        isLockFromSharing = mLockConnectionInfo?.sharedFrom != null && mLockConnectionInfo?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = createCommand(0xC7, keyTwo?:return, stringCodeToHex("0000"))
//        showLog("\napp writeC1: ${notify_characteristic?.value}")
        Log.d("TAG","\napp writeC7: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    fun stringCodeToHex(code: String): ByteArray {
        return code.takeIf { it.isNotBlank() }
            ?.filter { it.isDigit() }
            ?.map { Character.getNumericValue(it).toByte() }
            ?.toByteArray()
            ?: throw IllegalArgumentException("Invalid user code string")
    }

    private fun sendD6() {
        val sunion_service = getGattService(SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(NOTIFICATION_CHARACTERISTIC)
        notify_characteristic?.value = createCommand(0xD6, keyTwo?:return)
//        showLog("\napp writeD6: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    private fun sendD7() {
        val sunion_service = getGattService(SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(NOTIFICATION_CHARACTERISTIC)
        notify_characteristic?.value = createCommand(0xD7, keyTwo?:return, byteArrayOf(0x01))
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

    fun Byte.unSignedInt() = this.toInt() and 0xFF

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
//            0xCE -> ce(serialIncrementAndGet(), key, data)
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

    // to get Gatt service
    private fun getGattService(uuid: UUID): BluetoothGattService? {
        val service = mBluetoothGatt!!.getService(uuid)
//        if (service == null) Util.toast(this, "Can not find service UUID= $uuid")
        return service
    }


    private fun checkBTenable(): Boolean {
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

    }

    private fun checkToConnectBT() {

        val bool = mBluetoothAdapter?.startDiscovery()
        Log.i("", bool.toString())
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter)

//        unregisterReceiver(mReceiver)//when should do this???
    }
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // A Bluetooth device was found
                // Getting device information from the intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
//                showLogBT("scan: ${device?.address}")
            }
        }
    }

    private fun checkToConnect() {

        //1) get bluetoothAdapter
//        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
//            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//            bluetoothManager.adapter
//        }
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothLeScanner = (mBluetoothManager?:return).adapter.bluetoothLeScanner
        //2) check bluetooth is open

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        mBluetoothManager!!.adapter?.takeIf { !it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 124)
        }

//        showLog("current macAddress: ${mLockConnectionInfo?.macAddress}")

        //work...need lacation premission, and sunion's lock is bt not ble
        mBluetoothLeScanner!!.startScan(mScanCallback) // 開始搜尋


        mHandler!!.postDelayed({
            mBluetoothLeScanner!!.stopScan(mScanCallback)
            invalidateOptionsMenu()
        }, 5000)



    }

    private val mScanCallback = object: ScanCallback() {
        @SuppressLint("NewApi")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
//            showLog("scan: ${result?.device?.address}, isConnectable: ${result?.isConnectable}")
//            showLogBLE("scan: ${result?.device?.address}")

            if (result?.device?.address == mLockConnectionInfo?.macAddress) {

//                showLog("Find device: ${result?.device?.name}")

//                mBluetoothDevice = result?.device
                BluetoothDeviceConnByGatt(result?.device)

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

    private fun BluetoothDeviceConnByGatt(device: BluetoothDevice?) {
//        showLog("Trying to connect by gatt.")
        mBluetoothGatt = device?.connectGatt(this@MainActivity, false, mBluetoothGattCallback)
    }

    private fun closeBLEGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt?.disconnect()
            mBluetoothGatt?.close()
            mBluetoothAdapter?.cancelDiscovery()
//            mBluetoothGatt = null
        }
    }
    val mBluetoothGattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when(status){
                0 -> when(newState){
                    2 -> this@MainActivity.runOnUiThread {/*showLog("GATT連線成功")*/}
                    else -> this@MainActivity.runOnUiThread {/*showLog("GATT連線中斷")*/}
                }
                else -> this@MainActivity.runOnUiThread {/*showLog("GATT連線出錯: ${status}")*/}
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
                BluetoothGatt.GATT_FAILURE -> this@MainActivity.runOnUiThread {/*showLog("Service discovery Failure.")*/}
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
            val keyOne = Base64.decode(mLockConnectionInfo?.keyOne, Base64.DEFAULT)
            val decrypted = decrypt(
                keyOne,
                characteristic?.value?:return
            )
            when(decrypted?.component3()?.unSignedInt()){
                0xC0 -> {
                    Log.d("TAG","C0 notyfy ramNum2")
                    generateKeyTwo(randomNumberOne?:return,resolveC0(keyOne,characteristic.value)){
                        keyTwo = it
//                        showLog("C0 notyfy ramNum2\nApp use it to generateKeyTwo = $keyTwo")
                        if((mPermanentToken?:return@generateKeyTwo).isBlank())sendC1withOTToken(it)
                        else sendC1(it)
                    }

                }
            }
            val decrypted2 = decrypt(
                keyTwo?:return,
                characteristic.value
            )
            when(decrypted2?.component3()?.unSignedInt()){
                0xC1 -> {
                    val dataFromDevice = resolveC1(keyTwo?:return, characteristic.value)
                    if(dataFromDevice.toHex().length > 10){
                        Log.d("TAG","one time token : ${dataFromDevice.toHex()}")
                    }
                    else {
//                    val deviceToken = determineTokenState(tokenStateFromDevice, isLockFromSharing?:return)
                        val deviceToken = determineTokenState(dataFromDevice, false)
                        val permission = determineTokenPermission(dataFromDevice)
                        Log.d("TAG", "C1 notyfy token state : ${dataFromDevice.toHex()}")
                        Log.d("TAG", "token permission: $permission")
                    }
                }
                0xC7 -> {
                    val dataFromDevice = resolveC7(keyTwo?:return, characteristic.value)
                    if(dataFromDevice == true){
                        Log.d("TAG","admin pincode had been set.")
                    }
                    else {
                        Log.d("TAG", "admin pincode had not been set.")
                    }
                }

                0xD6 -> {
                    val mLockSetting = resolveD6(keyTwo?:return, characteristic.value)
                    val islocked = if(mLockSetting.status==0)"locked" else "unlock"
                    this@MainActivity.runOnUiThread {
//                        showLog("D6 notyfy Lock's setting: ${mLockSetting}")
                    }
                }
                0xE5 -> {
                    decrypt(keyTwo?:return, characteristic.value)?.let { bytes ->
                        val permanentToken = extractToken(resolveE5(bytes))
                        mPermanentToken = (permanentToken as DeviceToken.PermanentToken).token
                    }
                    saveData()
                }
                0xEF -> {
//                        decrypt(keyTwo?:return, characteristic.value)?.let { bytes ->
//                            val permanentToken = extractToken(resolveE5(bytes))
//                            mPermanentToken = (permanentToken as DeviceToken.PermanentToken).token
//                        }
                    this@MainActivity.runOnUiThread {/*showLog("EF")*/}

                }
            }

        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            this@MainActivity.runOnUiThread {
                /*showLog(
                    "\nonDescriptorWrite \n" +
                            "--->in Characteristic\n" +
                            "--->[${descriptor?.uuid}]\n" +
                            "--->:${descriptor?.value}"
                )*/
            }
        }
    }

    private fun extractToken(byteArray: ByteArray): DeviceToken {
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

    private fun determineTokenPermission(data: ByteArray): String {
        return String(data.copyOfRange(1, 2))
    }


    private fun determineTokenState(data: ByteArray, isLockFromSharing: Boolean): Int {
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
                                0 -> UNLOCKED
                                1 -> LOCKED
                                else -> UNKNOWN
                            },
                            battery = bytes[6].unSignedInt(),
                            batteryStatus = when (bytes[7].unSignedInt()) {
                                0 -> BATTERY_GOOD
                                1 -> BATTERY_LOW
                                else -> BATTERY_ALERT
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


    private fun decryptQRcode(scanString: String, function: () -> Unit) {
        val base64Decoded = Base64.decode(scanString, Base64.DEFAULT)
        val decrypted = decrypt(
            MainActivity.BARCODE_KEY.toByteArray(),
            pad(base64Decoded, true)
        )
        decrypted?.let { result ->
            val data = String(result).replace(Regex("\\P{Print}"), "")
//            Timber.d("decrypted qr code: $data")
//            showLog("\ndecrypted qr code: $data")
            function.invoke()
//            Log.d("TAG",oneLockViewModel.mLockConnectionInfo.value.toString())
        } ?: throw IllegalArgumentException("Decrypted string is null")
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
    private lateinit var settings: SharedPreferences
    private fun readData() {
        settings = this.getSharedPreferences(DATA, 0)
        mQRcode = settings.getString(MY_LOCK_QRCODE, "")
        mPermanentToken = settings.getString(MY_LOCK_TOKEN, "")
        if(mQRcode.isNullOrBlank())return
        decryptQRcode(mQRcode?:return){

        }
        println("read k2: ${keyTwo?.toHex()}")

    }


    private fun saveData() {
        settings = this.getSharedPreferences(DATA, 0)
        settings.edit()
            .putString(MY_LOCK_QRCODE, mQRcode)
            .putString(MY_LOCK_TOKEN, mPermanentToken)
            .commit()
        println("store k2: ${keyTwo?.toHex()}")
    }
}


