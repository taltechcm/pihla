package ee.taltech.aireapplication.dto

import kotlinx.serialization.Serializable

@Serializable
data class Map2Sync(
    val mapIdCode: String,
    val mapName: String,
    val mapFloors: List<FloorSync>,
)
{
    @Serializable
    data class FloorSync(
        val floorName: String,
        val mapLocations: List<String>
    )
}

