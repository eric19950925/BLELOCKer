package com.example.blelocker.View.AddLock

import android.content.DialogInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.example.blelocker.BaseFragment
import com.example.blelocker.BluetoothUtils.BleControlViewModel
import com.example.blelocker.Entity.BleStatus
import com.example.blelocker.R
import com.example.blelocker.databinding.FragmentAddAdminCodeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class AddAdminCodeFragment: BaseFragment(){
    private val bleControlViewModel by sharedViewModel<BleControlViewModel>()
    private val addAdminCodeViewModel by sharedViewModel<AddAdminCodeViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_add_admin_code
    private lateinit var currentBinding: FragmentAddAdminCodeBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentAddAdminCodeBinding.inflate(inflater, container, false)
        return currentBinding
    }

    override fun onViewHasCreated() {
        addAdminCodeViewModel.getLockInfo(bleControlViewModel.mMacAddress.value?:return)

        currentBinding.btnNext.setOnClickListener {
            addAdminCode()
        }
        addAdminCodeViewModel.adminCodeSet.observe(this){
            if(it){
                updateAdminCodeToApp()
                Navigation.findNavController(requireView()).navigate(R.id.action_addAdminCodeFragment_to_locationIntroFragment)
            }
        }
        bleControlViewModel.mLockBleStatus.observe(this){
            //if disconnect need to take user to set admin code todo
            //or click next to reconnect and set admin code
            if(bleControlViewModel.mLockBleStatus.value == BleStatus.UNCONNECT){
                MaterialAlertDialogBuilder(requireActivity(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                    .setTitle("Disconnect")
                    .setCancelable(true)
                    .setPositiveButton("reconnect") { _: DialogInterface, _: Int ->
                        bleControlViewModel.rxBleConnectWithLock(
                            addAdminCodeViewModel.mLock?:return@setPositiveButton,
                            success = {
//                        addAdminCode()
                            },
                            failure = {
                                Log.d("TAG",it.toString())
                            }
                        )
                    }.show()

            }
        }
    }

    private fun updateAdminCodeToApp() = viewLifecycleOwner.lifecycleScope.launch{
        addAdminCodeViewModel.updateLockAdminCode(currentBinding.etAdminCode.text.toString())
    }

    private fun addAdminCode()/* = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO)*/{
        if(currentBinding.etAdminCode.text.toString() == ""){
            //show hint
            return
        }

        addAdminCodeViewModel.addAdminCode(
            currentBinding.etAdminCode.text.toString(),
            bleControlViewModel.mRxBleConnection.value?:return
        )
    }

    override fun onBackPressed() {
    }

    override fun onDestroy() {
        super.onDestroy()
        addAdminCodeViewModel.disposeConnection()
    }

    override fun onStop() {
        super.onStop()
        addAdminCodeViewModel.clearConnection()
    }
}