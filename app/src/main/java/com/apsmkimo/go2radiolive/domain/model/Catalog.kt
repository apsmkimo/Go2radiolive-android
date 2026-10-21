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

/**
 * Rankings, topic counts, and languages for one country scope.
 * A null ranking or language list means that request failed, so the card shows no count.
 * A missing [counts] key means the same for that topic.
 */
data class CategoryHub(
    val votes: List<RadioStation>? = null,
    val clicks: List<RadioStation>? = null,
    val trend: List<RadioStation>? = null,
    val counts: Map<String, CountResult> = emptyMap(),
    val languages: List<CatalogLanguage>? = null,
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
