package com.example.blelocker.View

import android.Manifest
import android.animation.ValueAnimator
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.blelocker.*
import com.example.blelocker.MainActivity.Companion.DATA
import com.example.blelocker.MainActivity.Companion.MY_LOCK_QRCODE
import com.example.blelocker.entity.DeviceToken
import com.example.blelocker.entity.LockConnectionInformation
import com.example.blelocker.entity.LockSetting
import com.example.blelocker.entity.LockStatus.LOCKED
import com.example.blelocker.entity.LockStatus.UNLOCKED
import kotlinx.android.synthetic.main.fragment_onelock.*
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

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

    var mLockSetting: LockSetting?=null
    var mGattStatus: Boolean?=null
    var uiScope: Job?=null
    var testScope: Job?=null

    private val rotateAnimation = RotateAnimation(
        0f, 359f,
        Animation.RELATIVE_TO_SELF, 0.5f,
        Animation.RELATIVE_TO_SELF, 0.5f
    ).apply {
        this.duration = 2000
        this.repeatCount = ValueAnimator.INFINITE
    }

    override fun onViewHasCreated() {
        setHasOptionsMenu(true)
        my_toolbar.inflateMenu(R.menu.my_menu)
        my_toolbar.title = "BLE LOCKer"
//        my_toolbar.menu.clear()
//
        log_tv.movementMethod = ScrollingMovementMethod.getInstance()
        log_tv.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                while (log_tv.canScrollVertically(1)) {
                    log_tv.scrollBy(0, 10)
                }
            }
        })


        oneLockViewModel.mLockBleStatus.observe(this){
//            Log.d("TAG",it.toString())
            if(it != true){
                iv_my_lock_ble_status.visibility = View.VISIBLE
                btn_lock.clearAnimation()
                btn_lock.visibility = View.GONE
                iv_factory.visibility = View.GONE
            }else{
                iv_my_lock_ble_status.visibility = View.GONE
                btn_lock.visibility = View.VISIBLE
                if(mLockSetting!=null)return@observe
                btn_lock.setBackgroundResource(R.drawable.ic_loading_main)
                btn_lock.startAnimation(rotateAnimation)
                btn_lock_wait()
            }
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        iv_my_lock_ble_status.setOnClickListener {
            if(checkPermissions()!=true)return@setOnClickListener
            if(checkBTenable()!=true)return@setOnClickListener
            if(oneLockViewModel.mLockConnectionInfo.value == null)return@setOnClickListener
            bleScan()
        }
        btn_lock.setOnClickListener {
            sendD7(mLockSetting?.status == LOCKED)
            btn_lock_wait()
        }
        my_toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.scan -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_onelock_to_scan)
                    true
                }
                R.id.delete -> {
                    cleanLog()
                    true
                }
                R.id.play -> {
                    my_toolbar.menu.findItem(R.id.play).isVisible = false
                    my_toolbar.menu.findItem(R.id.pause).isVisible = true
                    testScope = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        try{
                            var timestamp_ = 0
                            while(timestamp_<60) {
                                delay(1000)
                                timestamp_ += 1
                                requireActivity().runOnUiThread{
                                    showLog("Thread Test ${timestamp_}")
                                }
                            }
                        }finally {
                            requireActivity().runOnUiThread{
                                showLog("Stop Thread Test.")
                                my_toolbar.menu.findItem(R.id.pause).isVisible = false
                                my_toolbar.menu.findItem(R.id.play).isVisible = true
                            }
                        }
                    }
                    true
                }
                R.id.pause -> {
                    my_toolbar.menu.findItem(R.id.pause).isVisible = false
                    my_toolbar.menu.findItem(R.id.play).isVisible = true
                    testScope?.cancel()
                    true
                }
                else -> false
            }
        }

        iv_factory.setOnClickListener {
            //check admincode
//            if(oneLockViewModel.mLockConnectionInfo.value?.adminCode.isNullOrBlank())return@setOnClickListener //wrong way
            //factory reset
            sendCE()
        }




















    }

    private fun btn_lock_wait() {
        btn_lock.isClickable = false
    }

    private fun btn_lock_ready() {
        btn_lock.isClickable = true
    }

    private fun bleScan() {
        mBluetoothManager = requireContext().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothLeScanner = (mBluetoothManager?:return).adapter.bluetoothLeScanner

//        test block before scan
//        runBlocking(Dispatchers.Default){
//            var timestamp_ = 0
//            while(timestamp_<30) {
//                delay(1000)
//                timestamp_ += 1
//                requireActivity().runOnUiThread{ showLog("Block for ${timestamp_} seconds.")}//Can't work until the end of time
//                Log.d("TAG","Block for ${timestamp_} seconds.")
//            }
//        }

        mBluetoothLeScanner?.startScan(mScanCallback) // 開始搜尋
        requireActivity().runOnUiThread{ oneLockViewModel.mLockBleStatus.value = true }//Start icon animation
        //using requireActivity() might meet Fragment not attached to Activity error and crash.
        uiScope = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            try{
                var timestamp_ = 0
                while(timestamp_<30) {
                    delay(1000)
                    timestamp_ += 1
                    requireActivity().runOnUiThread{
                        showLog("Had scanned for ${timestamp_} seconds.")
                    }
                }
            }finally {
                requireActivity().runOnUiThread{
                    showLog("Stop ble scan and counting.")
                    mBluetoothLeScanner?.stopScan(mScanCallback)
                    //沒有在連gatt，藍芽scan已逾時(30sec)
                    if (mBluetoothGatt == null) {
                        oneLockViewModel.mLockBleStatus.value = false
                    }
                }
            }
        }

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
            uiScope?.cancel()
        }
    }

    override fun onPause() {
        Log.d("TAG","onPause")
        //跳開啟藍芽提示會進來，若關閉ble scan會出錯
        //click btn will trigger here, so don't close gatt here.
//        closeBLEGatt()
        super.onPause()
    }

    override fun onStop() {
        Log.d("TAG","onStop")
        closeBLEGatt()
        super.onStop()
    }

    override fun onDestroyView() {
        Log.d("TAG","onDestroyView")
        closeBLEGatt()
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d("TAG","onDestroy")
        closeBLEGatt()
        super.onDestroy()
    }

    override fun onDetach() {
        Log.d("TAG","onDetach")
        closeBLEGatt()
        super.onDetach()
    }

    override fun onResume() {
        readSharedPreference()
        //todo : auto ble conn
        Log.d("TAG","onResume")
        super.onResume()
    }

    //todo:press back should leave this app


    private fun closeBLEGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt?.disconnect()//不知有無影響
            mBluetoothGatt?.close()
            mBluetoothAdapter?.cancelDiscovery()
            mBluetoothLeScanner?.stopScan(mScanCallback)
            oneLockViewModel.mLockBleStatus.value = false
            mLockSetting = null
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
                    requireActivity().runOnUiThread {
                        oneLockViewModel.mLockBleStatus.value = false
                        pauseScan()
                        closeBLEGatt()
                        //133通常需要重啟手機藍芽
                        showLog("GATT連線出錯: ${status}")
                    }
                }
            }
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
                requireActivity().runOnUiThread {
                    mHandler?.postDelayed({
//            gatt探索逾時
                        if(mGattStatus == null){
                            oneLockViewModel.mLockBleStatus.value = false
                            closeBLEGatt()
                        }
                    }, 10000)
                }
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
                    mGattStatus = true
                    setup()
                }
                BluetoothGatt.GATT_FAILURE -> requireActivity().runOnUiThread {showLog("Service discovery Failure.")}
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
                    Log.d("TAG","C0 notify ramNum2")
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
                        Log.d("TAG", "C1 notify token state : ${dataFromDevice.toHex()}")
                        Log.d("TAG", "token permission: $permission")
                    }
                }
                0xC7 -> {
                    val dataFromDevice = oneLockViewModel.resolveC7(keyTwo?:return, characteristic.value)
                    if(dataFromDevice == true){

                        var newLockInfo: LockConnectionInformation?=null
                        oneLockViewModel.mLockConnectionInfo.value?.let {
                            newLockInfo = LockConnectionInformation(
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
                                adminCode = "0000"
                            )
                        }
                        requireActivity().runOnUiThread {oneLockViewModel.mLockConnectionInfo.value = newLockInfo}

                        Log.d("TAG","admin pincode had been set.")
                    }
                    else {
                        Log.d("TAG", "admin pincode had not been set.")
                    }
                }
                0xCE -> {
                    val dataFromDevice = oneLockViewModel.resolveCE(keyTwo?:return, characteristic.value)
                    if(dataFromDevice){
                        requireActivity().runOnUiThread {
                            //clean data
                            oneLockViewModel.mLockConnectionInfo.value = null
                            claenSP()
                            readSharedPreference()
                            //close gatt
                            closeBLEGatt()
                            //set ble scan btn not clickable
                            iv_my_lock_ble_status.isClickable = false
                            showLog("CE notify 重置成功")
                        }
                    }else {
                        requireActivity().runOnUiThread {showLog("CE notify 重置失敗")}
                    }
                }

                0xD6 -> {
                    mLockSetting = oneLockViewModel.resolveD6(keyTwo?:return, characteristic.value)
                    val islocked = if(mLockSetting?.status==0)"locked" else "unlock"
                    requireActivity().runOnUiThread {
                        showLog("\nD6 notify Lock's setting: ${mLockSetting}")
                        btn_lock.clearAnimation()
                        when (mLockSetting?.status) {
                            LOCKED -> {
                                btn_lock.setBackgroundResource(R.drawable.ic_lock_main)
                                iv_factory.visibility = View.VISIBLE
                                btn_lock_ready()
                            }
                            UNLOCKED -> {
                                btn_lock.setBackgroundResource(R.drawable.ic_auto_unlock)
                                iv_factory.visibility = View.VISIBLE
                                btn_lock_ready()
                            }
                        }
                    }
                }
                0xE5 -> {
                    oneLockViewModel.decrypt(keyTwo?:return, characteristic.value)?.let { bytes ->
                        val permanentToken = oneLockViewModel.extractToken(oneLockViewModel.resolveE5(bytes))
                        mPermanentToken = (permanentToken as DeviceToken.PermanentToken).token
                        sendC7()//有了token就設admin code
                    }
                    saveData()
                    readSharedPreference()
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
                    "\nsetupNotification \n" +
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
        requireActivity().runOnUiThread {
            showLog("\napp writeC0: ${notify_characteristic.value}")}
        randomNumberOne = oneLockViewModel.resolveC0(keyOne,notify_characteristic.value)
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    private fun sendC1(keyTwo: ByteArray) {
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        val permanentToken = Base64.decode(mPermanentToken, Base64.DEFAULT)
        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = oneLockViewModel.createCommand(0xC1, keyTwo,permanentToken)
        requireActivity().runOnUiThread {
            showLog("\napp writeC1: ${notify_characteristic?.value}")}
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
        requireActivity().runOnUiThread {
            showLog("\napp writeC1_OT: ${notify_characteristic?.value}")}
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    private fun sendC7() {
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = oneLockViewModel.createCommand(0xC7, keyTwo?:return, oneLockViewModel.stringCodeToHex("0000"))
//        showLog("\napp writeC1: ${notify_characteristic?.value}")
        requireActivity().runOnUiThread {
            showLog("\napp writeC7: ${notify_characteristic?.value}")}
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    private fun sendCE(){
        val adminCode = oneLockViewModel.stringCodeToHex("0000")
        val sendBytes = byteArrayOf(adminCode.size.toByte()) + adminCode
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
        notify_characteristic?.value = oneLockViewModel.createCommand(0xCE, keyTwo?:return, sendBytes)
//        showLog("\napp writeC1: ${notify_characteristic?.value}")
        requireActivity().runOnUiThread {
            showLog("\napp writeCE: ${notify_characteristic?.value}")}
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }

    private fun sendD6() {
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        notify_characteristic?.value = oneLockViewModel.createCommand(0xD6, keyTwo?:return)
//        showLog("\napp writeD6: ${notify_characteristic?.value}")
        mBluetoothGatt?.writeCharacteristic(notify_characteristic)
    }
    private fun sendD7(toLock: Boolean) {
        val sunion_service = mBluetoothGatt?.getService(MainActivity.SUNION_SERVICE_UUID)
        val notify_characteristic = sunion_service?.getCharacteristic(MainActivity.NOTIFICATION_CHARACTERISTIC)
        notify_characteristic?.value = oneLockViewModel.createCommand(
            0xD7,
            keyTwo?:return,
            if(toLock)byteArrayOf(0x00)else byteArrayOf(0x01))
        requireActivity().runOnUiThread {
            showLog("\napp writeD7: ${notify_characteristic?.value}")}
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
        mQRcode = mSharedPreferences.getString(MY_LOCK_QRCODE, "")
        println("store QR: ${mQRcode}")
        mPermanentToken = mSharedPreferences.getString(MainActivity.MY_LOCK_TOKEN, "")
        if(mQRcode.isNullOrBlank())return
        requireActivity().runOnUiThread {
            oneLockViewModel.decryptQRcode(mQRcode?:return@runOnUiThread){
                tv_my_lock_mac.setText(oneLockViewModel.mLockConnectionInfo.value?.macAddress)
                tv_my_lock_tk.setText(mSharedPreferences.getString(MainActivity.MY_LOCK_TOKEN, ""))
            }
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

    private fun claenSP(){
        mSharedPreferences = requireActivity().getSharedPreferences(DATA, 0)
        mSharedPreferences.edit()
            .putString(MY_LOCK_QRCODE, "")
            .putString(MainActivity.MY_LOCK_TOKEN, "")
            .commit()
        tv_my_lock_mac.setText("")
        tv_my_lock_tk.setText("")
    }

}