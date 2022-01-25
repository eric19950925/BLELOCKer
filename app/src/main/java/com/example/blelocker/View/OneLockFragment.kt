package com.example.blelocker.View

import android.animation.ValueAnimator
import android.bluetooth.*
import android.content.DialogInterface
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.blelocker.*
import com.example.blelocker.BluetoothUtils.BleControlViewModel
import com.example.blelocker.Entity.AdminCodeDialog
import com.example.blelocker.Entity.BleStatus
import com.example.blelocker.MainActivity.Companion.DATA
import com.example.blelocker.Entity.DeviceToken
import com.example.blelocker.Entity.LockStatus.LOCKED
import com.example.blelocker.Entity.LockStatus.UNLOCKED
import com.example.blelocker.MainActivity.Companion.CURRENT_LOCK_MAC
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_onelock.*
import kotlinx.android.synthetic.main.fragment_onelock.log_tv
import kotlinx.android.synthetic.main.fragment_onelock.my_toolbar
import kotlinx.android.synthetic.main.fragment_setting.*
import kotlinx.coroutines.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.IOException
import java.lang.Exception
import java.util.*

class OneLockFragment: BaseFragment() {
    override fun getLayoutRes(): Int = R.layout.fragment_onelock
    val oneLockViewModel by sharedViewModel<OneLockViewModel>()
    val bleViewModel by sharedViewModel<BleControlViewModel>()

    private lateinit var mSharedPreferences: SharedPreferences
    private var mAdminCode: String? = null
    private var testScope: Job? = null
    private var dialogScope: Job? = null
    private var disconnectDialog: AlertDialog? = null

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

        dialogScope = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            try{ }finally {
                disconnectDialog?.dismiss()
            }
        }
        disconnectDialog = MaterialAlertDialogBuilder(requireActivity(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Disconnect")
            .setCancelable(true)
            .setPositiveButton("reconnect") { _: DialogInterface, _: Int ->
                bleViewModel.CloseGattScope()
                oneLockViewModel.mLockConnectionInfo.value?.let {
                    bleViewModel.bleScan(it.macAddress, it.keyOne)
                }
                dialogScope?.cancel()
            }.create()


        oneLockViewModel.mLockConnectionInfo.observe(viewLifecycleOwner){
            tv_my_lock_mac.text = oneLockViewModel.mLockConnectionInfo.value?.macAddress
            tv_my_lock_tk.text = oneLockViewModel.mLockConnectionInfo.value?.permanentToken
        }

        //observe the blue tooth connect status to update ui
        bleViewModel.mLockBleStatus.observe(viewLifecycleOwner){
            when(it){
                BleStatus.UNCONNECT -> {
                    iv_my_lock_ble_status.visibility = View.VISIBLE
                    btn_lock.clearAnimation()
                    btn_lock.visibility = View.GONE
                    ll_panel.visibility = View.GONE
                    iv_factory.visibility = View.GONE
                    bleViewModel.mLockSetting.value = null
                    //show disconnect dialog to help reconnect.
                    disconnectDialog?.show()
                }

                BleStatus.CONNECTTING -> {
                    iv_my_lock_ble_status.visibility = View.GONE
                    btn_lock.visibility = View.VISIBLE
                    updateUIbySetting()
                    disconnectDialog?.cancel()
                }
                //back to this page from settings
                BleStatus.CONNECT -> {
                    iv_my_lock_ble_status.visibility = View.GONE
                    btn_lock.visibility = View.VISIBLE
                    updateUIbySetting()
                    disconnectDialog?.cancel()
                }
                else -> {
                    iv_my_lock_ble_status.visibility = View.VISIBLE
                    btn_lock.clearAnimation()
                    btn_lock.visibility = View.GONE
                    ll_panel.visibility = View.GONE
                    iv_factory.visibility = View.GONE
                    bleViewModel.mLockSetting.value = null
                }
            }

        }

        //update log textview
        bleViewModel.mLogText.observe(viewLifecycleOwner){
            showLog(it)
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
                "C1" -> {
                    val isLockFromSharing = oneLockViewModel.mLockConnectionInfo.value?.sharedFrom != null && oneLockViewModel.mLockConnectionInfo.value?.sharedFrom?.isNotBlank() ?: false
                    val deviceToken = oneLockViewModel.determineTokenState(it.second as ByteArray, isLockFromSharing)
                    val permission = oneLockViewModel.determineTokenPermission(it.second as ByteArray)
                    Log.d("TAG", "C1 notify token state : ${(it.second as ByteArray).toHex()}")
                    Log.d("TAG", "token permission: $permission")
                    oneLockViewModel.mLockConnectionInfo.value?.let {
                        oneLockViewModel.updateLockConnectInformation(
                            it.copy(
                                permission = permission
                            )
                        )
                    }

                }
                "C7" -> {
                    try {
                        //update the cloud db by calling api
                        oneLockViewModel.updateLockAdminCode(checkNotNull(mAdminCode))
                    } catch (e:Exception) { Log.d("TAG",e.toString()) }
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
                    val mToken = it.second as DeviceToken.PermanentToken
                    oneLockViewModel.updateLockPermanentToken(mToken)
                    requireActivity().runOnUiThread {showLog("\nE5 notify Got PermanentToken: ${mToken.name} ${mToken.permission}.")}
                    if (mToken.isOwner){
                        launchAdminCodeDialog(AdminCodeDialog.INSERT)
                    }
                }
            }
        }

        //click and go to scan page
        iv_my_lock_ble_status.setOnClickListener {
            checkToStartBleScan()
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
            launchAdminCodeDialog(AdminCodeDialog.FACTORY_RESET)
        }

        btn_setting.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_onelock_to_setting)
        }
