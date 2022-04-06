package com.sunionrd.blelocker.View.account

import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sunionrd.blelocker.BaseFragment
import com.sunionrd.blelocker.CognitoUtils.CognitoControlViewModel
import com.sunionrd.blelocker.CognitoUtils.IdentityRequest
import com.sunionrd.blelocker.MainActivity
import com.sunionrd.blelocker.R
import com.sunionrd.blelocker.TFHApiViewModel
import com.sunionrd.blelocker.databinding.BaseDialogLayoutBinding
import com.sunionrd.blelocker.databinding.FragmentAccountBinding
import kotlinx.coroutines.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.IOException

class AccountFragment: BaseFragment(){
    private val accountViewModel by sharedViewModel<com.sunionrd.blelocker.AccountViewModel>()
    private val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    private val tfhApiViewModel by viewModel<TFHApiViewModel>()
    override fun getLayoutRes(): Int = R.layout.fragment_account
    var LoadingScope: Job? = null
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

    private fun checkPasswordBeforeDelete(password: String) {
        (requireActivity() as MainActivity).showLoadingView()
        //to check password you must log out first
        cognitoViewModel.LogOut { LogOutRequest ->
            when(LogOutRequest){
                com.sunionrd.blelocker.CognitoUtils.LogOutRequest.SUCCESS -> {
                    cognitoViewModel.checkPassword { identityRequest, map, callback ->
                        when(identityRequest) {
                            IdentityRequest.NEED_CREDENTIALS -> {
                                callback(mapOf("password" to password))
                            }

                            IdentityRequest.SUCCESS -> {
                                closeFCM()
                                deleteUser()
                            }
                            IdentityRequest.FAILURE -> {
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                                    Toast.makeText(requireContext(), "Delete Failure", Toast.LENGTH_LONG).show()
                                    if (findNavController().currentDestination?.id == R.id.account_Fragment) {
                                        (requireActivity() as MainActivity).hideLoadingView()
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
                com.sunionrd.blelocker.CognitoUtils.LogOutRequest.FAILURE -> {
                    Log.d("TAG","log out failure")
                }
            }
        }
    }

    private fun showLoading() = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
        (requireActivity() as MainActivity).showLoadingView()
        try{
            var mTimestamp = 0
            while(mTimestamp < 60) {
                delay(1000)
                mTimestamp += 1
            }
        }finally {
            (requireActivity() as MainActivity).hideLoadingView()
        }
    }

    private fun setupDeleteUserBtnClick() {
        currentBinding.btnDeleteUser.setOnClickListener {
            //editText ui
            val mBinding = BaseDialogLayoutBinding.inflate(layoutInflater)
            mBinding.efcPassword.visibility = View.VISIBLE
            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Delete Account")
                .setCancelable(false)
                .setMessage("Are you sure to delete account?\nType password to delete.")
                .setView(mBinding.root)
                .setPositiveButton("Delete") { dialog: DialogInterface, _: Int ->
                    checkPasswordBeforeDelete(mBinding.efcPassword.getText())
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun deleteUser() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {//
            cognitoViewModel.getIdentityId { jwtToken ->
                cognitoViewModel.currentUser.value?.signOut()
                tfhApiViewModel.deleteAccount(jwtToken) { response ->
//                    when (response) {
//                        "AAA" -> {
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//                                cognitoViewModel.closeCognitoCache()
                                reStartActivity()
                            }
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                                cognitoViewModel.currentUser.value = null
                            }
//                        }
//                        else -> {
//                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//                                Log.e("TAG", "errorMsg: $response")
//                                LoadingScope?.cancel()
//                            }
//                        }
//                    }
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
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            (requireActivity() as MainActivity).showLoadingView()
        }
        closeFCM()
        cognitoViewModel.LogOut { LogOutRequest ->
            when(LogOutRequest){
                com.sunionrd.blelocker.CognitoUtils.LogOutRequest.SUCCESS -> {
                    reStartActivity()
                }
                com.sunionrd.blelocker.CognitoUtils.LogOutRequest.FAILURE -> {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        Toast.makeText(requireContext(), "Log out Failure", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun closeFCM() {
        cognitoViewModel.getIdentityId{ jwtToken ->
            (requireActivity() as MainActivity).getFCMtoken {
                tfhApiViewModel.setFCM(false, it, jwtToken){}
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