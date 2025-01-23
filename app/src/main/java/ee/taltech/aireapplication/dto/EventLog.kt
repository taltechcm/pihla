package ee.taltech.aireapplication.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventLog(
    val androidIdCode: String,
    val appLaunch: String,
    val mapIdCode: String,
    val mapName: String,
    val appName: String,

    val tag: String,

    val message: String? = null,
    val intValue: Int? = null,
    val doubleValue: Double? = null,
)
