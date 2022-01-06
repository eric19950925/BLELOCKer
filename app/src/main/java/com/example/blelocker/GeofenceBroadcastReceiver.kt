package com.example.blelocker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver: BroadcastReceiver(){
    companion object {
        const val AUTO_UNLOCK_CHANNEL_ID = "auto_unlock_channel_id"

        /**
         * The identifier for the notification displayed for geofence service.
         */
        const val AUTO_UNLOCK_NOTIFICATION_ID = 6212021
    }
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
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
        geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

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
            sendNotification(context, geofenceTransitionDetails)
            Log.i("TAG", geofenceTransitionDetails)
        } else {
            // Log the error.
            Log.e("TAG", "error")
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
    private fun sendNotification(
        context: Context,
        notificationDetails: String
    ) {
        // Get an instance of the Notification manager
        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create an explicit content Intent that starts the main Activity.
        val notificationIntent = Intent(
            context,
            MainActivity::class.java
        )

        // Construct a task stack.
        val stackBuilder = TaskStackBuilder.create(context)

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity::class.java)

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent)

        // Get a PendingIntent containing the entire back stack.
        val notificationPendingIntent =
            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        // Get a notification builder that's compatible with platform versions >= 4
        val builder = NotificationCompat.Builder(
            context,
            AUTO_UNLOCK_CHANNEL_ID
        )

        // The PendingIntent to launch activity.
        val activityIntent = PendingIntent.getActivity(
            context, 0,
            Intent(
                context,
                MainActivity::class.java
            ),
            0
        )
        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_lock_main)
            // to decode the Bitmap.
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_lock_main
                )
            )
            .setContentTitle(notificationDetails)
            .setContentText("geofence_transition_notification_text")
            .setContentIntent(notificationPendingIntent)
            .addAction(R.drawable.ic_lock_main, "Launch iKey", activityIntent)

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(AUTO_UNLOCK_CHANNEL_ID) // Channel ID
        }

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true)

        // Issue the notification
        // Update notification content if running as a foreground service.
        mNotificationManager.notify(AUTO_UNLOCK_NOTIFICATION_ID, builder.build())
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