package com.sunionrd.blelocker.View.SignIn

import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.amazonaws.AmazonClientException
import com.amazonaws.mobileconnectors.cognitoidentityprovider.exceptions.CognitoParameterInvalidException
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.CognitoUtils.CognitoControlViewModel
import com.sunionrd.blelocker.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sunionrd.blelocker.CognitoUtils.IdentityRequest
import com.sunionrd.blelocker.databinding.FragmentForgetPasswordBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.net.UnknownHostException

class ForgetPassword: BaseFragment() {
    private val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private var VerificationCodeDialog: AlertDialog? = null
    override fun getLayoutRes()= R.layout.fragment_forget_password
    private lateinit var currentBinding: FragmentForgetPasswordBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentForgetPasswordBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        currentBinding.btnSubmit.setOnClickListener {
            if(currentBinding.etPass.getText() != currentBinding.etRepeatPass.getText())return@setOnClickListener
            handleNewPassWord()
        }
    }

    override fun onBackPressed() {

    }

    private fun handleNewPassWord(){

        cognitoViewModel.forgotPasswordInBackground(cognitoViewModel.mUserID.value, currentBinding.etPass.getText())
        { identityRequest, map, IdentityResponse ->
            when(identityRequest) {
//                IdentityRequest.NEED_CREDENTIALS -> {
//                    IdentityResponse(mapOf("password" to etPassword.text.toString()))
//                }
//
//                IdentityRequest.NEED_MULTIFACTORCODE -> {
////                    val editText = EditText(requireActivity())
////                    MFAcodeDialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
////                        .setTitle("Enter your MFA code:")
////                        .setCancelable(false)
////                        .setView(editText)
////                        .setPositiveButton("confirm") { dialog: DialogInterface, _: Int ->
////                            IdentityResponse(mapOf("mfaCode" to editText.text.toString()))
////                        }
////                        .show()
//                }

                IdentityRequest.NEED_NEWPASSWORD -> {
                    val editText = com.sunionrd.blelocker.widget.EditFieldCompoundView(requireActivity())
                    //todo : need to use custom edit widget, and ui should be bigger
                    VerificationCodeDialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                        .setTitle("Enter your Verification Code:")
                        .setCancelable(false)
                        .setView(editText)
                        .setPositiveButton("confirm") { dialog: DialogInterface, _: Int ->
                            IdentityResponse(mapOf("Code" to editText.getText()))
                        }
                        .show()
                }

                IdentityRequest.SUCCESS -> {
                    Toast.makeText(requireActivity(), "Success!", Toast.LENGTH_LONG).show()
                    reStartActivity()
                }

                IdentityRequest.FAILURE -> {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        map?.let {
                            when(it["exception"] as AmazonClientException){
                                is CognitoParameterInvalidException -> {
                                    Toast.makeText(requireActivity(), "請填入帳號", Toast.LENGTH_LONG).show()
                                }
                                is UserNotFoundException -> {
                                    Toast.makeText(requireActivity(), "找不到此帳號", Toast.LENGTH_LONG).show()
                                }
                                is NotAuthorizedException -> {
                                    Toast.makeText(requireActivity(), "帳號或密碼錯誤", Toast.LENGTH_LONG).show()
                                }
                                else ->{
                                    if( it["exception"]?.cause is UnknownHostException){
                                        Toast.makeText(requireActivity(), "網路錯誤", Toast.LENGTH_LONG).show()
                                        return@let
                                    }else Toast.makeText(requireActivity(), "發生某些錯誤", Toast.LENGTH_LONG).show()
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
}