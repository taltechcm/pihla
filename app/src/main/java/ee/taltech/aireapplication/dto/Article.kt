package ee.taltech.aireapplication.dto

import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val title: String,
    val displayTitle: String,
    val plainText: String,
    val displayText: String,
)
