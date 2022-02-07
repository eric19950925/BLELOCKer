package com.example.blelocker.View.SignIn

import com.example.blelocker.BaseFragment
import com.example.blelocker.CognitoUtils.CognitoControlViewModel
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_signup.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SignUpFragment: BaseFragment() {
    override fun getLayoutRes(): Int = R.layout.fragment_signup
    val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private var userId: String? = null
    override fun onViewHasCreated() {
        btnSignUp.setOnClickListener {
            if (etPass.getText().toString().endsWith(etRepeatPass.getText().toString())) {
                userId = etUsername.text.toString().replace(" ", "")
                cognitoViewModel.addAttribute("name", userId)
                cognitoViewModel.addAttribute(
                    "phone_number",
                    etMobile.getText().toString().replace(" ", "")
                )
                cognitoViewModel.addAttribute("email", etEmail.getText().toString().replace(" ", ""))
                cognitoViewModel.signUpInBackground(userId, etPass.getText().toString())
            } else {
            }
        }

        btnVerify.setOnClickListener {
            cognitoViewModel.confirmUser(userId, etConfCode.getText().toString().replace(" ", ""))
            //finish();
        }
    }

    override fun onBackPressed() {
    }
}