package com.example.blelocker

import android.app.PendingIntent
import android.content.DialogInterface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.util.*
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.ImageView

import androidx.palette.graphics.Palette
import com.google.android.material.navigation.NavigationView
import androidx.palette.graphics.Palette.PaletteAsyncListener
import com.example.blelocker.Exception.drawableToBitmap


class MainActivity : AppCompatActivity() {

    //常數設置
    companion object {
        const val CIPHER_MODE = "AES/ECB/NoPadding"
        const val BARCODE_KEY = "SoftChefSunion65"
        val NOTIFICATION_CHARACTERISTIC: UUID = UUID.fromString("de915dce-3539-61ea-ade7-d44a2237601f")
        val SUNION_SERVICE_UUID: UUID = UUID.fromString("fc3d8cf8-4ddc-7ade-1dd9-2497851131d7")
        const val DATA = "DATA"
        const val CURRENT_LOCK_MAC = "CURRENT_LOCK_MAC"
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        var MY_PENDING_INTENT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
    private var disconnectDialog: AlertDialog? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var tvUserName: TextView
    private lateinit var navView: NavigationView
    private lateinit var ivUser: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getSupportActionBar()?.hide()
        initDisconnectedDialog()
        drawerLayout = findViewById(R.id.drawer_layout)
        tvUserName = findViewById(R.id.tvUserName)
        navView = findViewById(R.id.nav_view)
        ivUser = findViewById(R.id.ivUser)
    }

    fun controlDrawer(){
        if (this.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            this.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    fun setUserName(name: String){
        tvUserName.text = name
    }

    fun setPaletteColor(id: Int){
        ivUser.setImageResource(id)
        val mBitmap = drawableToBitmap(this.getDrawable(id)?:return)
        Palette.from(mBitmap).maximumColorCount(12)
            .generate(PaletteAsyncListener { palette ->
                // Get the "vibrant" color swatch based on the bitmap
                val vibrant = palette!!.lightVibrantSwatch
                if (vibrant != null) {
                    // Set the background color of a layout based on the vibrant color
                    navView.setBackgroundColor(vibrant.rgb)
                    // Update the title TextView with the proper text color
                    tvUserName.setTextColor(vibrant.rgb)
                }
            })
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

    fun backToHome() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment
        navHostFragment.navController.let { nav ->
            nav.navigate(R.id.global_pop_inclusive_to_alllock)
        }
    }

    fun launchDisconnectedDialog(cause: String = "") {
        if (disconnectDialog?.isShowing == false /*&& !isCheckingOccupiedScanning*/) {
            disconnectDialog?.show()
        }
    }


}


