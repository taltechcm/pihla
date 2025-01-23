package ee.taltech.aireapplication.domain

data class Location(
    val systemName: String,
    val displayName: String,
    val sortPriority: Int = 0,
    val patrolPriority: Int = 0,
)
