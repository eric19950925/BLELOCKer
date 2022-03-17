package com.example.blelocker.View.AddLock

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.example.blelocker.BaseFragment
import com.example.blelocker.R
import com.example.blelocker.databinding.FragmentPermissionRequestBinding

class PermissionRequestFragment: BaseFragment(){
    companion object {
        const val REQUEST_CODE = 555
        const val PROGRESS = "1 / 5"
    }
    override fun getLayoutRes(): Int? = null

    private lateinit var currentBinding: FragmentPermissionRequestBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentPermissionRequestBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        currentBinding.btnScan.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_permissionRequestFragment_to_scanFragment)
        }
    }

    override fun onBackPressed() {
    }
}