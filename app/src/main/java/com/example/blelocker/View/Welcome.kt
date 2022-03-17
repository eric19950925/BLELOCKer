package com.example.blelocker.View

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.example.blelocker.BaseFragment
import com.example.blelocker.R
import com.example.blelocker.databinding.FragmentCommitsBinding
import com.example.blelocker.databinding.FragmentLoginBinding
//import kotlinx.android.synthetic.main.fragment_all_locks.*
//import kotlinx.android.synthetic.main.fragment_commits.*
//import kotlinx.android.synthetic.main.fragment_commits.my_toolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Welcome:BaseFragment() {
    override fun getLayoutRes(): Int = R.layout.fragment_commits
    private lateinit var currentBinding: FragmentCommitsBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentCommitsBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        currentBinding.logo.visibility = View.VISIBLE
        setHasOptionsMenu(true)
        currentBinding.myToolbar.visibility = View.GONE

        val objectAnimator = ObjectAnimator.ofObject(
            currentBinding.logo,
            "textColor",
            ArgbEvaluator(),
            ContextCompat.getColor(requireContext(), R.color.white),
            ContextCompat.getColor(requireContext(), R.color.md_theme_light_onPrimary)
        )

        objectAnimator.duration = 2000
        objectAnimator.start()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            delay(3000)
            Navigation.findNavController(requireView()).navigate(R.id.action_welcome_to_login_Fragment)
        }
    }

    override fun onBackPressed() {
    }
}