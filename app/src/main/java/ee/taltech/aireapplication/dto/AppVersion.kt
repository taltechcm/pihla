package ee.taltech.aireapplication.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppVersion(
    val apkVersionCode: Long,
    val apkVersionName: String,
    val url: String,
    val uploadDT: String,
    val fileSize: Long,
)
