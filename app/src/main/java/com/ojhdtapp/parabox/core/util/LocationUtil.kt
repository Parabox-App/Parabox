package com.ojhdtapp.parabox.core.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.os.Build
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class LocationUtil @Inject constructor(
    private val context: Context,
    private val locationClient: FusedLocationProviderClient
) {
    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(): Flow<Resource<LatLng>> = callbackFlow {
        if (!hasLocationPermission(context)) {
            trySend(Resource.Error("no permission"))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(10000L)
            .setIntervalMillis(10000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.lastOrNull()?.let {
                    trySend(Resource.Success(LatLng(it.latitude, it.longitude)))
                }
            }
        }
        locationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
        awaitClose {
            locationClient.removeLocationUpdates(locationCallback)
        }
    }

    fun getAddressFromLatLng(location: LatLng): Flow<Resource<String>> = callbackFlow {
        val geoCoder = Geocoder(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geoCoder.getFromLocation(
                location.latitude, location.longitude, 1
            ) {
                if (it.isNotEmpty()) {
                    trySend(Resource.Success(it.first().getAddress()))
                } else {
                    trySend(Resource.Error("no result"))
                }
            }
        } else {
            val res = geoCoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!res.isNullOrEmpty()) {
                trySend(Resource.Success(res.first().getAddress()))
            } else {
                trySend(Resource.Error("no result"))
            }
        }

        awaitClose {}
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun Address.getAddress(): String {
    return buildString {
        append(getAddressLine(0))
        append(", ")
        getAddressLine(1)?.takeIf { !it.contains("null") }?.let {
            append(it)
            append(", ")
        }
        append(locality)
        append(", ")
        append(adminArea)
        append(", ")
        append(countryName)
    }
}
