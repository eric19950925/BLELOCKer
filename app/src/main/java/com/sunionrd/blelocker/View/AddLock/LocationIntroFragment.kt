package com.sunionrd.blelocker.View.AddLock

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.databinding.FragmentLocationIntroBinding

class LocationIntroFragment: BaseFragment(){
    override fun getLayoutRes(): Int = R.layout.fragment_location_intro
    private lateinit var currentBinding: FragmentLocationIntroBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentLocationIntroBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        currentBinding.tvSkip.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_locationIntroFragment_to_locationSummaryFragment)
        }
    }

    override fun onBackPressed() {
    }
}