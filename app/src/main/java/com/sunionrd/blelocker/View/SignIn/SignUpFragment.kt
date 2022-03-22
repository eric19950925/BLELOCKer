package com.sunionrd.blelocker.View.SignIn

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.CognitoUtils.CognitoControlViewModel
import com.sunionrd.blelocker.R
//import kotlinx.android.synthetic.main.fragment_signup.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SignUpFragment: BaseFragment() {
    override fun getLayoutRes(): Int = R.layout.fragment_signup
    val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private var userId: String? = null
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        return null
    }
    override fun onViewHasCreated() {
        /*
        btnSignUp.setOnClickListener {
            if (etPass.getText().toString().endsWith(etRepeatPass.getText().toString())) {
                userId = etUsername.text.toString().replace(" ", "")
                cognitoViewModel.addAttribute("name", userId)
//                cognitoViewModel.addAttribute(
//                    "phone_number",
//                    etMobile.getText().toString().replace(" ", "")
//                )
                cognitoViewModel.addAttribute("email", etEmail.getText().toString().replace(" ", ""))
                cognitoViewModel.signUpInBackground(userId, etPass.getText().toString())
            } else {
            }
        }

        btnVerify.setOnClickListener {
            cognitoViewModel.confirmUser(userId, etConfCode.getText().toString().replace(" ", ""))
            //finish();
        }

         */
    }

    override fun onBackPressed() {
    }
}