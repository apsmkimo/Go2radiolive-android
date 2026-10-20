package com.apsmkimo.go2radiolive.domain.model

/** How a category station list is ordered. Rankings keep their own order. */
enum class CatalogSort {
    CLICKS,
    VOTES,
    NAME,
}

data class CountResult(
    val count: Int = 0,
    val capped: Boolean = false,
)

data class CatalogLanguage(
    val name: String = "",
    val stationCount: Int = 0,
)

/** Rankings, topic counts, and languages for one country scope. */
data class CategoryHub(
    val votes: List<RadioStation> = emptyList(),
    val clicks: List<RadioStation> = emptyList(),
    val trend: List<RadioStation> = emptyList(),
    val counts: Map<String, CountResult> = emptyMap(),
    val languages: List<CatalogLanguage> = emptyList(),
    val complete: Boolean = true,
)

/** Stations for one category, plus a global supplement when the country list is short. */
data class CatalogPage(
    val stations: List<RadioStation> = emptyList(),
    val others: List<RadioStation> = emptyList(),
    val complete: Boolean = true,
)

enum class CatalogLevel {
    HUB,
    GROUP,
    LIST,
}
