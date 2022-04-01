package com.sunionrd.blelocker.View.SignIn

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.CognitoUtils.CognitoControlViewModel
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.databinding.FragmentSignupBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SignUpFragment: BaseFragment() {
    override fun getLayoutRes(): Int = R.layout.fragment_signup
    private val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private var userId: String? = null
    private lateinit var currentBinding: FragmentSignupBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentSignupBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {

        currentBinding.btnSignUp.setOnClickListener {
            if (currentBinding.etPass.getText().endsWith(currentBinding.etRepeatPass.getText())) {
                userId = currentBinding.etUsername.getText().replace(" ", "")
                cognitoViewModel.addAttribute("name", userId)
//                cognitoViewModel.addAttribute(
//                    "phone_number",
//                    etMobile.getText().toString().replace(" ", "")
//                )
                cognitoViewModel.addAttribute("email", currentBinding.etEmail.getText().replace(" ", ""))
                currentBinding.btnSignUp.isClickable = false //avoid to signup again
                cognitoViewModel.signUpInBackground(userId, currentBinding.etPass.getText())
            } else {
            }
        }

        currentBinding.btnVerify.setOnClickListener {
            cognitoViewModel.confirmUser(userId, currentBinding.etConfCode.getText().replace(" ", ""))
            reStartActivity()
        }
    }

    override fun onBackPressed() {
    }
}