package com.sunionrd.blelocker.View.AddLock

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.BluetoothUtils.BleControlViewModel
import com.sunionrd.blelocker.MainActivity
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.databinding.FragmentLocationSummaryBinding
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class LocationSummaryFragment: BaseFragment(){
    private val bleControlViewModel by sharedViewModel<BleControlViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_location_summary
    private lateinit var currentBinding: FragmentLocationSummaryBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentLocationSummaryBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        currentBinding.btnComplete.setOnClickListener {

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