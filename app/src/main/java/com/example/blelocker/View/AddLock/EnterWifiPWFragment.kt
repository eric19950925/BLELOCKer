package com.example.blelocker.View.AddLock

import androidx.navigation.Navigation
import com.example.blelocker.BaseFragment
import com.example.blelocker.BluetoothUtils.BleControlViewModel
import com.example.blelocker.OneLockViewModel
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_enter_wifi_password.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class EnterWifiPWFragment: BaseFragment(){
    private val bleControlView by sharedViewModel<BleControlViewModel>()
    private val oneLockViewModel by sharedViewModel<OneLockViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_enter_wifi_password

    override fun onViewHasCreated() {
        arguments?.getString("WIFI_NAME").let {
            tv_wifi_name.text = it
        }
        btn_connect_to_wifi.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_enterWifiPWFragment_to_addAdminCodeFragment)
        }
    }

    override fun onBackPressed() {
    }
}