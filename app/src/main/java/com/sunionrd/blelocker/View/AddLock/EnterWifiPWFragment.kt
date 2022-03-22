package com.sunionrd.blelocker.View.AddLock

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.BluetoothUtils.BleControlViewModel
import com.sunionrd.blelocker.OneLockViewModel
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.databinding.FragmentEnterWifiPasswordBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class EnterWifiPWFragment: BaseFragment(){
    private val bleControlView by sharedViewModel<BleControlViewModel>()
    private val oneLockViewModel by sharedViewModel<OneLockViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_enter_wifi_password
    private lateinit var currentBinding: FragmentEnterWifiPasswordBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentEnterWifiPasswordBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        arguments?.getString("WIFI_NAME").let {
            currentBinding.tvWifiName.text = it
        }
        currentBinding.btnConnectToWifi.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_enterWifiPWFragment_to_addAdminCodeFragment)
        }
    }

    override fun onBackPressed() {
    }
}