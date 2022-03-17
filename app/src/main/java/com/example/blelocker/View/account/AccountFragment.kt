package com.example.blelocker.View.account

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.blelocker.BaseFragment
import com.example.blelocker.R
//import kotlinx.android.synthetic.main.fragment_all_locks.*
//import kotlinx.android.synthetic.main.fragment_all_locks.my_toolbar
//import kotlinx.android.synthetic.main.fragment_onelock.*

class AccountFragment: BaseFragment(){
    override fun getLayoutRes(): Int = R.layout.fragment_account
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        return null
    }
    override fun onViewHasCreated() {
        setHasOptionsMenu(true)
//        my_toolbar.menu.clear()
//        my_toolbar.title = "Account"
//        my_toolbar.setNavigationOnClickListener {
//            val fragmentManager = this.findNavController()
//            fragmentManager.popBackStack()
//        }
    }

    override fun onBackPressed() {

    }
}