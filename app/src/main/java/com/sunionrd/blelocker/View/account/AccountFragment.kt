package com.sunionrd.blelocker.View.account

import android.content.DialogInterface
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.CognitoUtils.CognitoControlViewModel
import com.sunionrd.blelocker.MainActivity
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.TFHApiViewModel
import com.sunionrd.blelocker.databinding.FragmentAccountBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.IOException

class AccountFragment: BaseFragment(){
    private val accountViewModel by sharedViewModel<com.sunionrd.blelocker.AccountViewModel>()
    private val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private val tfhApiViewModel by viewModel<TFHApiViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_account
    var testScope: Job? = null
    private lateinit var currentBinding: FragmentAccountBinding
    override fun getLayoutBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        currentBinding = FragmentAccountBinding.inflate(inflater, container, false)
        return currentBinding
    }
    override fun onViewHasCreated() {
        //set top menu
        setHasOptionsMenu(true)
        currentBinding.myToolbar.menu.clear()
        currentBinding.myToolbar.inflateMenu(R.menu.my_menu)
        currentBinding.myToolbar.menu.findItem(R.id.scan).isVisible = false
        currentBinding.myToolbar.menu.findItem(R.id.play).isVisible = false
        currentBinding.myToolbar.menu.findItem(R.id.delete).isVisible = false
        currentBinding.myToolbar.title = "Account"
        currentBinding.myToolbar.setTitleTextColor(resources.getColor(R.color.primary, null))
        currentBinding.myToolbar.setBackgroundColor(resources.getColor(R.color.white, null))
        currentBinding.myToolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        currentBinding.myToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        setupCommitBtnClick()

//        setupLogTextview()

        setupDeleteUserBtnClick()

        setupLogoutBtnClick()
    }

    private fun setupDeleteUserBtnClick() {
        currentBinding.btnDeleteUser.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Warning")
                .setCancelable(true)
                .setMessage("Are you sure to delete user?\nPress \"confirm\" to delete.")
                .setPositiveButton("confirm") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    deleteUser()
                }
                .show()
        }
    }

    private fun deleteUser() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
            cognitoViewModel.LogOut { LogOutRequest ->
                when(LogOutRequest){
                    com.sunionrd.blelocker.CognitoUtils.LogOutRequest.SUCCESS -> {
                        tfhApiViewModel.subPubUserDelete(
                            cognitoViewModel.mqttManager?:return@LogOut,
                            cognitoViewModel.mIdentityPoolId.value?:return@LogOut,
                            cognitoViewModel.mJwtToken.value?:return@LogOut
                        ){ response ->
                            Log.d("TAG", "response: $response")
                            when(response){
                                "AAA" -> {
                                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                                        cognitoViewModel.closeCognitoCache()
                                        val intent = requireActivity().intent
                                        intent.addFlags(
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                                    or Intent.FLAG_ACTIVITY_NO_ANIMATION
                                        )
                                        requireActivity().overridePendingTransition(0, 0)
                                        requireActivity().finish()

                                        requireActivity().overridePendingTransition(0, 0)
                                        startActivity(intent)
                                    }
                                }
                                else -> {
                                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
                                        Log.d("TAG",response)
                                    }
                                }
                            }
                        }
                    }
                    com.sunionrd.blelocker.CognitoUtils.LogOutRequest.FAILURE -> {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                            Toast.makeText(requireContext(), "Log out Failure", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupCommitBtnClick() {
        currentBinding.clGithubCommit.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_to_commits)
        }
    }

    private fun setupLogoutBtnClick(){
        currentBinding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Warning")
                .setCancelable(true)
                .setMessage("Are you sure to logout?\nPress \"confirm\" to logout.")
                .setPositiveButton("confirm") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    logout()
                }
                .show()
        }
    }

    private fun logout() = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
        cognitoViewModel.LogOut { LogOutRequest ->
            when(LogOutRequest){
                com.sunionrd.blelocker.CognitoUtils.LogOutRequest.SUCCESS -> {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        val intent = requireActivity().intent
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        )
                        requireActivity().overridePendingTransition(0, 0)
                        requireActivity().finish()

                        requireActivity().overridePendingTransition(0, 0)
                        startActivity(intent)
                    }
//                    Navigation.findNavController(requireView()).navigate(R.id.action_to_login)
//                    Toast.makeText(requireContext(), "Log out Success", Toast.LENGTH_LONG).show()
                }
                com.sunionrd.blelocker.CognitoUtils.LogOutRequest.FAILURE -> {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        Toast.makeText(requireContext(), "Log out Failure", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            currentBinding.tvUserName.text = cognitoViewModel.mUserID.value
        }
    }

    private fun setupLogTextview() {
        currentBinding.logTv.movementMethod = ScrollingMovementMethod.getInstance()
        currentBinding.logTv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                while (currentBinding.logTv.canScrollVertically(1)) {
                    currentBinding.logTv.scrollBy(0, 10)
                }
            }
        })

        accountViewModel.data.observe(this, Observer {
            // Populate the UI
            showLog(it.toString())
            viewLifecycleOwner.lifecycle.coroutineScope.launch {
                if(accountViewModel.mCommits.value == null)return@launch
                delay(1000)
                Navigation.findNavController(requireView()).navigate(R.id.action_to_commits)
            }
        })

        accountViewModel.loadingState.observe(this, Observer {
            // Observe the loading state
            showLog(it.toString())
        })
    }

    private fun showLog(logText: String) = viewLifecycleOwner.lifecycleScope.launch{
        try {
            var log = currentBinding.logTv.text.toString()

            log += "\n\n${logText}"

            currentBinding.logTv.text = log
        } catch (e: IOException) {
        }
    }
    private fun cleanLog() {
//        log_tv.text = ""
    }
    override fun onBackPressed() {
    }
}