package com.sunionrd.blelocker

import android.animation.AnimatorInflater
import android.animation.AnimatorInflater.loadAnimator
import android.app.PendingIntent
import android.content.DialogInterface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.util.*
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout

import androidx.palette.graphics.Palette
import com.google.android.material.navigation.NavigationView
import androidx.palette.graphics.Palette.PaletteAsyncListener
import com.sunionrd.blelocker.Exception.drawableToBitmap
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.Transformation
import kotlinx.coroutines.delay
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.util.Log
import android.widget.Toast
import androidx.vectordrawable.graphics.drawable.AnimatorInflaterCompat.loadAnimator
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging


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
    private lateinit var tvHelp: TextView
    private lateinit var navView: NavigationView
    private lateinit var ivUser: ImageView
    private lateinit var mLoadingView: ConstraintLayout
    private lateinit var mPoint1: CardView
    private lateinit var mPoint2: CardView
    private lateinit var mPoint3: CardView
    private lateinit var animator: ObjectAnimator
    private lateinit var animator2: ObjectAnimator
    private lateinit var animator3: ObjectAnimator


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getSupportActionBar()?.hide()
        initDisconnectedDialog()
        drawerLayout = findViewById(R.id.drawer_layout)
        tvUserName = findViewById(R.id.tvUserName)
        tvHelp = findViewById(R.id.tvHelp)
        navView = findViewById(R.id.nav_view)
        ivUser = findViewById(R.id.ivUser)
        mLoadingView = findViewById(R.id.loadingView)
        mPoint1 = findViewById(R.id.cardOne)
        mPoint2 = findViewById(R.id.cardTwo)
        mPoint3 = findViewById(R.id.cardThree)

    }

    fun getFCMtoken(callback:(token: String) -> Unit){
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("TAG", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            callback(task.result)
        })
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

    fun showLoadingView(){
        mLoadingView.visibility = View.VISIBLE


//        animator = AnimatorInflater.loadAnimator(this, R.animator.alpha) as ObjectAnimator
//        animator.target = mPoint1

        animator = ObjectAnimator.ofFloat(mPoint1, "alpha", 1f, 0f)
        animator.repeatMode = ObjectAnimator.REVERSE
        animator.duration = 800
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.start()
        animator2 = ObjectAnimator.ofFloat(mPoint2, "alpha", 1f, 0f)
        animator2.repeatCount = ObjectAnimator.INFINITE
        animator2.duration = 800
        animator2.repeatMode = ObjectAnimator.REVERSE
        animator2.startDelay = 200
        animator2.start()
        animator3 = ObjectAnimator.ofFloat(mPoint3, "alpha", 1f, 0f)
        animator3.repeatCount = ObjectAnimator.INFINITE
        animator3.duration = 800
        animator3.repeatMode = ObjectAnimator.REVERSE
        animator3.startDelay = 400
        animator3.start()

    }

    fun hideLoadingView(){
        mLoadingView.visibility = View.GONE
        animator.cancel()
        animator2.cancel()
        animator3.cancel()
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
                    tvHelp.setTextColor(vibrant.rgb)
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


