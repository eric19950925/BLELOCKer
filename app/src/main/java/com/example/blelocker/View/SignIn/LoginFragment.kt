package com.example.blelocker.View.SignIn

import android.content.DialogInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.viewbinding.ViewBinding
import com.amazonaws.AmazonClientException
import com.amazonaws.mobileconnectors.cognitoidentityprovider.exceptions.CognitoParameterInvalidException
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException
import com.example.blelocker.BaseFragment
import com.example.blelocker.CognitoUtils.CognitoControlViewModel
import com.example.blelocker.CognitoUtils.IdentityRequest
import com.example.blelocker.MainActivity
import com.example.blelocker.R
import com.example.blelocker.databinding.FragmentLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.net.UnknownHostException

class LoginFragment: BaseFragment() {
    override fun getLayoutRes(): Int? = null

    private lateinit var currentBinding: FragmentLoginBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        currentBinding = FragmentLoginBinding.inflate(inflater, container, false)
        return currentBinding
    }

    private val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private var dialogMfa: AlertDialog? = null

    override fun onViewHasCreated() {

        getDetail()
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

    private fun getDetail(){
        cognitoViewModel.getUserDetails (
            onSuccess = {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
                    Log.d("TAG",it)
                    cognitoViewModel.initLogin(it){ identityRequest, map, callback ->
                        when(identityRequest) {
                            IdentityRequest.SUCCESS -> {
                                Navigation.findNavController(requireView()).navigate(R.id.action_login_to_alllock)
                            }
                            else -> {
                                Log.d("TAG","auto login failure")
                            }
                        }
                    }
                }
            },
            onFailure = {//todo if login failure , it can not go back to login page
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
//                    val navHostFragment = (activity as MainActivity).supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment
//                    navHostFragment.navController.navigate(R.id.action_to_login)
                }
            }
        )
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
                    val editText = EditText(requireActivity())
                    dialogMfa = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                        .setTitle("Enter your MFA code:")
                        .setCancelable(false)
                        .setView(editText)
                        .setPositiveButton("confirm") { dialog: DialogInterface, _: Int ->
                            callback(mapOf("mfaCode" to editText.text.toString()))
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
                    //todo
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        cognitoViewModel.mUserID.value = currentBinding.etUsername.getText()
                    }
                    Navigation.findNavController(requireView()).navigate(R.id.action_login_to_alllock)
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
                                    val editText = EditText(requireActivity())
                                    MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                                        .setTitle("Enter your Verification Code:")
                                        .setCancelable(false)
                                        .setView(editText)
                                        .setPositiveButton("confirm") { dialog: DialogInterface, _: Int ->
                                            cognitoViewModel.confirmUser(currentBinding.etUsername.getText(), editText.getText().toString().replace(" ", ""))
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