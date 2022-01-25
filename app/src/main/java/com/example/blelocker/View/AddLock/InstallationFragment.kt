package com.example.blelocker.View.AddLock

import androidx.navigation.Navigation
import com.example.blelocker.BaseFragment
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_installation.*

class InstallationFragment: BaseFragment(){
    override fun getLayoutRes(): Int = R.layout.fragment_installation

    override fun onViewHasCreated() {
        tv_skip.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_installationFragment_to_connectFragment)
        }
    }

    override fun onBackPressed() {
    }
}