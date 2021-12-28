package com.example.blelocker.View

import android.Manifest
import android.animation.ValueAnimator
import android.bluetooth.*
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.blelocker.*
import com.example.blelocker.BluetoothUtils.BleControlViewModel
import com.example.blelocker.Entity.BleStatus
import com.example.blelocker.MainActivity.Companion.DATA
import com.example.blelocker.MainActivity.Companion.MY_LOCK_QRCODE
import com.example.blelocker.Entity.DeviceToken
import com.example.blelocker.Entity.LockStatus.LOCKED
import com.example.blelocker.Entity.LockStatus.UNLOCKED
import kotlinx.android.synthetic.main.fragment_onelock.*
import kotlinx.android.synthetic.main.fragment_onelock.log_tv
import kotlinx.android.synthetic.main.fragment_onelock.my_toolbar
import kotlinx.android.synthetic.main.fragment_setting.*
import kotlinx.coroutines.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.IOException
import java.util.*

class OneLockFragment: BaseFragment() {
    override fun getLayoutRes(): Int = R.layout.fragment_onelock
    val oneLockViewModel by sharedViewModel<OneLockViewModel>()
    val bleViewModel by sharedViewModel<BleControlViewModel>()

    private lateinit var mSharedPreferences: SharedPreferences
    private var mBluetoothManager: BluetoothManager? = null
    var isLockFromSharing: Boolean? = null
    var testScope: Job? = null

    private val rotateAnimation = RotateAnimation(
        0f, 359f,
        Animation.RELATIVE_TO_SELF, 0.5f,
        Animation.RELATIVE_TO_SELF, 0.5f
    ).apply {
        this.duration = 2000
        this.repeatCount = ValueAnimator.INFINITE
    }

