package ee.taltech.aireapplication.locations

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import ee.taltech.aireapplication.R
import ee.taltech.aireapplication.domain.Location
import ee.taltech.aireapplication.dto.MapLocation2Sync
import ee.taltech.aireapplication.dto.MapLocationSync
import ee.taltech.aireapplication.helpers.SettingsRepository
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.text.DecimalFormat
import java.util.Locale

class LocationsRepository(private val context: Context, private val systemLocations: List<String>) {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private fun locationsInUpper(): Boolean {
        return SettingsRepository.getBoolean(
            context,
            "locationsInUpperCase",
            context.resources.getBoolean(R.bool.locationsInUpperCase)
        )
    }


    fun getMapLocations(mapId: String, floorName: String, locale: Locale): List<Location> {
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
        val savedLocations: List<MapLocation2Sync> = Json.decodeFromString(savedLocationsJsonStr!!)

        val locations = savedLocations
            .filter { l -> l.floorName == floorName }
            //.sortedByDescending { l -> l.sortPriority }
            .map { l ->
                Location(
                    systemName = l.mapLocation,
                    displayName = (l.translations.find { t -> t.lang == locale.language }?.value
                        ?: l.mapLocation).let { if (locationsInUpper()) it.uppercase() else it},
                    sortPriority = l.sortPriority,
                    patrolPriority = l.patrolPriority
                )
            }
            .sortedWith(compareBy({ l -> l.sortPriority }, { l -> l.displayName }))


        return locations
    }

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

    @OptIn(ExperimentalSerializationApi::class)
    fun getPatrolLocations(mapId: String, floorName: String, locale: Locale): List<Location> {
        // try to load saved location from backend
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        if (!appSharedPrefs.contains(mapId)) return systemLocations.map { l ->
            Location(
                systemName = l,
                displayName = l
            )
        }

        var savedLocations: List<MapLocation2Sync> = listOf()

        val savedLocationsJsonStr = appSharedPrefs.getString(mapId, "[]")
        try {
            savedLocations = Json.decodeFromString(savedLocationsJsonStr!!)

        } catch (e: kotlinx.serialization.MissingFieldException) {
            Log.d(TAG, "MissingFieldException: $e")
        }

        val locations = savedLocations
            .filter { l -> l.patrolPriority > 0 && l.floorName == floorName }
            //.sortedByDescending { l -> l.patrolPriority }
            .map { l ->
                Location(
                    systemName = l.mapLocation,
                    displayName = (l.translations.find { t -> t.lang == locale.language }?.value
                        ?: l.mapLocation).let { if (locationsInUpper()) it.uppercase() else it},
                    sortPriority = l.sortPriority,
                    patrolPriority = l.patrolPriority
                )
            }
            .sortedWith(compareBy({ l -> l.patrolPriority }, { l -> l.systemName }))

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

    fun getHomeBaseLocation(mapId: String, floorName: String, locale: Locale): Location? {
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
            .firstOrNull()

        val savedLocationsJsonStr = appSharedPrefs.getString(mapId, "[]")
        val savedLocations: List<MapLocation2Sync> = Json.decodeFromString(savedLocationsJsonStr!!)

        val location = savedLocations
            .filter { l -> l.mapLocation == "home base" && l.floorName == floorName }
            //.sortedByDescending { l -> l.patrolPriority }
            .map { l ->
                Location(
                    systemName = l.mapLocation,
                    displayName = (l.translations.find { t -> t.lang == locale.language }?.value
                        ?: l.mapLocation).let { if (locationsInUpper()) it.uppercase() else it},
                    sortPriority = l.sortPriority,
                    patrolPriority = l.patrolPriority
                )
            }
            .firstOrNull()

        return location
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
            .filter { l -> l.mapLocation == "home base" }
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