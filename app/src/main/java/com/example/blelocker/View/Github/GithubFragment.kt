package com.example.blelocker.View.Github

import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.blelocker.BaseFragment
import com.example.blelocker.GithubViewModel
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_onelock.*
import kotlinx.android.synthetic.main.fragment_onelock.my_toolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.IOException

class GithubFragment: BaseFragment() {
    private val githubViewModel by sharedViewModel<GithubViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_git_hub
    var testScope: Job? = null

    override fun onViewHasCreated() {
        //set top menu
        my_toolbar.menu.clear()
        setHasOptionsMenu(true)
        my_toolbar.inflateMenu(R.menu.my_menu)
        my_toolbar.menu.findItem(R.id.github).isVisible = true
        my_toolbar.menu.findItem(R.id.scan).isVisible = false
        my_toolbar.title = "Git hub"

        //set log textview
        log_tv.movementMethod = ScrollingMovementMethod.getInstance()
        log_tv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                while (log_tv.canScrollVertically(1)) {
                    log_tv.scrollBy(0, 10)
                }
            }
        })

        githubViewModel.data.observe(this, Observer {
            // Populate the UI
            showLog(it.toString())
            viewLifecycleOwner.lifecycle.coroutineScope.launch {
                if(githubViewModel.mCommits.value == null)return@launch
                delay(1000)
                Navigation.findNavController(requireView()).navigate(R.id.action_to_commits)
            }
        })

        githubViewModel.loadingState.observe(this, Observer {
            // Observe the loading state
            showLog(it.toString())
        })

        //top menu function
        my_toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.github -> {
                    githubViewModel.fetchData()
                    true
                }
                R.id.delete -> {
                    cleanLog()
                    true
                }
                R.id.play -> {
                    my_toolbar.menu.findItem(R.id.play).isVisible = false
                    my_toolbar.menu.findItem(R.id.pause).isVisible = true
                    testScope = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        try{
                            var timestamp_ = 0
                            while(timestamp_<60) {
                                delay(1000)
                                timestamp_ += 1
                                requireActivity().runOnUiThread{
                                    showLog("Thread Test ${timestamp_}")
                                }
                            }
                        }finally {
                            requireActivity().runOnUiThread{
                                showLog("Stop Thread Test.")
                                my_toolbar.menu.findItem(R.id.pause).isVisible = false
                                my_toolbar.menu.findItem(R.id.play).isVisible = true
                            }
                        }
                    }
                    true
                }
                R.id.pause -> {
                    my_toolbar.menu.findItem(R.id.pause).isVisible = false
                    my_toolbar.menu.findItem(R.id.play).isVisible = true
                    testScope?.cancel()
                    true
                }
                else -> false
            }
        }
    }
    private fun showLog(logText: String) = viewLifecycleOwner.lifecycleScope.launch{
        try {
            var log = log_tv.text.toString()

            log = log +"\n\n${logText}"

            log_tv.text = log
        } catch (e: IOException) {
        }
    }
    private fun cleanLog() {
        log_tv.text = ""
    }
    override fun onBackPressed() {
        Navigation.findNavController(requireView()).navigate(R.id.action_back_to_alllocks)
    }
}