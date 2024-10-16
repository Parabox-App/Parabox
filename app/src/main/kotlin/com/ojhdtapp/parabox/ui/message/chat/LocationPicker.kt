package com.ojhdtapp.parabox.ui.message.chat

import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.model.LatLng
import com.google.api.services.drive.model.File
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LocationPicker(
    modifier: Modifier = Modifier,
    state: MessagePageState.EditAreaState,
    onEvent: (MessagePageEvent) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cameraState = rememberCameraPositionState()
    LaunchedEffect(key1 = state.locationPickerState.firstLocationGotten) {
        if (state.locationPickerState.firstLocationGotten) {
            cameraState.centerOnLocation(state.locationPickerState.currentLocation)
            onEvent(MessagePageEvent.UpdateSelectedLocation(state.locationPickerState.currentLocation))
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 0.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(
                    isBuildingEnabled = false,
                    isIndoorEnabled = false,
                    isMyLocationEnabled = true,
                    mapType = MapType.NORMAL,
                    isTrafficEnabled = false
                ),
                onMapClick = {
                    onEvent(MessagePageEvent.UpdateSelectedLocation(it))
                    coroutineScope.launch {
                        cameraState.centerOnLocation(it)
                    }
                }
            ) {
                Marker(
                    state = MarkerState(position = state.locationPickerState.selectedLocation),
                    title = "选定的位置",
                    draggable = true,
                )
            }
        }
    }
}

private suspend fun CameraPositionState.centerOnLocation(
    location: LatLng
) = animate(
    update = CameraUpdateFactory.newLatLngZoom(
        location,
        15f
    ),
    durationMs = 1500
)

private fun LatLng.toLocation(): Location {
    return Location("google").also {
        it.latitude = latitude
        it.longitude = longitude
    }
}