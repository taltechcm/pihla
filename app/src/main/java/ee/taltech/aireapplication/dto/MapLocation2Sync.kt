package ee.taltech.aireapplication.dto

import kotlinx.serialization.Serializable

@Serializable
data class MapLocation2Sync(
    val floorName: String,
    val mapLocation: String,
    val sortPriority: Int,
    val patrolPriority: Int,
    val translations: List<Translation>
) {
    @Serializable
    data class Translation(
        val lang: String,
        val value: String
    )
}