    override fun onViewHasCreated() {
        //set top menu
        setHasOptionsMenu(true)
        my_toolbar.inflateMenu(R.menu.my_menu)
        my_toolbar.menu.findItem(R.id.scan).isVisible = false
        my_toolbar.title = "BLE LOCKer"

        //set log textview
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

        oneLockViewModel.mLockConnectionInfo.observe(viewLifecycleOwner){
            tv_my_lock_mac.setText(oneLockViewModel.mLockConnectionInfo.value?.macAddress)
            tv_my_lock_tk.setText(oneLockViewModel.mLockConnectionInfo.value?.permanentToken)
        }

        //observe the blue tooth connect status to update ui
        bleViewModel.mLockBleStatus.observe(viewLifecycleOwner){
            when(it){
                BleStatus.UNCONNECT -> {
                    iv_my_lock_ble_status.visibility = View.VISIBLE
                    btn_lock.visibility = View.GONE
                    ll_panel.visibility = View.GONE
                    iv_factory.visibility = View.GONE
                    bleViewModel.mLockSetting.value = null
                }
                BleStatus.CONNECTTING -> {
                    iv_my_lock_ble_status.visibility = View.GONE
                    btn_lock.visibility = View.VISIBLE
                    updateUIbySetting()
                }
                //back to this page from settings
                BleStatus.CONNECT -> {
                    iv_my_lock_ble_status.visibility = View.GONE
                    btn_lock.visibility = View.VISIBLE
                    updateUIbySetting()
                }
            }

        }

        //update log textview
        bleViewModel.mLogText.observe(viewLifecycleOwner){
            showLog(it)
        }

        bleViewModel.mDescriptorValue.observe(viewLifecycleOwner){
            if(hadConn())return@observe
            if(it)bleViewModel.sendC0(oneLockViewModel.mLockConnectionInfo.value?.keyOne?:return@observe)
        }

        bleViewModel.mCharacteristicValue.observe(viewLifecycleOwner){
            when(it.first){
                "C0" -> {
//                    showLog("C0 notyfy ramNum2\nApp use it to generateKeyTwo and ready to send C1")
                    oneLockViewModel.mLockConnectionInfo.value?.permanentToken?.let { permanentToken ->
                        if(permanentToken.isBlank())bleViewModel.sendC1withOTToken(oneLockViewModel.mLockConnectionInfo.value?.oneTimeToken?:return@let)
                        else bleViewModel.sendC1(permanentToken)
                    }
                }
                "C7" -> {
                    oneLockViewModel.updateLockAdminCode("0000")
                }
                "CE" -> {
                    if(it.second == true){
                        requireActivity().runOnUiThread {
                            //clean data
                            oneLockViewModel.mLockConnectionInfo.value = null
                            //set ble scan btn not clickable
                            iv_my_lock_ble_status.isClickable = false
                            showLog("CE notify 重置成功")
                        }
                    }else {
                        requireActivity().runOnUiThread {showLog("CE notify 重置失敗")}
                    }
                }
                "D6" -> {

                }
                "E5" -> {
                    oneLockViewModel.updateLockPermanentToken((it.second as DeviceToken.PermanentToken).token)
                }
            }
        }

        //click and go to scan page
        iv_my_lock_ble_status.setOnClickListener {
            if(checkPermissions()!=true)return@setOnClickListener
            if(checkBTenable()!=true)return@setOnClickListener
            if(oneLockViewModel.mLockConnectionInfo.value == null)return@setOnClickListener
            bleScan()
        }

        //click to lock/unlock
        btn_lock.setOnClickListener {
            bleViewModel.sendD7(bleViewModel.mLockSetting.value?.status == LOCKED)
            btn_lock_wait()
        }

        //top menu function
        my_toolbar.setOnMenuItemClickListener {
            when(it.itemId){
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
            //factory reset
            bleViewModel.sendCE()
        }

        btn_setting.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_onelock_to_setting)
        }

    }

    private fun hadConn(): Boolean {
        return bleViewModel.mLockSetting.value != null
    }

    private fun updateUIbySetting() {
        bleViewModel.mLockSetting.value.let {
            if(it == null){
                btn_lock.setBackgroundResource(R.drawable.ic_loading_main)
                btn_lock.startAnimation(rotateAnimation)
                iv_factory.visibility = View.GONE
                ll_panel.visibility = View.GONE
                btn_lock_wait()
            }else{
                btn_lock.clearAnimation()
                iv_factory.visibility = View.VISIBLE
                ll_panel.visibility = View.VISIBLE
                btn_lock_ready()
                when(it.status){
                    LOCKED -> btn_lock.setBackgroundResource(R.drawable.ic_lock_main)
                    UNLOCKED -> btn_lock.setBackgroundResource(R.drawable.ic_auto_unlock)
                }
            }
        }
    }

    private fun btn_lock_wait() {
        btn_lock.isClickable = false
    }

    private fun btn_lock_ready() {
        btn_lock.isClickable = true
    }

    private fun bleScan() {
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
        bleViewModel.bleScan(oneLockViewModel.mLockConnectionInfo.value?.macAddress?:return)

    }

    override fun onPause() {
        Log.d("TAG","onPause")

        super.onPause()
    }

    override fun onStop() {
        Log.d("TAG","onStop")
        super.onStop()
    }

    override fun onDestroyView() {
        Log.d("TAG","onDestroyView")
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d("TAG","onDestroy")
        super.onDestroy()
    }

    override fun onDetach() {
        Log.d("TAG","onDetach")
        super.onDetach()
    }

    override fun onBackPressed() {
        Log.d("TAG","onBackPressed")
        bleViewModel.CloseGattScope()
        bleViewModel.CloseBleScanScope()
        Navigation.findNavController(requireView()).navigate(R.id.action_onelock_to_all)
    }

    override fun onResume() {
        super.onResume()
        cleanLog()
        //get lock info by macAddress
        getArguments()?.getString("MAC_ADDRESS").let {
            if(it.isNullOrBlank())return
            oneLockViewModel.getLockInfo(it)
        }
        //todo : auto ble conn

        Log.d("TAG","onResume")
    }

    private fun checkBTenable(): Boolean {
        mBluetoothManager = requireContext().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothManager?.adapter?.takeIf { !it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 124)
        }
        return mBluetoothManager?.adapter?.isEnabled?:false
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

    private fun readSharedPreference() {
        mSharedPreferences = requireActivity().getSharedPreferences(DATA, 0)
        val mQRcode = mSharedPreferences.getString(MY_LOCK_QRCODE, "")
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
            .putString(MY_LOCK_QRCODE, "mQRcode")
            .apply()
    }

    private fun claenSP(){
        mSharedPreferences = requireActivity().getSharedPreferences(DATA, 0)
        mSharedPreferences.edit()
            .putString(MY_LOCK_QRCODE, "")
            .putString(MainActivity.MY_LOCK_TOKEN, "")
            .apply()
        tv_my_lock_mac.setText("")
        tv_my_lock_tk.setText("")
    }

}