package com.apsmkimo.go2radiolive.data.remote

import com.apsmkimo.go2radiolive.domain.station.PlaylistBodySource
import com.apsmkimo.go2radiolive.domain.station.PlaylistParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/** Reads a station's own playlist. The body is capped so a stream is not treated as text. */
class OkHttpPlaylistClient(
    private val userAgent: String,
) : PlaylistBodySource {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override fun fetch(url: String, timeoutMs: Long): String? {
        if (!PlaylistParser.isHttp(url)) return null
        val budget = timeoutMs.coerceIn(500L, 6_000L)
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "audio/x-mpegurl, audio/x-scpls, application/vnd.apple.mpegurl, text/plain, */*")
                .build()
            val call = client.newCall(request)
            call.timeout().timeout(budget, TimeUnit.MILLISECONDS)
            call.execute().use { response ->
                if (!response.isSuccessful) return null
                val stream = response.body?.byteStream() ?: return null
                stream.use { input ->
                    val buffer = ByteArrayOutputStream()
                    val tmp = ByteArray(4096)
                    var total = 0
                    while (total < MAX_BYTES) {
                        val read = input.read(tmp)
                        if (read < 0) break
                        buffer.write(tmp, 0, read)
                        total += read
                    }
                    buffer.toString(Charsets.UTF_8.name())
                }
            }
        }.getOrNull()
    }

    companion object {
        private const val MAX_BYTES = 256 * 1024
    }
}
