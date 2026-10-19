package com.apsmkimo.go2radiolive.data.remote

internal fun cacheControlHeader(bypassCache: Boolean): String? = if (bypassCache) "no-cache" else null

internal fun cacheBustToken(bypassCache: Boolean): Long? = if (bypassCache) System.currentTimeMillis() else null
