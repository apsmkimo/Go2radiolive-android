package com.apsmkimo.go2radiolive.data.remote

/** ISO 3166-1 alpha-2, or null when the value is missing or not two letters. */
internal fun normalizeRegion(raw: String?): String? {
    val code = raw?.trim()?.uppercase() ?: return null
    if (code.length != 2 || !code[0].isLetter() || !code[1].isLetter()) return null
    return code
}

/** Network country, then SIM country, then the locale country. */
internal fun preferredRegion(network: String?, sim: String?, locale: String?): String? {
    return normalizeRegion(network) ?: normalizeRegion(sim) ?: normalizeRegion(locale)
}

/**
 * Country code for the Local tab.
 * Taiwan stays Taiwan. An unknown region, or any other region with no stations, uses the US.
 */
internal fun localStationsCountry(detected: String?, primaryCount: Int): String {
    val region = normalizeRegion(detected) ?: return "US"
    if (region == "TW" || region == "US") return region
    if (primaryCount <= 0) return "US"
    return region
}
