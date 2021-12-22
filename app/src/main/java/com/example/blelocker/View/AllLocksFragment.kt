package com.example.blelocker.View

import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blelocker.BaseFragment
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_all_locks.*

class AllLocksFragment : BaseFragment(){
    override fun getLayoutRes(): Int = R.layout.fragment_all_locks

    override fun onViewHasCreated() {
        setHasOptionsMenu(true)
        my_toolbar.menu.clear()
        my_toolbar.inflateMenu(R.menu.my_menu)
        my_toolbar.menu.findItem(R.id.delete).isVisible = false
        my_toolbar.title = "All Locks"


        val adapter = AllLocksAdapter()
        recyclerview.adapter = adapter
        recyclerview.layoutManager = LinearLayoutManager(requireContext())


        my_toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.scan -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_onelock_to_scan)
                    true
                }
                R.id.delete -> {
                    true
                }
                R.id.play -> {
                    my_toolbar.menu.findItem(R.id.play).isVisible = false
                    my_toolbar.menu.findItem(R.id.pause).isVisible = true
//                    testScope = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
//                        try{
//                            var timestamp_ = 0
//                            while(timestamp_<60) {
//                                delay(1000)
//                                timestamp_ += 1
//                                requireActivity().runOnUiThread{
//                                    showLog("Thread Test ${timestamp_}")
//                                }
//                            }
//                        }finally {
//                            requireActivity().runOnUiThread{
//                                showLog("Stop Thread Test.")
//                                my_toolbar.menu.findItem(R.id.pause).isVisible = false
//                                my_toolbar.menu.findItem(R.id.play).isVisible = true
//                            }
//                        }
//                    }
                    true
                }
                R.id.pause -> {
                    my_toolbar.menu.findItem(R.id.pause).isVisible = false
                    my_toolbar.menu.findItem(R.id.play).isVisible = true
//                    testScope?.cancel()
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        Navigation.findNavController(requireView()).navigate(R.id.action_to_onelock)
    }
}