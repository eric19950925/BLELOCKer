package com.example.blelocker.View.AddLock

import androidx.navigation.Navigation
import com.example.blelocker.BaseFragment
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_permission_request.*

class PermissionRequestFragment: BaseFragment(){
    companion object {
        const val REQUEST_CODE = 555
        const val PROGRESS = "1 / 5"
    }
    override fun getLayoutRes(): Int = R.layout.fragment_permission_request

    override fun onViewHasCreated() {
        btn_scan.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_permissionRequestFragment_to_scanFragment)
        }
    }

    override fun onBackPressed() {
    }
}