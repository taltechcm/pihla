
package ee.taltech.aireapplication.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class WebLink(
    val id: String,
    val uri: String,
    val isIframe: Boolean,

    val zoomFactor: Float?,
    val textZoom: Int?,
    val loadWithOverviewMode: Boolean,
    val useWideViewPort: Boolean,
    val layoutAlgorithm: String,
    val builtInZoomControls: Boolean,
    val displayZoomControls: Boolean,

    val webLinkName: String?,
    val webLinkDisplayName: String?
)
