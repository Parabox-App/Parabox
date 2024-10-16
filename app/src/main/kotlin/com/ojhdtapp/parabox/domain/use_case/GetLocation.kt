package com.ojhdtapp.parabox.domain.use_case

import com.google.android.gms.maps.model.LatLng
import com.ojhdtapp.parabox.core.util.LocationUtil
import com.ojhdtapp.parabox.core.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLocation @Inject constructor(
    private val locationUtil: LocationUtil
) {
    operator fun invoke(): Flow<Resource<LatLng>> {
        return locationUtil.requestLocationUpdates()
    }
}