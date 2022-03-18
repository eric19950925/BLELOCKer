package com.example.blelocker.View.Github

import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.example.blelocker.BaseFragment
import com.example.blelocker.GithubViewModel
import com.example.blelocker.R
import com.example.blelocker.databinding.FragmentGitHubBinding
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
    private lateinit var currentBinding: FragmentGitHubBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentGitHubBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        //set top menu
        setHasOptionsMenu(true)
        currentBinding.myToolbar.menu.clear()
        currentBinding.myToolbar.inflateMenu(R.menu.my_menu)
        currentBinding.myToolbar.menu.findItem(R.id.scan).isVisible = false
        currentBinding.myToolbar.menu.findItem(R.id.play).isVisible = false
        currentBinding.myToolbar.menu.findItem(R.id.delete).isVisible = false
        currentBinding.myToolbar.title = "Account"
        currentBinding.myToolbar.setTitleTextColor(resources.getColor(R.color.primary, null))
        currentBinding.myToolbar.setBackgroundColor(resources.getColor(R.color.white, null))
        currentBinding.myToolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        currentBinding.myToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        setupGithub()

//        setupLogTextview()


    }

    private fun setupGithub() {
        currentBinding.clGithubCommit.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_to_commits)
        }
    }

    private fun setupLogTextview() {
        currentBinding.logTv.movementMethod = ScrollingMovementMethod.getInstance()
        currentBinding.logTv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                while (currentBinding.logTv.canScrollVertically(1)) {
                    currentBinding.logTv.scrollBy(0, 10)
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
    }

    private fun showLog(logText: String) = viewLifecycleOwner.lifecycleScope.launch{
        try {
            var log = currentBinding.logTv.text.toString()

            log += "\n\n${logText}"

            currentBinding.logTv.text = log
        } catch (e: IOException) {
        }
    }
    private fun cleanLog() {
//        log_tv.text = ""
    }
    override fun onBackPressed() {
    }
}