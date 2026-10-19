package com.apsmkimo.go2radiolive.di

import android.content.Context
import android.telephony.TelephonyManager
import com.apsmkimo.go2radiolive.alarm.WakeAlarmRepository
import com.apsmkimo.go2radiolive.audio.AudioSettingsRepository
import com.apsmkimo.go2radiolive.data.remote.preferredRegion
import com.apsmkimo.go2radiolive.data.local.FavoriteLocalDataSource
import com.apsmkimo.go2radiolive.data.local.PodcastRegionRepository
import com.apsmkimo.go2radiolive.data.remote.RadioBrowserApi
import com.apsmkimo.go2radiolive.data.repository.FavoriteRepositoryImpl
import com.apsmkimo.go2radiolive.data.repository.PodcastRepositoryImpl
import com.apsmkimo.go2radiolive.data.repository.StationRepositoryImpl
import com.apsmkimo.go2radiolive.domain.repository.FavoriteRepository
import com.apsmkimo.go2radiolive.domain.repository.PodcastRepository
import com.apsmkimo.go2radiolive.domain.repository.StationRepository
import com.apsmkimo.go2radiolive.domain.usecase.GetCategoriesUseCase
import com.apsmkimo.go2radiolive.domain.usecase.GetCountriesUseCase
import com.apsmkimo.go2radiolive.domain.usecase.GetPodcastEpisodesUseCase
import com.apsmkimo.go2radiolive.domain.usecase.GetPodcastShowsUseCase
import com.apsmkimo.go2radiolive.domain.usecase.GetStationsUseCase
import com.apsmkimo.go2radiolive.domain.usecase.ObserveFavoritesUseCase
import com.apsmkimo.go2radiolive.domain.usecase.ResolveStreamUrlUseCase
import com.apsmkimo.go2radiolive.domain.usecase.ToggleFavoriteUseCase
import com.apsmkimo.go2radiolive.presentation.player.RadioPlaybackService
import com.apsmkimo.go2radiolive.presentation.player.RadioPlayerController
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Manual composition root (Clean Architecture).
 * Presentation depends on use cases; data implements domain ports.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // SMCPKG_SUPPORT>>>Cursor017
    init {
        AudioSettingsRepository.start(appContext)
    }
    // SMCPKG_SUPPORT<<<Cursor017

    val gson: Gson = Gson()

    // SMCPKG_SUPPORT>>>Cursor098
    val wakeAlarmRepository = WakeAlarmRepository(appContext, gson)
    // SMCPKG_SUPPORT<<<Cursor098

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", RadioPlaybackService.USER_AGENT)
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        )
        .build()

    private val radioBrowserApi: RadioBrowserApi = Retrofit.Builder()
        .baseUrl(RADIO_BROWSER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(RadioBrowserApi::class.java)

    private val podcastClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", RadioPlaybackService.USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .build()

    // SMCPKG_SUPPORT>>>Cursor092
    val podcastRegionRepository = PodcastRegionRepository(appContext)
    // SMCPKG_SUPPORT<<<Cursor092

    val stationRepository: StationRepository = StationRepositoryImpl(radioBrowserApi)
    val podcastRepository: PodcastRepository = PodcastRepositoryImpl(podcastClient, gson)
    val favoriteRepository: FavoriteRepository = FavoriteRepositoryImpl(
        FavoriteLocalDataSource(appContext, gson),
    )

    val getStationsUseCase = GetStationsUseCase(stationRepository)
    val getCategoriesUseCase = GetCategoriesUseCase(stationRepository)
    val getCountriesUseCase = GetCountriesUseCase(stationRepository)
    val getPodcastShowsUseCase = GetPodcastShowsUseCase(podcastRepository)
    val getPodcastEpisodesUseCase = GetPodcastEpisodesUseCase(podcastRepository)

    fun deviceRegion(): String {
        // SMCPKG_SUPPORT>>>Cursor075
        val telephony = runCatching {
            appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        }.getOrNull()
        val network = runCatching { telephony?.networkCountryIso }.getOrNull()
        val sim = runCatching { telephony?.simCountryIso }.getOrNull()
        return preferredRegion(network, sim, Locale.getDefault().country).orEmpty()
        // SMCPKG_SUPPORT<<<Cursor075
    }
    val observeFavoritesUseCase = ObserveFavoritesUseCase(favoriteRepository)
    val toggleFavoriteUseCase = ToggleFavoriteUseCase(favoriteRepository)
    val resolveStreamUrlUseCase = ResolveStreamUrlUseCase(stationRepository)

    val playerController = RadioPlayerController(appContext, gson)

    companion object {
        const val RADIO_BROWSER_BASE_URL = "https://de1.api.radio-browser.info/"
        // SMCPKG_SUPPORT>>>Cursor076
        // Debug and release ad ids live in the build type (manifestPlaceholders / BuildConfig).
        // const val ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
        // const val ADMOB_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        // SMCPKG_SUPPORT<<<Cursor076
    }
}
