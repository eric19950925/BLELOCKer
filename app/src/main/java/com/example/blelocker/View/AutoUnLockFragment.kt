package com.example.blelocker.View

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.navigation.Navigation
import com.example.blelocker.*
import kotlinx.android.synthetic.main.fragment_auto_unlock.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class AutoUnLockFragment: BaseFragment(){
    override fun getLayoutRes(): Int = R.layout.fragment_auto_unlock
    private lateinit var mSharedPreferences: SharedPreferences
    val geofenceViewModel by viewModel<GeofencingViewModel>()
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewHasCreated() {

        switch_geofence.isChecked = readGeoSP()

        geofenceViewModel.setTargetLocation()

        switch_geofence.setOnTouchListener { _, _ ->
            switch_geofence.isClickable = false
//            if(!switch_geofence.isChecked){
                sendNotification()
                //geofenceViewModel.setGeofencingClient()
//                    onSuccess = {
//                        switch_geofence.isClickable = true
//                        switch_geofence.isChecked = true
//                        mSharedPreferences = requireActivity().getSharedPreferences(MainActivity.DATA, 0)
                       //mSharedPreferences.edit()
                       //     .putBoolean("GEO", true)
//                            .apply()
//
//                    }
//                ) {
//                    true.also { switch_geofence.isClickable = it }
//                }
//            }else {
//                geofenceViewModel.RemoveGeofencing ()
//                    onSuccess = {
//                        switch_geofence.isClickable = true
//                        switch_geofence.isChecked = false
//                        mSharedPreferences = requireActivity().getSharedPreferences(MainActivity.DATA, 0)
//                        mSharedPreferences.edit()
//                            .putBoolean("GEO", false)
//                            .apply()
//
//                    }
//                ) {
//                    true.also { switch_geofence.isClickable = it }
//                }
//            }
            false
        }
    }

    override fun onBackPressed() {
        Navigation.findNavController(requireView()).navigate(R.id.back_to_setting)
    }
    private fun readGeoSP(): Boolean{
        mSharedPreferences = requireActivity().getSharedPreferences(MainActivity.DATA, 0)
        return mSharedPreferences.getBoolean("GEO", false)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendNotification() {
        val channel = NotificationChannel("Day15", "Day15", NotificationManager.IMPORTANCE_HIGH)
        val builder = Notification.Builder(
            requireActivity(),
            "Day15"
        )
        builder.setSmallIcon(R.drawable.ic_lock_main)
            .setContentTitle("觸發auto unlock")
            .setContentText("門已解鎖~歡迎回家!!")
            .setLargeIcon(BitmapFactory.decodeResource(resources,R.drawable.ic_auto_unlock))
            .setAutoCancel(true)
        val notification : Notification = builder.build()
        val manager = (requireActivity().getSystemService(NOTIFICATION_SERVICE)) as NotificationManager
        manager.createNotificationChannel(channel)
        manager.notify(0, notification)
    }
}