package com.example.blelocker.View

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.lifecycle.coroutineScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.example.blelocker.BaseFragment
import com.example.blelocker.BluetoothUtils.BleControlViewModel
import com.example.blelocker.Entity.BleStatus
import com.example.blelocker.Entity.LockConfig
import com.example.blelocker.Entity.LockSetting
import com.example.blelocker.MainActivity
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_setting.*
import kotlinx.android.synthetic.main.fragment_setting.log_tv
import kotlinx.android.synthetic.main.fragment_setting.my_toolbar
import kotlinx.coroutines.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.IOException

class SettingFragment: BaseFragment() {
    val bleViewModel by sharedViewModel<BleControlViewModel>()
    override fun getLayoutRes(): Int  = R.layout.fragment_setting
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewHasCreated() {
        //set top menu
        setHasOptionsMenu(true)
        my_toolbar.inflateMenu(R.menu.my_menu)
        my_toolbar.title = "Setting"
        my_toolbar.menu.findItem(R.id.scan).isVisible = false
        my_toolbar.menu.findItem(R.id.play).isVisible = false
        my_toolbar.menu.findItem(R.id.github).isVisible = false
        my_toolbar.menu.findItem(R.id.delete).isVisible = false

        //set log textview
        log_tv.movementMethod = ScrollingMovementMethod.getInstance()
        log_tv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                while (log_tv.canScrollVertically(1)) {
                    log_tv.scrollBy(0, 10)
                }
            }
        })
        bleViewModel.getLockStatus()

//        switch_auto_lock.setOnCheckedChangeListener will get some bug
        switch_auto_lock.setOnTouchListener { v, event ->
            switch_auto_lock.isClickable = false
            runBlocking(Dispatchers.Default){
                bleViewModel.mLockSetting.value?.config?.let {
                    val new_setting = LockConfig(
                        orientation = it.orientation ,
                        isSoundOn = it.isSoundOn,
                        isVacationModeOn = it.isVacationModeOn,
                        isAutoLock = !switch_auto_lock.isChecked,
                        autoLockTime = it.autoLockTime
                    )
                    bleViewModel.setAutoLock(new_setting)
                }
            }
            false
        }

        bleViewModel.mCharacteristicValue.observe(viewLifecycleOwner){
            when(it.first) {
                "C7" -> {
//                    oneLockViewModel.updateLockAdminCode("0000")
                }
                "CE" -> {

                }
                "D4" -> {
                    requireActivity().runOnUiThread {

                        val mCfg = it.second as LockConfig

                        switch_auto_lock.isChecked = mCfg.isAutoLock
                    }

                }
                "D5" -> {

                }
                "D6" -> {
                    bleViewModel.mLockSetting.value = it.second as LockSetting
                    switch_auto_lock.isClickable = true
                }
            }
        }

        bleViewModel.mLockSetting.observe(viewLifecycleOwner){
            switch_auto_lock.isChecked = it.config.isAutoLock
        }

        //update log textview
        bleViewModel.mLogText.observe(viewLifecycleOwner){
            showLog(it)
        }

        bleViewModel.mLockBleStatus.observe(viewLifecycleOwner) {
            if(it == BleStatus.UNCONNECT){
                (activity as MainActivity).launchDisconnectedDialog()
            }
        }

    }
    private fun showLog(logText: String) {
        try {
            var log = log_tv.text.toString()

            log = log +"${logText}\n"

            log_tv.text = log
        } catch (e: IOException) {
        }
    }
    override fun onBackPressed() {
        Navigation.findNavController(requireView()).navigate(R.id.action_back_to_onelock)
    }
}