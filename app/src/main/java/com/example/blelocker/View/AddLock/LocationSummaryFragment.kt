package com.example.blelocker.View.AddLock

import com.example.blelocker.BaseFragment
import com.example.blelocker.MainActivity
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_location_summary.*

class LocationSummaryFragment: BaseFragment(){
    override fun getLayoutRes(): Int = R.layout.fragment_location_summary

    override fun onViewHasCreated() {
        btn_complete.setOnClickListener {
            (requireActivity() as MainActivity).backToHome()
        }
    }

    override fun onBackPressed() {
    }
}