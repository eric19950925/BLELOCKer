package com.example.blelocker

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import com.example.blelocker.MainActivity.Companion.GEOFENCE_RADIUS_IN_METERS
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task

class GeofencingViewModel(val context: Context): ViewModel(), OnCompleteListener<Void> {
    private var geofencingClient: GeofencingClient? = null
    private var geofenceList: List<Geofence>? = null
    private val geofencePendingIntent : PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    init {
        geofencingClient = LocationServices.getGeofencingClient(context)
    }
    fun setTargetLocation(){
        geofenceList = listOf(
            Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId("EricShihSunionTest")

                // Set the circular region of this geofence.
                .setCircularRegion(
                    25.056475621863527, 121.47266461696209,
                    GEOFENCE_RADIUS_IN_METERS
                )

                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration(Geofence.NEVER_EXPIRE)

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)

                // Create the geofence.
                .build())
    }
    @SuppressLint("MissingPermission")
    fun setGeofencingClient(){

        geofencingClient?.addGeofences(getGeofencingRequest(), geofencePendingIntent)?.addOnCompleteListener(this)
//            addOnSuccessListener {
                // Geofences added
                // ...
//                Toast.makeText(context, "設定成功", Toast.LENGTH_LONG).show()
//                onSuccess.invoke()
//            }
//            addOnFailureListener {
                // Failed to add geofences
                // ...
//                Toast.makeText(context, "設定失敗", Toast.LENGTH_LONG).show()
//                onFailure.invoke()
//            }
//        }
    }

    private fun getGeofencingRequest(): GeofencingRequest {

        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList?:return@apply)
        }.build()
    }

    fun RemoveGeofencing() {
        geofencingClient?.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removed
                // ...
                Toast.makeText(context, "解除成功", Toast.LENGTH_LONG).show()
//                onSuccess.invoke()
            }
            addOnFailureListener {
                // Failed to remove geofences
                // ...
                Toast.makeText(context, "解除失敗", Toast.LENGTH_LONG).show()
//                onFailure.invoke()
            }
        }
    }

    override fun onComplete(task: Task<Void>) {
//        viewModel.pendingGeofenceOperationResult.value = Event.success(task.isSuccessful to pendingGeofenceTask)
        if (task.isSuccessful) {
            Toast.makeText(context, "設定成功", Toast.LENGTH_LONG).show()
//            Timber.d("PendingGeofenceTask isSuccessful")
//            FirebaseAnalytics.getInstance(this).logEvent("geo_operation_success", null)
//
//            pendingGeofenceTask = PendingGeofenceTask.NONE
        } else {
            Toast.makeText(context, "設定失敗", Toast.LENGTH_LONG).show()
            // Get the status code for the error and log it using a user-friendly message.
//            FirebaseAnalytics.getInstance(this).logEvent("geo_operation_error", null)
//
//            viewModel.pendingGeofenceOperationResult.value = Event.error(GeofenceErrorMessages.getErrorString(this, task.exception))
//            Timber.e(GeofenceErrorMessages.getErrorString(this, task.exception))
//            Toast.makeText(this, "Geofence operation error", Toast.LENGTH_SHORT).show()
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
//                Toast.makeText(this, "Please make sure Location permission is Allow all the time", Toast.LENGTH_SHORT).show()
//                FirebaseAnalytics.getInstance(this).logEvent("geo_operation_permission_error", null)
//            }
//            pendingGeofenceTask = PendingGeofenceTask.NONE
        }
    }
}