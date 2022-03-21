package com.example.blelocker.View

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.example.blelocker.BaseFragment
import com.example.blelocker.CognitoUtils.CognitoControlViewModel
import com.example.blelocker.CognitoUtils.IdentityRequest
import com.example.blelocker.R
import com.example.blelocker.databinding.FragmentCommitsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class Welcome:BaseFragment() {
    override fun getLayoutRes(): Int = R.layout.fragment_commits
    private lateinit var currentBinding: FragmentCommitsBinding
    private val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
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

        objectAnimator.duration = 3000
        objectAnimator.start()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            delay(4000)

                cognitoViewModel.autoLogin("eric_shih@sunion.com.tw"){ identityRequest, map, callback ->
                    when(identityRequest) {
                        IdentityRequest.SUCCESS -> {
                            Navigation.findNavController(requireView()).navigate(R.id.action_welcome_to_home_Fragment)
                        }
                        else -> {
                            Log.d("TAG","Need other info, please log in again.")
                            Navigation.findNavController(requireView()).navigate(R.id.action_welcome_to_login_Fragment)
                        }
                    }
                }
         }
    }

    override fun onBackPressed() {
    }
}