package com.example.blelocker.View.Github

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.blelocker.BaseFragment
import com.example.blelocker.GithubViewModel
import com.example.blelocker.R
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class CommitFragment: BaseFragment() {
    private val githubViewModel by sharedViewModel<GithubViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_commits
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        return null
    }
    override fun onViewHasCreated() {
        setHasOptionsMenu(true)
//        my_toolbar.menu.clear()
//        my_toolbar.inflateMenu(R.menu.my_menu)
//        my_toolbar.menu.findItem(R.id.github).isVisible = true
//        my_toolbar.menu.findItem(R.id.scan).isVisible = false
//        my_toolbar.menu.findItem(R.id.play).isVisible = false
//        my_toolbar.menu.findItem(R.id.delete).isVisible = false
//        my_toolbar.title = "Commits"


        val adapter = CommitsAdapter(
            CommitsAdapter.OnClickListener{
//            val bundle = Bundle()
//            bundle.putString("MAC_ADDRESS", it)
//            Navigation.findNavController(requireView()).navigate(R.id.action_to_onelock,bundle)
            }
        )

//        recyclerview.adapter = adapter
//        recyclerview.layoutManager = LinearLayoutManager(requireContext())


        githubViewModel.mCommits.observe(this){
            adapter.submitList(it)
        }
    }

    override fun onBackPressed() {
    }
}