package com.example.blelocker.View.Settings.AutoUnLock

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.example.blelocker.*
import com.example.blelocker.BluetoothUtils.BleControlViewModel
//import kotlinx.android.synthetic.main.fragment_auto_unlock.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class AutoUnLockFragment: BaseFragment(){
    override fun getLayoutRes(): Int = R.layout.fragment_auto_unlock
    private lateinit var mSharedPreferences: SharedPreferences
    private val geofenceViewModel by viewModel<GeofencingViewModel>()
    private val bleViewModel by sharedViewModel<BleControlViewModel>()
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        return null
    }
    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewHasCreated() {
/*
        switch_geofence.isChecked = readGeoSP()

        switch_geofence.setOnTouchListener { _, _ ->
            switch_geofence.isClickable = false
            if(!switch_geofence.isChecked){

                geofenceViewModel.setGeofencing(
                    success = {
                        switch_geofence.isClickable = true
                        switch_geofence.isChecked = true
                        saveGeoSP(true)
                        Toast.makeText(context, "設定成功", Toast.LENGTH_LONG).show()
                    },
                    failure = {
                        Toast.makeText(context, "設定失敗", Toast.LENGTH_LONG).show()
                        if (it is ApiException){
                            when(it.statusCode){
                                1000 -> {
//                                    showNoGpsSignalDialog()
                                    Log.d("TAG","showNoGpsSignalDialog()")
                                }
                                1004 -> {
//                                    showPermissionDialog()
                                    Log.d("TAG","showPermissionDialog()")
                                }
                            }
                        }else{
                            Log.d("TAG","oops")
//                            dialogController.createDialog(DialogCondition(
//                                title = getString(R.string.oops),
//                                message = getString(R.string.operation_failed),
//                                center = PositiveButton(label = getString(R.string.pop_btn_ok))
//                            ))
                        }
                    }
                )
            }else {
                geofenceViewModel.removeGeofencing (
                    success = {
                        switch_geofence.isClickable = true
                        switch_geofence.isChecked = false
                        saveGeoSP(false)

                    },
                    failure = {
                    switch_geofence.isClickable = true
                    }
                )
            }
            false
        }
        */
    }

    override fun onBackPressed() {
        Navigation.findNavController(requireView()).navigate(R.id.back_to_home)
    }
    private fun readGeoSP(): Boolean{
        mSharedPreferences = requireActivity().getSharedPreferences(MainActivity.DATA, 0)
        return mSharedPreferences.getBoolean("GEO", false)

    }
    private fun saveGeoSP(value: Boolean) {
        mSharedPreferences = requireActivity().getSharedPreferences(MainActivity.DATA, 0)
        mSharedPreferences.edit()
            .putBoolean("GEO", value)
            .apply()
    }
}