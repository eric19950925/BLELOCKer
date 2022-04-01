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
import org.json.JSONObject

class SunionFirebaseMessagingService: FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("TAG", "From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            //Log.d("TAG", "payload.toString: ${remoteMessage.data}")
            //remoteMessage.data's data structure is Map not jsonObject, should not change to string
            //todo : Proguard maybe cause crash at here.
            val params = remoteMessage.data
            val notifyObject = JSONObject(params as Map<*, *>?)
            Log.d("TAG", "Message data payload: $notifyObject")
            val msg = notifyObject.getString("message")
            showNotification("BleLocker FCM from AWS",msg)
        }
        remoteMessage.notification?.let {
            Log.d("TAG", "Message Notification Body: ${it.body }")
            showNotification("BleLocker FCM from Firebase", it.body.toString())
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
            .setContentTitle(title)
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
        notificationManager.notify(System.currentTimeMillis().toString(),0, builder.build())
    }
}