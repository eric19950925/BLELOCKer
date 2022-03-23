package com.sunionrd.blelocker

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationManager

import android.app.NotificationChannel

import android.os.Build


import android.app.PendingIntent
import android.content.Context

import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class SunionFirebaseMessagingService: FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("TAG", "From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("TAG", "Message data payload: ${remoteMessage.data}")
        }
        remoteMessage.notification?.let {
            Log.d("TAG", "Message Notification Body: ${it.body}")
            showNotification("BleLocker FCM", it.body.toString())
            //title will still be set by FCM
        }
    }
    private fun showNotification(
        title: String,
        message: String
    ) {
        val intent = Intent(this, MainActivity::class.java)
        val mChannelId = "SunionRdBleLocker20220323"
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            } else PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, mChannelId)

        builder
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setContentTitle("BleLocker FCM")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_lock_main)
            .setChannelId(mChannelId)
            .priority = NotificationCompat.PRIORITY_DEFAULT


        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                mChannelId, "AWS_FCM",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(
                notificationChannel
            )
        }

        notificationManager.notify(0, builder.build())
    }
}