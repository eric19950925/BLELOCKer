package com.example.blelocker

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import java.util.*
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    //常數設置
    companion object {
        const val CIPHER_MODE = "AES/ECB/NoPadding"
        const val BARCODE_KEY = "SoftChefSunion65"
        val NOTIFICATION_CHARACTERISTIC = UUID.fromString("de915dce-3539-61ea-ade7-d44a2237601f")
        val SUNION_SERVICE_UUID = UUID.fromString("fc3d8cf8-4ddc-7ade-1dd9-2497851131d7")
        const val DATA = "DATA"
        const val MY_LOCK_QRCODE = "MY_LOCK_QRCODE"
        const val MY_LOCK_TOKEN = "MY_LOCK_KEYTWO"
    }
    private var disconnectDialog: AlertDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getSupportActionBar()?.hide()
        initDisconnectedDialog()
    }

    override fun onSupportNavigateUp(): Boolean {
        //enter one lock page
        return findNavController(R.id.my_nav_host_fragment).navigateUp()
    }

    private fun initDisconnectedDialog() {
        disconnectDialog = MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Disconnect")
            .setCancelable(false)
            .setPositiveButton("confirm") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                popupToOneLockPage()
            }
            .create()
    }

    private fun popupToOneLockPage() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment
        navHostFragment.navController.let { nav ->
            when (nav.currentDestination?.id) {
                in listOf(
//                    R.id.setting_Fragment,
//                    R.id.permission_request,
                    R.id.scan,
//                    R.id.connect_to_lock,
//                    R.id.add_admin_code
                ) -> {
//                    Timber.d("skip popup to home page.")
                }
                else -> nav.navigate(R.id.global_pop_inclusive_to_onelock)
            }
        }
    }

    fun launchDisconnectedDialog(cause: String = "") {
        if (disconnectDialog?.isShowing == false /*&& !isCheckingOccupiedScanning*/) {
            disconnectDialog?.show()
        }
    }


}


