package com.example.blelocker.View

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blelocker.BaseFragment
import com.example.blelocker.BluetoothUtils.BleControlViewModel
import com.example.blelocker.CognitoUtils.CognitoControlViewModel
import com.example.blelocker.CognitoUtils.LogOutRequest.*
import com.example.blelocker.MainActivity
import com.example.blelocker.OneLockViewModel
import com.example.blelocker.R
import kotlinx.android.synthetic.main.fragment_all_locks.*
import kotlinx.android.synthetic.main.fragment_all_locks.my_toolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class AllLocksFragment : BaseFragment(){
    val oneLockViewModel by viewModel<OneLockViewModel>()
    val cognitoViewModel by sharedViewModel<CognitoControlViewModel>()
    val bleViewModel by sharedViewModel<BleControlViewModel>()
    private lateinit var mSharedPreferences: SharedPreferences
    var count = 0
    override fun getLayoutRes(): Int = R.layout.fragment_all_locks

    override fun onViewHasCreated() {
        setHasOptionsMenu(true)
        my_toolbar.menu.clear()
        my_toolbar.inflateMenu(R.menu.my_menu)
        my_toolbar.menu.findItem(R.id.github).isVisible = true
        my_toolbar.menu.findItem(R.id.play).isVisible = false
//        my_toolbar.menu.findItem(R.id.delete).isVisible = false
        my_toolbar.title = "All Locks"


        val adapter = AllLocksAdapter(AllLocksAdapter.OnClickListener{
            if(!checkPermissions())return@OnClickListener
            if(!checkBTenable())return@OnClickListener
//            val bundle = Bundle()
//            bundle.putString("MAC_ADDRESS", it)
            saveCurrentLockMac(it)
            Navigation.findNavController(requireView()).navigate(R.id.action_to_onelock/*,bundle*/)
        })

        recyclerview.adapter = adapter
        recyclerview.layoutManager = LinearLayoutManager(requireContext())

        cognitoViewModel.getUserInfo{
            tv_user_id.text = cognitoViewModel.mUserID.value
            tv_user_jwt_token.text = cognitoViewModel.mJwtToken.value
        }
        cognitoViewModel.initialAWSIotClient()// todo 需要等待他做完才能connect

        my_toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.scan -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_alllocks_to_scan)
                    true
                }
                R.id.github -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_alllocks_to_github)
                    true
                }
                R.id.delete -> {
                    oneLockViewModel.deleteLocks()
                    true
                }
                else -> false
            }
        }
        oneLockViewModel.allLocks.observe(this) { locks ->
            locks.let { adapter.submitList(it) }
            count = locks.size
            if(count > 8){
                my_toolbar.menu.findItem(R.id.scan).isVisible = false
            }
        }
        btn_logout.setOnClickListener {
            logOut()
        }

        btn_autoUnlock.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_alllocks_to_autolock)
        }

        btn_pub.setOnClickListener {
            cognitoViewModel.mqttPublish()
        }

    }

    private fun logOut()= viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
        cognitoViewModel.LogOut { LogOutRequest ->
            when(LogOutRequest){
                SUCCESS -> {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        val navHostFragment = (activity as MainActivity).supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment
                        navHostFragment.navController.navigate(R.id.action_to_login)
                    }
//                    Navigation.findNavController(requireView()).navigate(R.id.action_to_login)
//                    Toast.makeText(requireContext(), "Log out Success", Toast.LENGTH_LONG).show()
                }
                FAILURE -> {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                        Toast.makeText(requireContext(), "Log out Failure", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    private fun saveCurrentLockMac(macAddress: String) {
        mSharedPreferences = requireActivity().getSharedPreferences(MainActivity.DATA, 0)
        mSharedPreferences.edit()
            .putString(MainActivity.CURRENT_LOCK_MAC, macAddress)
            .apply()
    }

    override fun onBackPressed() {
//        logOut()
        requireActivity().finish()
    }
    private fun checkPermissions() : Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1)
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH_SCAN
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

    }
    private fun checkBTenable(): Boolean {
        val mBluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothManager.adapter?.takeIf { !it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 124)
        }
        return mBluetoothManager.adapter?.isEnabled?:false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cognitoViewModel.mqttDisconnect()
    }
}