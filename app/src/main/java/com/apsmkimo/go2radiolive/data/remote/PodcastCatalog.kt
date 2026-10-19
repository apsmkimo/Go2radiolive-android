package com.apsmkimo.go2radiolive.data.remote

import com.apsmkimo.go2radiolive.domain.model.PodcastShow

/** Apple storefront code. Blank or invalid regions fall back to the US chart. */
internal fun podcastStorefront(region: String?): String {
    val code = region?.trim()?.lowercase() ?: return "us"
    if (code.length != 2 || !code[0].isLetter() || !code[1].isLetter()) return "us"
    return code
}

/**
 * A non-US storefront uses the US chart only when its catalog is empty.
 * HTTP and other failures stay failures so the UI can show an error.
 * The US storefront reports its own result.
 */
internal fun usesUsPodcastFallback(storefront: String, chartFailed: Boolean, catalogEmpty: Boolean): Boolean {
    // SMCPKG_SUPPORT>>>Cursor090
    // if (storefront == "us") return false
    // return chartFailed || catalogEmpty
    if (storefront == "us" || chartFailed) return false
    return catalogEmpty
    // SMCPKG_SUPPORT<<<Cursor090
}

internal fun dedupePodcasts(groups: List<List<PodcastShow>>, limit: Int = 200): List<PodcastShow> {
    val seen = HashSet<String>()
    val out = ArrayList<PodcastShow>(limit.coerceAtMost(200))
    for (group in groups) {
        for (show in group) {
            if (show.id.isBlank() || show.name.isBlank()) continue
            if (!seen.add(show.id)) continue
            out.add(show)
            if (out.size >= limit) return out
        }
    }
    return out
}
