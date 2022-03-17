package com.example.blelocker.View.AddLock

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.example.blelocker.BaseFragment
import com.example.blelocker.R
import com.example.blelocker.databinding.FragmentInstallationBinding

class InstallationFragment: BaseFragment(){
    override fun getLayoutRes(): Int? = null
    private lateinit var currentBinding: FragmentInstallationBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentInstallationBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        currentBinding.tvSkip.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_installationFragment_to_connectFragment)
        }
    }

    override fun onBackPressed() {
    }
}