package com.sunionrd.blelocker.View.SignIn

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.CognitoUtils.CognitoControlViewModel
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.databinding.FragmentEnterUserIdBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class EnterUserId: BaseFragment() {
    override fun getLayoutRes()= R.layout.fragment_enter_user_id
    private val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private lateinit var currentBinding: FragmentEnterUserIdBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentEnterUserIdBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        currentBinding.tvEnterIdTitle.text = "Forget Password"
        currentBinding.btnNext.setOnClickListener {
            cognitoViewModel.mUserID.value = currentBinding.etEmail.getText().replace(" ", "")
            Navigation.findNavController(requireView()).navigate(R.id.action_enterUserId_to_forgetPassword)
        }
    }

    override fun onBackPressed() {
    }
}