package com.apsmkimo.go2radiolive.presentation.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

/**
 * Status bar, navigation bar, IME, and display-cutout insets.
 * PaddingValues do not consume insets, so a bar and the content under it
 * can both stay clear of the same cutout.
 */
@Composable
fun topSafePadding(): PaddingValues =
    WindowInsets.safeDrawing
        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        .asPaddingValues()

@Composable
fun bottomSafePadding(): PaddingValues =
    WindowInsets.safeDrawing
        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
        .asPaddingValues()

@Composable
fun safeDrawingPadding(): PaddingValues = WindowInsets.safeDrawing.asPaddingValues()

@Composable
fun screenContentInsets(): WindowInsets = WindowInsets.safeDrawing

@Composable
fun sheetContentInsets(): WindowInsets =
    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
