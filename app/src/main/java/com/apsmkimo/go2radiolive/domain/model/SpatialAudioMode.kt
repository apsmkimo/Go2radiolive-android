package com.apsmkimo.go2radiolive.domain.model

/**
 * Exactly one spatial mode is active. [OFF] bypasses the spatial renderer.
 */
enum class SpatialAudioMode {
    OFF,
    NATURAL,
    SURROUND,
    ORBIT,
    ;

    companion object {
        fun fromStored(value: String?): SpatialAudioMode {
            if (value.isNullOrBlank()) return OFF
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OFF
        }
    }
}
