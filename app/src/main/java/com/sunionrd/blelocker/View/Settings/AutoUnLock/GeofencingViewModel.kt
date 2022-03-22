package com.sunionrd.blelocker.View.Settings.AutoUnLock

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.sunionrd.blelocker.MainActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices


class GeofencingViewModel(val context: Context): ViewModel() {
    private lateinit var geofencePendingIntent: PendingIntent

    init {
//        geofencingClient = LocationServices.getGeofencingClient(context)
    }
    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission", "UnspecifiedImmutableFlag")
    fun setGeofencing(success: () -> Unit,
                            failure: (error: Exception) -> Unit){
        val geofence = Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId("EricShihSunionTest")

            // Set the circular region of this geofence.
            .setCircularRegion(
                25.056475621863527, 121.47266461696209,
                MainActivity.GEOFENCE_RADIUS_IN_METERS
            )

            // Set the expiration duration of the geofence. This geofence gets automatically
            // removed after this period of time.
            .setExpirationDuration(Geofence.NEVER_EXPIRE)

            // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)

            // Create the geofence.
            .build()
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        geofencePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            MainActivity.MY_PENDING_INTENT_FLAG
        )
        val geofencingClient = LocationServices.getGeofencingClient(context)
        if (geofence != null && ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(getGeofencingRequest(geofence), geofencePendingIntent)
                .addOnSuccessListener {
//                    viewModel.saveAllReminder(viewModel.getAllReminder() + myReminder)
                    success()
                    Log.d("TAG","success set geofence ")
                }
                .addOnFailureListener {
                    failure(it)
                    Log.d("TAG",it.toString())
                }
        }
    }

    private fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(listOf(geofence))
        }.build()
    }

    fun removeGeofencing(success: () -> Unit,
                         failure: (error: Exception) -> Unit) {
        val geofencingClient = LocationServices.getGeofencingClient(context)
        geofencingClient.removeGeofences(listOf("EricShihSunionTest"))
            .addOnSuccessListener {
                // Geofences removed
                // ...
                Toast.makeText(context, "解除成功", Toast.LENGTH_LONG).show()
                success.invoke()
            }
            .addOnFailureListener {
                // Failed to remove geofences
                // ...
                Toast.makeText(context, "解除失敗", Toast.LENGTH_LONG).show()
                failure.invoke(it)
            }
    }
}