//        btn_admin_code.setOnClickListener {
//            launchAdminCodeDialog(AdminCodeDialog.INSERT)
//        }

    }

    private fun launchAdminCodeDialog(onNext: Int) {
        val editText = EditText(requireActivity())
        val dialog = MaterialAlertDialogBuilder(
            requireActivity(),
            R.style.ThemeOverlay_App_MaterialAlertDialog
        )
            .setCancelable(false)
            .setTitle("Enter your Admin code:")
            .setView(editText)
            .setPositiveButton("confirm") { _: DialogInterface, _: Int ->
                when(onNext){
                    AdminCodeDialog.INSERT ->{
                        bleViewModel.sendC7(editText.text.toString())
                        mAdminCode = editText.text.toString()
                    }
                    AdminCodeDialog.UPDATE ->{
                        bleViewModel.sendC8(editText.text.toString())
                        mAdminCode = editText.text.toString()
                    }
                    AdminCodeDialog.FACTORY_RESET ->{
                        if(editText.text.toString() == oneLockViewModel.mLockConnectionInfo.value?.adminCode) {
                            bleViewModel.sendCE(editText.text.toString())
                        }
                        else launchErrorDialog()
                    }
                }
            }
//            .setNegativeButton(getString(R.string.global_cancel)) { _: DialogInterface, _: Int ->
//            }
            .create()

        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    private fun launchErrorDialog() {
        val code = oneLockViewModel.mLockConnectionInfo.value?.adminCode
        MaterialAlertDialogBuilder(
            requireActivity(),
            R.style.ThemeOverlay_App_MaterialAlertDialog
        )
            .setCancelable(false)
            .setTitle("Error")
//            .setMessage("admin code is $code")
            .setPositiveButton("confirm") { _: DialogInterface, _: Int ->

            }
//            .setNegativeButton(getString(R.string.global_cancel)) { _: DialogInterface, _: Int ->
//            }
            .show()
    }

    private fun checkToStartBleScan() {
//        if(checkPermissions()!=true)return
//        if(checkBTenable()!=true)return
        if(oneLockViewModel.mLockConnectionInfo.value == null)return
        bleScan()
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
        oneLockViewModel.mLockConnectionInfo.value?.let {
            bleViewModel.bleScan(it.macAddress, it.keyOne)
        }

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
        bleViewModel.mLockBleStatus.value = null
        bleViewModel.CloseBleScanScope()
        Navigation.findNavController(requireView()).navigate(R.id.action_onelock_to_all)
    }

    override fun onResume() {
        super.onResume()
        cleanLog()
        //get lock info by macAddress
        //if use the bundle to get data, can only get data when enter this page from all lock page.
        //want to get data every time launch this page by resume(ex:wake from sleep), should store data in sp.
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            oneLockViewModel.getLockInfo(readCurrentLockMac()?:return@launch)
            if(bleViewModel.mLockBleStatus.value == BleStatus.CONNECT)return@launch
            delay(100)
            oneLockViewModel.mLockConnectionInfo.value?.let {
                bleViewModel.bleScan(it.macAddress, it.keyOne)
            }
        }
        Log.d("TAG","onResume")
    }

    private fun readCurrentLockMac(): String?{
        mSharedPreferences = requireActivity().getSharedPreferences(DATA, 0)
        return mSharedPreferences.getString(CURRENT_LOCK_MAC, "")

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
    private fun saveCurrentLockMac(macAddress: String) {
        mSharedPreferences = requireActivity().getSharedPreferences(DATA, 0)
        mSharedPreferences.edit()
            .putString(CURRENT_LOCK_MAC, macAddress)
            .apply()
    }

    private fun claenLockMac(){
        mSharedPreferences = requireActivity().getSharedPreferences(DATA, 0)
        mSharedPreferences.edit()
            .putString(CURRENT_LOCK_MAC, "")
            .apply()
    }

}