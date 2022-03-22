package com.sunionrd.blelocker.View.SignIn

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.viewbinding.ViewBinding
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.CognitoUtils.CognitoControlViewModel
import com.sunionrd.blelocker.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
//import kotlinx.android.synthetic.main.fragment_forget_password.*
//import kotlinx.android.synthetic.main.fragment_login.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ForgetPassword: BaseFragment() {
    private val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private var VerificationCodeDialog: AlertDialog? = null
    override fun getLayoutRes()= R.layout.fragment_forget_password
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        return null
    }
    override fun onViewHasCreated() {
//        btnSubmit.setOnClickListener {
//            if(etPass.text.toString() != etRepeatPass.text.toString())return@setOnClickListener
//            handleNewPassWord()
//        }
    }

    override fun onBackPressed() {

    }

    private fun handleNewPassWord(){
        /*
        cognitoViewModel.forgotPasswordInBackground(cognitoViewModel.mUserID.value, etPass.text.toString())
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
                    val editText = EditText(requireActivity())
                    VerificationCodeDialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                        .setTitle("Enter your Verification Code:")
                        .setCancelable(false)
                        .setView(editText)
                        .setPositiveButton("confirm") { dialog: DialogInterface, _: Int ->
                            IdentityResponse(mapOf("Code" to editText.text.toString()))
                        }
                        .show()
                }

                IdentityRequest.SUCCESS -> {
                    Toast.makeText(requireActivity(), "Success!", Toast.LENGTH_LONG).show()
                    //to login page
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

         */
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