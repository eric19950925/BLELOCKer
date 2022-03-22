package com.sunionrd.blelocker.View.account

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.AccountViewModel
import com.sunionrd.blelocker.MainActivity
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.databinding.FragmentCommitsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class CommitFragment: BaseFragment() {
    private val githubViewModel by sharedViewModel<com.sunionrd.blelocker.AccountViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_commits
    private var loadingScope: Job? = null
    private lateinit var currentBinding: FragmentCommitsBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentCommitsBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        setHasOptionsMenu(true)
        currentBinding.myToolbar.menu.clear()
        currentBinding.myToolbar.inflateMenu(R.menu.my_menu)
        currentBinding.myToolbar.menu.findItem(R.id.github).isVisible = false
        currentBinding.myToolbar.menu.findItem(R.id.scan).isVisible = false
        currentBinding.myToolbar.menu.findItem(R.id.play).isVisible = false
        currentBinding.myToolbar.menu.findItem(R.id.delete).isVisible = false
        currentBinding.myToolbar.title = "Commits"


        val adapter = CommitsAdapter(
            CommitsAdapter.OnClickListener{
//            val bundle = Bundle()
//            bundle.putString("MAC_ADDRESS", it)
//            Navigation.findNavController(requireView()).navigate(R.id.action_to_onelock,bundle)
            }
        )

        currentBinding.recyclerview.adapter = adapter
        currentBinding.recyclerview.layoutManager = LinearLayoutManager(requireContext())


        githubViewModel.mCommits.observe(this){
            adapter.submitList(it)
            loadingScope?.cancel()
        }
    }

    override fun onResume() {
        super.onResume()
        loadingScope = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            try{
                (requireActivity() as MainActivity).showLoadingView()
                var mTimestamp = 0
                while(mTimestamp < 60) {
                    delay(1000)
                    mTimestamp += 1
                }
            }finally {
                (requireActivity() as MainActivity).hideLoadingView()
            }
        }
        githubViewModel.fetchData()
    }

    override fun onBackPressed() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        githubViewModel.mCommits.value = null
    }
}