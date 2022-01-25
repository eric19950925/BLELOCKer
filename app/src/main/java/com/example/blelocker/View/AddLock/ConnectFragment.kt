package com.example.blelocker.View.AddLock

import android.util.Base64
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.blelocker.BaseFragment
import com.example.blelocker.BluetoothUtils.BleControlViewModel
import com.example.blelocker.Entity.BleStatus
import com.example.blelocker.Entity.DeviceToken
import com.example.blelocker.OneLockViewModel
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_connect.*
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ConnectFragment: BaseFragment(){
    private val bleControlViewModel by sharedViewModel<BleControlViewModel>()
    private val oneLockViewModel by sharedViewModel<OneLockViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_connect

    override fun onViewHasCreated() {
        bleControlViewModel.mCharacteristicValue.observe(this) {
            when(it.first){
                "OneTimeToken" -> {
                    val pair = it.second as Pair<ByteArray, DeviceToken>
                    //update lock data with pair
                    oneLockViewModel.mLockConnectionInfo.value?.let { lock ->
                        oneLockViewModel.updateLockConnectInformation(
                            lock.copy(
                                keyTwo = Base64.encodeToString(
                                    pair.first,
                                    Base64.DEFAULT),
                                permission = (pair.second as DeviceToken.PermanentToken).permission,
                                permanentToken = (pair.second as DeviceToken.PermanentToken).token
                            )
                        )
                    }
                    Log.d("TAG","connect success")
                }
            }
        }

        bleControlViewModel.mLockBleStatus.observe(this) {
            btn_connect.isClickable = true
            btn_connect.text = if(it == BleStatus.CONNECT)"Connect" else "Scan"
        }

        btn_connect.setOnClickListener {
            btn_connect.isClickable = false
            if(bleControlViewModel.mLockBleStatus.value == BleStatus.CONNECT){
                toSelectWiFiPage()
            }else{
                rxConnect()
            }

        }
    }

    private fun toSelectWiFiPage() {
        Navigation.findNavController(requireView()).navigate(R.id.action_connectFragment_to_selectWifiFragment)
    }

    private fun rxConnect() {
        bleControlViewModel.rxBleConnectWithLock(
            oneLockViewModel.mLockConnectionInfo.value?:return,
            success = {
                updateCurrentMacAddress()
            },
            failure = {
                Log.d("TAG",it.toString())
                viewLifecycleOwner.lifecycleScope.launch{ //todo
                    bleControlViewModel.mGattStatus.value = false
                }
            }
        )
    }

    private fun updateCurrentMacAddress() = viewLifecycleOwner.lifecycleScope.launch{
        bleControlViewModel.mMacAddress.value = oneLockViewModel.mLockConnectionInfo.value?.macAddress
    }

    override fun onBackPressed() {
    }

}