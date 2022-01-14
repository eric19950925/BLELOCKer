package com.example.blelocker.View.Settings.AutoUnLock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.blelocker.R
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent?:return)
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e("TAG", errorMessage)
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER){
            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            // Get the transition details as a String.
            val geofenceTransitionDetails = getGeofenceTransitionDetails(
                context?:return,
                geofenceTransition,
                triggeringGeofences
            )

            // Send notification and log the transition details.
//            sendNotification(context, geofenceTransitionDetails)
            //start ble scan
            val serviceIntent = Intent(context, AutoUnlockService::class.java)
            context.startService(serviceIntent)
            Log.i("TAG", geofenceTransitionDetails)

        }else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val geofenceTransitionDetails = getGeofenceTransitionDetails(
                context?:return,
                geofenceTransition,
                triggeringGeofences
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sendNotification(context, geofenceTransitionDetails)
            }
            Log.i("TAG", geofenceTransitionDetails)
        } else {
            // Log the strange error.//todo
            Log.e("TAG", GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode))
        }

    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition The ID of the geofence transition.
     * @param triggeringGeofences The geofence(s) triggered.
     * @return The transition details formatted as String.
     */
    private fun getGeofenceTransitionDetails(
        context: Context,
        geofenceTransition: Int,
        triggeringGeofences: List<Geofence>
    ): String {
        val geofenceTransitionString =
            getTransitionString(context, geofenceTransition)

        // Get the Ids of each geofence that was triggered.
        val triggeringGeofencesIdsList = ArrayList<String?>()
        for (geofence in triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.requestId)
        }
//        val triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList)
        return geofenceTransitionString
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the HomeActivity.
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendNotification(
        context: Context,
        notificationDetails: String
    ) {

        val channel = NotificationChannel("Day15", "Day15", NotificationManager.IMPORTANCE_HIGH)

        val builder = Notification.Builder(
            context,
            "Day15"
        )
        builder.setSmallIcon(R.drawable.ic_lock_main)
            .setContentTitle("BTLocker - Auto Unlock")
            .setContentText(notificationDetails)
            .setLargeIcon(BitmapFactory.decodeResource(Resources.getSystem(),
                R.drawable.ic_auto_unlock
            ))
            .setAutoCancel(true)
        val notification : Notification = builder.build()
        val manager = (context.getSystemService(Context.NOTIFICATION_SERVICE)) as NotificationManager
        manager.createNotificationChannel(channel)
        manager.notify(0, notification)

    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType A transition type constant defined in Geofence
     * @return A String indicating the type of transition
     */
    private fun getTransitionString(
        context: Context,
        transitionType: Int
    ): String {
        return when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "geofence_transition_entered"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "geofence_transition_exited"
            else -> "unknown_geofence_transition"
        }
    }
}