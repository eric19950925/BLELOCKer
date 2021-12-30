package com.example.blelocker.View

import android.content.DialogInterface
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.Navigation
import com.example.blelocker.BaseFragment
import com.example.blelocker.CognitoUtils.CognitoControlViewModel
import com.example.blelocker.CognitoUtils.IdentityRequest
import com.example.blelocker.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_login.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class LoginFragment: BaseFragment() {
    override fun getLayoutRes(): Int = R.layout.fragment_login
    val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private var MFAcodeDialog: AlertDialog? = null
    override fun onViewHasCreated() {
        btnLogin.setOnClickListener{
            handleLogin()
        }
        btnSignup.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_login_to_signup)
        }



    }

    private fun handleLogin() {
        cognitoViewModel.initLogin(
            etUsername.text.toString().replace(" ", "")
        ) { identityRequest, map, callback ->
            when(identityRequest) {
                IdentityRequest.NEED_CREDENTIALS -> {
                    callback(mapOf("password" to etPassword.text.toString()))
                }

                IdentityRequest.NEED_MULTIFACTORCODE -> {
                    val editText = EditText(requireActivity())
                    MFAcodeDialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
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
                    Navigation.findNavController(requireView()).navigate(R.id.action_login_to_alllock)
                }

                IdentityRequest.FAILURE -> {
                    Toast.makeText(requireContext(), "Sign in Failure", Toast.LENGTH_LONG).show()
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

}