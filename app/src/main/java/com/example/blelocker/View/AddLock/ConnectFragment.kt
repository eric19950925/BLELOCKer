package com.example.blelocker.View.AddLock

import android.content.DialogInterface
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.blelocker.BaseFragment
import com.example.blelocker.BluetoothUtils.BleControlViewModel
import com.example.blelocker.Entity.BleStatus
import com.example.blelocker.MainActivity
import com.example.blelocker.OneLockViewModel
import com.example.blelocker.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_connect.*
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ConnectFragment: BaseFragment(){
    private val bleControlViewModel by sharedViewModel<BleControlViewModel>()
    private val oneLockViewModel by sharedViewModel<OneLockViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_connect

    override fun onViewHasCreated() {

        bleControlViewModel.mLockBleStatus.observe(this) {
            btn_connect.isClickable = true
            btn_connect.text = if(it == BleStatus.CONNECT)"Connect" else "Start"
        }

        btn_connect.setOnClickListener {
            btn_connect.isClickable = false
            if(bleControlViewModel.mLockBleStatus.value == BleStatus.CONNECT){
                toSelectWiFiPage()
            }else{
                rxConnect()
            }

        }

        setupBackPressedCallback()
    }

    private fun toSelectWiFiPage() {
        Navigation.findNavController(requireView()).navigate(R.id.action_connectFragment_to_selectWifiFragment)
    }
    private fun setupBackPressedCallback() {
        //需中斷連線
        //刪除DB
        //Now app do not have to factory reset lock. Let user read the cook book them self.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    MaterialAlertDialogBuilder(
                        requireActivity(),
                        R.style.ThemeOverlay_App_MaterialAlertDialog
                    )
                        .setTitle("Cancel Setup Flow?")
//                        .setMessage(getString(R.string.location_setup_cancel_message))
                        .setCancelable(true)
                        .setNegativeButton("cancel") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                        }
                        .setPositiveButton("ok") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                            bleControlViewModel.disposeConnection()
                            oneLockViewModel.deleteOneLock(oneLockViewModel.mLockConnectionInfo.value?:return@setPositiveButton)
                            (requireActivity() as MainActivity).backToHome()
                        }
                        .show()
                }
            }
        )
    }
    private fun rxConnect() {
        bleControlViewModel.rxBleConnectWithLock(
            oneLockViewModel.mLockConnectionInfo.value?:return,
            success = {
                updateCurrentMacAddress()
            },
            failure = {
                Log.d("TAG",it.toString())
            }
        )
    }

    private fun updateCurrentMacAddress() = viewLifecycleOwner.lifecycleScope.launch{
        bleControlViewModel.mMacAddress.value = oneLockViewModel.mLockConnectionInfo.value?.macAddress
    }

    override fun onBackPressed() {
    }

}