package ee.taltech.aireapplication.dto

import kotlinx.serialization.Serializable

@Serializable
data class MapSync(
    val mapIdCode: String,
    val mapName: String,
    val mapLocations: List<String>
)