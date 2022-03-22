package com.sunionrd.blelocker.View

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.sunionrd.blelocker.*
import com.sunionrd.blelocker.BluetoothUtils.BleControlViewModel
import com.sunionrd.blelocker.CognitoUtils.CognitoControlViewModel
import com.sunionrd.blelocker.CognitoUtils.LogOutRequest.*
import com.sunionrd.blelocker.Entity.*
import com.sunionrd.blelocker.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class HomeFragment : BaseFragment(){
    private val tfhApiViewModel by viewModel<TFHApiViewModel>()
    private val oneLockViewModel by viewModel<OneLockViewModel>()
    private val homeViewModel by viewModel<HomeViewModel>()

    private val bleViewModel by sharedViewModel<BleControlViewModel>()
    private val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private lateinit var mSharedPreferences: SharedPreferences
    var count = 0
    private var loadingScope: Job? = null
    private lateinit var currentBinding: FragmentHomeBinding
    override fun getLayoutRes(): Int? = null

    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return currentBinding
    }

    override fun onViewHasCreated() {

        setHasOptionsMenu(true)
        currentBinding.myToolbar.menu.clear()
        currentBinding.myToolbar.inflateMenu(R.menu.my_menu)
        currentBinding.myToolbar.menu.findItem(R.id.github).isVisible = true
        currentBinding.myToolbar.menu.findItem(R.id.play).isVisible = false
        currentBinding.myToolbar.menu.findItem(R.id.delete).isVisible = false
        currentBinding.myToolbar.setNavigationIcon(R.drawable.ic_baseline_menu_24)
        currentBinding.myToolbar.title = ""

        val adapter = HomeLockAdapter(
            onClickListener = setupLockNameClick(),
            onLockStatusClickListener = setupLockStatusClick(),
            onSettingClickListener = setupSettingClick())

        currentBinding.recyclerview.adapter = adapter
        currentBinding.recyclerview.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)

        currentBinding.myToolbar.setNavigationOnClickListener {
            setupMenuNavigationIcon()
        }

        currentBinding.myToolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.scan -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_alllocks_to_scan)
                    true
                }
                R.id.github -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_home_Fragment_to_account_Fragment)
                    true
                }
                R.id.delete -> {
                    oneLockViewModel.deleteLocks()
                    true
                }
                else -> false
            }
        }
        homeViewModel.mHomeLocksData.observe(this) { locks -> //todo: this adapter should not use this data structure(LockConnectInformation)
            locks.let { adapter.submitList(it) }
            count = locks.size
            if(count > 8){
                currentBinding.myToolbar.menu.findItem(R.id.scan).isVisible = false
            }
        }

        oneLockViewModel.isPowerOn.observe(this){ power ->
            currentBinding.tvPower.text = if(power)"On" else "Off"
            loadingScope?.cancel()
        }

        cognitoViewModel.mMqttStatus.observe(this){ status ->
            when(status){
               MqttStatus.UNCONNECT -> {
                   currentBinding.btnSubpubUpdate.isClickable = false
               }
               MqttStatus.CONNECTED -> {
                   getDeviceShadow()
               }
               MqttStatus.CONNECTTING -> {
                   currentBinding.btnSubpubUpdate.isClickable = false
               }
               MqttStatus.CONNECTION_LOST -> {
                   currentBinding.btnSubpubUpdate.isClickable = false
               }
               MqttStatus.RECONNECTING -> {
                   currentBinding.btnSubpubUpdate.isClickable = false
               }
            }
        }

        currentBinding.btnAutoUnlock.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_alllocks_to_autolock)
        }

        currentBinding.btnSPGetTime.setOnClickListener {
            setupGetTimeClick()
        }

        currentBinding.btnSubpubUpdate.setOnClickListener {
            currentBinding.btnSubpubUpdate.isClickable = false
            try{
                tfhApiViewModel.subPubUpdateClassicShadow(
                    cognitoViewModel.mqttManager?:return@setOnClickListener,
                    oneLockViewModel.isPowerOn.value?:return@setOnClickListener
                ){
                    updateShadowPower(it)
                    currentBinding.btnSubpubUpdate.isClickable = true
                    //正確顯示狀態後才能開始操作
                }
            }catch (e : Exception){Log.d("TAG",e.toString())}

        }

        setupBackPressedCallback()
    }

    private fun setupGetTimeClick() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
            cognitoViewModel.getUserInfo{
                tfhApiViewModel.subPubGetTime(
                    mqttManager = cognitoViewModel.mqttManager,
                    cognitoViewModel.mIdentityPoolId.value?:return@getUserInfo,
                    cognitoViewModel.mJwtToken.value?:return@getUserInfo)
                { response ->
                    Log.d("TAG", "response: $response")
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        currentBinding.tvTime.text = response
                    }
//                    when(response){
//                        "AAA" -> {
//                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
//                                currentBinding.tvTime.text = response
//                            }
//                        }
//                        else -> {
//                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
//                                Log.d("TAG",response)
//                            }
//                        }
//                    }
                }
            }
        }
    }

    private fun setupBackPressedCallback() {
        //需中斷mqtt連線
        //刪除DB-cognito
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    closeMqttConnection()
                    requireActivity().finish()
                }
            }
        )
    }

    private fun setupMenuNavigationIcon() {
        (requireActivity() as MainActivity).controlDrawer()
    }

    private fun initHomeLocksData() {
        val list = mutableListOf<HomeLocks>()
        oneLockViewModel.allLocks.observe(this){
            it.forEach {
                list.add(
                    HomeLocks(
                        macAddress = it.macAddress,
                        deviceName = it.deviceName,
                        lockStatus = LockStatus.UNKNOWN
                    )
                )
            }
            homeViewModel.mHomeLocksData.value = list
        }
    }


    private fun setupSettingClick() = HomeLockAdapter.OnClickListener {
        if(!checkPermissions())return@OnClickListener
        if(!checkBTenable())return@OnClickListener
//            val bundle = Bundle()
//            bundle.putString("MAC_ADDRESS", it)
        saveCurrentLockMac(it)
//            Navigation.findNavController(requireView()).navigate(R.id.action_to_onelock/*,bundle*/)
    }

    private fun setupLockStatusClick() = HomeLockAdapter.OnClickListener { macAddress ->

        oneLockViewModel.getLockInfo(macAddress){
            when(homeViewModel.mHomeLocksData.value?.find { it.macAddress == macAddress }?.lockStatus){
                //確認是否連線，若無則連線，若有則往下做 todo
                LockStatus.UNKNOWN -> {
//                oneLockViewModel.getLockInfo()
                    bleViewModel.rxBleConnectWithLock(
                        oneLockViewModel.mLockConnectionInfo.value?:return@getLockInfo,
                        success = {
                            //get D6
                            homeViewModel.getLockSetting(
                                oneLockViewModel.mLockConnectionInfo.value?:return@rxBleConnectWithLock,
                                bleViewModel.mRxBleConnection.value?:return@rxBleConnectWithLock
                            )
                        },
                        failure = {
                            Log.d("TAG","failure to connect: $it")
                        }
                    )
                }
                //連線後會收到D6，即會更新鎖體狀態，此處用於反面設定 todo
                LockStatus.LOCKED, LockStatus.UNLOCKED -> {
                    homeViewModel.controlLockStatus(
                        homeViewModel.mLockSetting.value?.status?:0,
                        oneLockViewModel.mLockConnectionInfo.value?:return@getLockInfo,
                        bleViewModel.mRxBleConnection.value?:return@getLockInfo
                    )
                }
                else -> {
                    Log.d("TAG","ble status is null")
                }
            }
        }

    }

    private fun setupLockNameClick() = HomeLockAdapter.OnClickListener {

    }

    private fun updateShadowPower(power: Boolean) = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
        oneLockViewModel.isPowerOn.value = power
    }

    private fun saveCurrentLockMac(macAddress: String) {
        mSharedPreferences = requireActivity().getSharedPreferences(MainActivity.DATA, 0)
        mSharedPreferences.edit()
            .putString(MainActivity.CURRENT_LOCK_MAC, macAddress)
            .apply()
    }

    override fun onBackPressed() {
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
    private fun checkBTenable(): Boolean {
        val mBluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothManager.adapter?.takeIf { !it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 124)
        }
        return mBluetoothManager.adapter?.isEnabled?:false
    }

    private fun closeMqttConnection() {
//        if(cognitoViewModel.mMqttStatus.value != MqttStatus.CONNECTED)return
//        tfhApiViewModel.unSubscribeGetTopic(cognitoViewModel.mqttManager?:return){
            cognitoViewModel.mqttDisconnect()
            cognitoViewModel.closeCognitoCache()
//        }
    }

    override fun onResume() {
        super.onResume()
        //check if been global sign out
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            currentBinding.tvUserId.text = cognitoViewModel.mUserID.value
            (requireActivity() as MainActivity).setUserName(cognitoViewModel.mUserID.value?:"")
            (requireActivity() as MainActivity).setPaletteColor(R.drawable.ic_icons8_github)
        }
        Log.d("TAG","getUserStatus")
        //loading... todo
        loadingScope = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            (requireActivity() as MainActivity).showLoadingView()
            try{
                var mTimestamp = 0
                while(mTimestamp < 60) {
                    delay(1000)
                    mTimestamp += 1
                }
            }finally {
                (requireActivity() as MainActivity).hideLoadingView()
            }
        }
        cognitoViewModel.getUserDetails(
            onFailure = {
                loadingScope?.cancel()
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                    val navHostFragment = (activity as MainActivity).supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment
                    navHostFragment.navController.navigate(R.id.action_to_login)
                }
            },
            onSuccess = {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
                    Log.d("TAG",it)
                    if(cognitoViewModel.mMqttStatus.value == null){
                        cognitoViewModel.initMQTTbyAWSIotCore()
                    }
                }
            }
        )
        //todo: prepare HomeLocks data
        initHomeLocksData()
    }

    private fun getDeviceShadow() {
        tfhApiViewModel.subPubGetClassicShadow(cognitoViewModel.mqttManager?:return){
            updateShadowPower(it)
            //正確顯示狀態後才能開始操作
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
                currentBinding.btnSubpubUpdate.isClickable = true
            }
        }
    }
}