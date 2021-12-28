package com.example.blelocker.View

import android.os.Bundle
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blelocker.BaseFragment
import com.example.blelocker.OneLockViewModel
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_all_locks.*
import kotlinx.android.synthetic.main.fragment_all_locks.my_toolbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class AllLocksFragment : BaseFragment(){
    val oneLockViewModel by viewModel<OneLockViewModel>()
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
    }

    override fun onBackPressed() {
        requireActivity().finish()
    }
}