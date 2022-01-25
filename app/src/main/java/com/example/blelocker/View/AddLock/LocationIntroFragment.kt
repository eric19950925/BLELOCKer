package com.example.blelocker.View.AddLock

import androidx.navigation.Navigation
import com.example.blelocker.BaseFragment
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_location_intro.*

class LocationIntroFragment: BaseFragment(){
    override fun getLayoutRes(): Int = R.layout.fragment_location_intro

    override fun onViewHasCreated() {
        tv_skip.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_locationIntroFragment_to_locationSummaryFragment)
        }
    }

    override fun onBackPressed() {
    }
}