package ee.taltech.aireapplication.locations

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import ee.taltech.aireapplication.domain.Location
import ee.taltech.aireapplication.dto.MapLocationSync
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.Locale

class LocationsRepository(private val context: Context, private val systemLocations: List<String>) {

    fun getMapLocations(mapId: String, locale: Locale): List<Location> {

        // try to load saved location from backend
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        if (!appSharedPrefs.contains(mapId)) return systemLocations.map { l ->
            Location(
                systemName = l,
                displayName = l,
            )
        }

        val savedLocationsJsonStr = appSharedPrefs.getString(mapId, "[]")
        val savedLocations: List<MapLocationSync> = Json.decodeFromString(savedLocationsJsonStr!!)

        val locations = savedLocations
            //.sortedByDescending { l -> l.sortPriority }
            .map { l ->
                Location(
                    systemName = l.mapLocation,
                    displayName = l.translations.find { t -> t.lang == locale.language }?.value
                        ?: l.mapLocation,
                    sortPriority = l.sortPriority,
                    patrolPriority = l.patrolPriority
                )
            }
            .sortedWith(compareBy({ l -> l.sortPriority }, { l -> l.displayName }))


        return locations
    }

    fun getPatrolLocations(mapId: String, locale: Locale): List<Location> {
        // try to load saved location from backend
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        if (!appSharedPrefs.contains(mapId)) return systemLocations.map { l ->
            Location(
                systemName = l,
                displayName = l
            )
        }

        val savedLocationsJsonStr = appSharedPrefs.getString(mapId, "[]")
        val savedLocations: List<MapLocationSync> = Json.decodeFromString(savedLocationsJsonStr!!)

        val locations = savedLocations
            .filter { l -> l.patrolPriority > 0 }
            //.sortedByDescending { l -> l.patrolPriority }
            .map { l ->
                Location(
                    systemName = l.mapLocation,
                    displayName = l.translations.find { t -> t.lang == locale.language }?.value
                        ?: l.mapLocation,
                    sortPriority = l.sortPriority,
                    patrolPriority = l.patrolPriority
                )
            }
            .sortedWith(compareBy({ l -> l.patrolPriority }, { l -> l.systemName }))

        return locations
    }

    fun getHomeBaseLocation(mapId: String, locale: Locale): Location? {
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        if (!appSharedPrefs.contains(mapId)) return systemLocations
            .filter { l -> l == "home base" }
            .map { l ->
                Location(
                    systemName = l,
                    displayName = l
                )
            }
            .first()

        val savedLocationsJsonStr = appSharedPrefs.getString(mapId, "[]")
        val savedLocations: List<MapLocationSync> = Json.decodeFromString(savedLocationsJsonStr!!)

        val location = savedLocations
            .filter { l -> l.mapLocation == "home base"  }
            //.sortedByDescending { l -> l.patrolPriority }
            .map { l ->
                Location(
                    systemName = l.mapLocation,
                    displayName = l.translations.find { t -> t.lang == locale.language }?.value
                        ?: l.mapLocation,
                    sortPriority = l.sortPriority,
                    patrolPriority = l.patrolPriority
                )
            }
            .firstOrNull()

        return location
    }

}