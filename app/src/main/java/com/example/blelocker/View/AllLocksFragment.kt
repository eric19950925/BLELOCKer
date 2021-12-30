package com.example.blelocker.View

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blelocker.BaseFragment
import com.example.blelocker.CognitoUtils.CognitoControlViewModel
import com.example.blelocker.CognitoUtils.LogOutRequest.*
import com.example.blelocker.MainActivity
import com.example.blelocker.OneLockViewModel
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_all_locks.*
import kotlinx.android.synthetic.main.fragment_all_locks.my_toolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class AllLocksFragment : BaseFragment(){
    val oneLockViewModel by viewModel<OneLockViewModel>()
    val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    var count = 0
    override fun getLayoutRes(): Int = R.layout.fragment_all_locks

    override fun onViewHasCreated() {
        setHasOptionsMenu(true)
        my_toolbar.menu.clear()
        my_toolbar.inflateMenu(R.menu.my_menu)
        my_toolbar.menu.findItem(R.id.github).isVisible = true
        my_toolbar.menu.findItem(R.id.play).isVisible = false
//        my_toolbar.menu.findItem(R.id.delete).isVisible = false
        my_toolbar.title = "All Locks"


        val adapter = AllLocksAdapter(AllLocksAdapter.OnClickListener{
            val bundle = Bundle()
            bundle.putString("MAC_ADDRESS", it)
            Navigation.findNavController(requireView()).navigate(R.id.action_to_onelock,bundle)
        })

        recyclerview.adapter = adapter
        recyclerview.layoutManager = LinearLayoutManager(requireContext())

        cognitoViewModel.getAccessToken{
            tv_user_id.text = cognitoViewModel.mUserID.value
            tv_user_access_token.text = cognitoViewModel.mAccessToken.value
        }

        my_toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.scan -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_alllocks_to_scan)
//                    var sample_lock = LockConnectionInformation(
//                        macAddress = "it.macAddress${count}",
//                        displayName = "it.displayName",
//                        keyOne = "it.keyOne",
//                        keyTwo = "it.keyTwo",
//                        oneTimeToken = "it.oneTimeToken",
//                        permanentToken = "it.permanentToken",
//                        isOwnerToken = true,
//                        tokenName = "T",
//                        sharedFrom = "it.sharedFrom",
//                        index = 0,
//                        adminCode = "0000"
//                    )
//                    oneLockViewModel.insertLock(sample_lock)
//                    count++
                    true
                }
                R.id.github -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_alllocks_to_github)
                    true
                }
                R.id.delete -> {
                    oneLockViewModel.deleteLocks()
                    true
                }
                else -> false
            }
        }
        oneLockViewModel.allLocks.observe(this) { locks ->
            locks.let { adapter.submitList(it) }
            count = locks.size
            if(count > 8){
                my_toolbar.menu.findItem(R.id.scan).isVisible = false
            }
        }
        btn_logout.setOnClickListener {
            logOut()
        }
    }

    private fun logOut()= viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
        cognitoViewModel.LogOut { LogOutRequest ->
            when(LogOutRequest){
                SUCCESS -> {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        val navHostFragment = (activity as MainActivity).supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment
                        navHostFragment.navController.navigate(R.id.action_to_login)
                    }
//                    Navigation.findNavController(requireView()).navigate(R.id.action_to_login)
//                    Toast.makeText(requireContext(), "Log out Success", Toast.LENGTH_LONG).show()
                }
                FAILURE -> {
                    Toast.makeText(requireContext(), "Log out Failure", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onBackPressed() {
//        logOut()
        requireActivity().finish()
    }
}