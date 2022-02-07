package com.example.blelocker.View.SignIn

import androidx.navigation.Navigation
import com.example.blelocker.BaseFragment
import com.example.blelocker.CognitoUtils.CognitoControlViewModel
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_enter_user_id.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class EnterUserId: BaseFragment() {
    override fun getLayoutRes()= R.layout.fragment_enter_user_id
    private val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    override fun onViewHasCreated() {
        tv_enter_id_title.text = "Forget Password"
        btn_next.setOnClickListener {
            cognitoViewModel.mUserID.value = etEmail.text.toString().replace(" ", "")
            Navigation.findNavController(requireView()).navigate(R.id.action_enterUserId_to_forgetPassword)
        }
    }

    override fun onBackPressed() {
    }
}