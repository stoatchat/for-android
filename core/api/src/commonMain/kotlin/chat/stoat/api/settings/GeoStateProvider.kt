package chat.stoat.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.stoat.api.routes.microservices.geo.GeoResponse

object GeoStateProvider {
    var geoState by mutableStateOf<GeoResponse?>(null)
        private set

    fun updateGeoState(newGeoState: GeoResponse?) {
        checkNotNull(newGeoState) { "You shall not unset this value" }
        check(if (geoState?.isAgeRestrictedGeo == true) newGeoState.isAgeRestrictedGeo else true) { "You shall not apply a laxer value" }

        geoState = newGeoState
    }
}