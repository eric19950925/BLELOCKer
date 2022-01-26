package com.example.blelocker.View.AddLock

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.blelocker.BaseFragment
import com.example.blelocker.BluetoothUtils.BleControlViewModel
import com.example.blelocker.MainActivity
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_location_summary.*
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class LocationSummaryFragment: BaseFragment(){
    private val bleControlViewModel by sharedViewModel<BleControlViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_location_summary

    override fun onViewHasCreated() {
        btn_complete.setOnClickListener {

            //作左右判定 todo
            (requireActivity() as MainActivity).backToHome()
        }
    }

    override fun onBackPressed() {
    }

    override fun onStop() {
        super.onStop()
        Log.d("TAG","onStop")
        //中斷連線
        viewLifecycleOwner.lifecycleScope.launch {
            bleControlViewModel.mLockBleStatus.value = null
            bleControlViewModel.disposeConnection()
            bleControlViewModel.disposeState()
        }
    }
}