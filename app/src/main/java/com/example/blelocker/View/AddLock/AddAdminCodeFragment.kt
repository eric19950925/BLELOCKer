package com.example.blelocker.View.AddLock

import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.blelocker.BaseFragment
import com.example.blelocker.BluetoothUtils.BleControlViewModel
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_add_admin_code.*
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class AddAdminCodeFragment: BaseFragment(){
    private val bleControlViewModel by sharedViewModel<BleControlViewModel>()
    private val addAdminCodeViewModel by sharedViewModel<AddAdminCodeViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_add_admin_code

    override fun onViewHasCreated() {
        addAdminCodeViewModel.getLockInfo(bleControlViewModel.mMacAddress.value?:return)

        btn_next.setOnClickListener {
            addAdminCode()
        }
        addAdminCodeViewModel.adminCodeSet.observe(this){
            if(it){
                updateAdminCodeToApp()
                Navigation.findNavController(requireView()).navigate(R.id.action_addAdminCodeFragment_to_locationIntroFragment)
            }
        }
    }

    private fun updateAdminCodeToApp() = viewLifecycleOwner.lifecycleScope.launch{
        addAdminCodeViewModel.updateLockAdminCode(et_admin_code.text.toString())
    }

    private fun addAdminCode()/* = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO)*/{
        addAdminCodeViewModel.addAdminCode(
            et_admin_code.text.toString(),
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