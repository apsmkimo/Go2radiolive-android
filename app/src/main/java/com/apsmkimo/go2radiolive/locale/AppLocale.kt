package com.apsmkimo.go2radiolive.locale

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.media3.session.DefaultMediaNotificationProvider
import com.apsmkimo.go2radiolive.R
import java.util.concurrent.CopyOnWriteArrayList

/** Applies and reads the per-app locale, including notification strings outside an Activity. */
internal object AppLocale {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun selectedTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppLanguage.SYSTEM
        val locale = locales.get(0) ?: return AppLanguage.SYSTEM
        return AppLanguage.selection(locale.language, locale.country, locale.script)
    }

    fun apply(tag: String) {
        if (selectedTag() == tag) return
        val locales = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
        listeners.forEach { it.invoke() }
    }

    /** Loads the stored locale so a process that starts in a service still sees it. */
    fun warm() {
        AppCompatDelegate.getApplicationLocales()
    }
}

internal fun Context.withAppLocale(): Context {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) return this
    val localeList = locales.unwrap() as? LocaleList ?: return this
    val config = Configuration(resources.configuration)
    config.setLocales(localeList)
    return createConfigurationContext(config)
}

internal fun Context.refreshLocalizedChannels() {
    val localized = withAppLocale()
    val manager = getSystemService(NotificationManager::class.java) ?: return
    renameChannel(
        manager,
        DefaultMediaNotificationProvider.DEFAULT_CHANNEL_ID,
        localized.getString(R.string.playback_channel),
    )
    renameChannel(manager, ALARM_CHANNEL_ID, localized.getString(R.string.alarm_channel))
}

private fun renameChannel(manager: NotificationManager, id: String, name: String) {
    val existing = manager.getNotificationChannel(id) ?: return
    existing.name = name
    manager.createNotificationChannel(existing)
}

internal const val ALARM_CHANNEL_ID = "go2_wake_alarm"
