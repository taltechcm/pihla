package ee.taltech.aireapplication.dto

import kotlinx.serialization.Serializable

@Serializable
data class NewsItem(
    val category: String,
    val title: String,
    val description: String,
    val pubDate: String,
    val link: String,
    val shortLink: String,
)
