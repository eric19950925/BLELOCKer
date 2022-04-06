package com.sunionrd.blelocker.View.SignIn

import android.content.DialogInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import com.amazonaws.AmazonClientException
import com.amazonaws.mobileconnectors.cognitoidentityprovider.exceptions.CognitoParameterInvalidException
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.CognitoUtils.CognitoControlViewModel
import com.sunionrd.blelocker.CognitoUtils.IdentityRequest
import com.sunionrd.blelocker.MainActivity
import com.sunionrd.blelocker.databinding.FragmentLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.TFHApiViewModel
import com.sunionrd.blelocker.databinding.BaseDialogLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.net.UnknownHostException

class LoginFragment: BaseFragment() {
    override fun getLayoutRes(): Int? = null

    private lateinit var currentBinding: FragmentLoginBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        currentBinding = FragmentLoginBinding.inflate(inflater, container, false)
        return currentBinding
    }

    private val tfhApiViewModel by viewModel<TFHApiViewModel>()
    private val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private var dialogMfa: AlertDialog? = null

    override fun onViewHasCreated() {

        currentBinding.btnLogin.setOnClickListener{
            handleLogin()
        }
        currentBinding.btnSignup.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_login_to_signup)
        }
        currentBinding.tvForgetPassword.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_login_Fragment_to_enterUserId)
        }


    }

    private fun handleLogin() {

        cognitoViewModel.initLogin(
            currentBinding.etUsername.getText().replace(" ", "")
        ) { identityRequest, map, callback ->
            when(identityRequest) {
                IdentityRequest.NEED_CREDENTIALS -> {
                    callback(mapOf("password" to currentBinding.etPassword.getText()))
                }

                IdentityRequest.NEED_MULTIFACTORCODE -> {
                    val editText = com.sunionrd.blelocker.widget.EditFieldCompoundView(requireActivity())
                    dialogMfa = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                        .setTitle("Enter your MFA code:")
                        .setCancelable(false)
                        .setView(editText)
                        .setPositiveButton("confirm") { dialog: DialogInterface, _: Int ->
                            callback(mapOf("mfaCode" to editText.getText()))
                        }
                        .show()
                }

//                IdentityRequest.NEED_NEWPASSWORD -> {
//                    val newPasswordDialog = layoutInflater.inflate(R.layout.dialog_new_password, null)
//                    val passwordInput = newPasswordDialog.find(R.id.new_password_form_password) as EditText
//                    alert {
//                        title = "Enter New Password"
//                        customView = newPasswordDialog
//                        positiveButton("OK") { callback(mapOf("password" to passwordInput.text.toString())) }
//                    }.show()
//                }

                IdentityRequest.SUCCESS -> {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        //if have not login success, get detail will always failure, so can not do this.
                        //Question : How to avoid going to home page and back to login page by login with id which had been delete?
                        //Answer : sign out before delete account.
                        cognitoViewModel.mUserID.value = currentBinding.etUsername.getText()
                        cognitoViewModel.getIdentityId{ jwtToken ->
                            (requireActivity() as MainActivity).getFCMtoken {
                                tfhApiViewModel.setFCM(true, it, jwtToken){}
                            }
                            Navigation.findNavController(requireView()).navigate(R.id.action_login_to_alllock)
                        }
                    }
                    cognitoViewModel.setAttachPolicy()
                }

                IdentityRequest.FAILURE -> {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        map?.let {
                            when(it["exception"] as AmazonClientException ){
                                is CognitoParameterInvalidException -> {
                                    Toast.makeText(requireActivity(), "請填入帳號", Toast.LENGTH_LONG).show()
                                }
                                is UserNotFoundException -> {
                                    Toast.makeText(requireActivity(), "找不到此帳號", Toast.LENGTH_LONG).show()
                                }
                                is NotAuthorizedException -> {
                                    Toast.makeText(requireActivity(), "帳號或密碼錯誤", Toast.LENGTH_LONG).show()
                                }
                                is UserNotConfirmedException -> {
                                    val mBinding = BaseDialogLayoutBinding.inflate(layoutInflater)
                                    mBinding.efcCode.visibility = View.VISIBLE
                                    MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                                        .setTitle("Enter your Verification Code:")
                                        .setCancelable(false)
                                        .setView(mBinding.root)
                                        .setPositiveButton("confirm") { dialog: DialogInterface, _: Int ->
                                            cognitoViewModel.confirmUser(currentBinding.etUsername.getText(), mBinding.efcCode.getText().replace(" ", ""))
                                        }
                                        .show()
                                }
                                else ->{
                                    when( it["exception"]?.cause ){
                                        is UnknownHostException -> {
                                            Toast.makeText(requireActivity(), "網路錯誤", Toast.LENGTH_LONG).show()
                                            return@let
                                        }
                                        is UserNotFoundException -> {
                                            Toast.makeText(requireActivity(), "找不到此帳號", Toast.LENGTH_LONG).show()
                                        }
                                        else -> Toast.makeText(requireActivity(), "發生某些錯誤", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    showErrorDialog()
                }
            }
        }
    }

    override fun onBackPressed() {

    }

    private fun showErrorDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("ERROR")
            .setCancelable(false)
            .setMessage("somthing wrong")
            .setPositiveButton("confirm") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cognitoViewModel.closeCognitoCache()
    }
}