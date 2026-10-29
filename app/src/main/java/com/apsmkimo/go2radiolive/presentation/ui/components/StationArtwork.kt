package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.station.StationIconMotion
import com.apsmkimo.go2radiolive.domain.station.StationImages

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarqueeTitle(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .basicMarquee(iterations = Int.MAX_VALUE),
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
    )
}

@Composable
fun StationArtwork(
    station: RadioStation,
    motion: StationIconMotion,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .clip(shape)
            .semantics { contentDescription = station.name },
    ) {
        StationImage(station, Modifier.fillMaxSize())
        if (motion != StationIconMotion.NONE) {
            NowPlayingOverlay(motion, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StationImage(station: RadioStation, modifier: Modifier) {
    val homepageIcon = remember(station.homepage) { StationImages.homepageFavicon(station.homepage) }
    val favicon = station.faviconUrl.trim()
    var phase by remember(station.stationUuid, favicon, homepageIcon) {
        mutableIntStateOf(
            when {
                favicon.isNotEmpty() -> 0
                homepageIcon != null -> 1
                else -> 2
            },
        )
    }
    val url = when (phase) {
        0 -> favicon
        1 -> homepageIcon
        else -> null
    }
    Box(modifier) {
        StationPlaceholder(station.name, Modifier.fillMaxSize())
        if (!url.isNullOrBlank()) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .listener(
                        onError = { _, _ ->
                            phase = if (phase <= 0 && homepageIcon != null) 1 else 2
                        },
                    )
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun StationPlaceholder(name: String, modifier: Modifier = Modifier) {
    val color = Color(StationImages.placeholderColorArgb(name))
    Box(
        modifier = modifier.background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = StationImages.placeholderLetter(name),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun NowPlayingOverlay(motion: StationIconMotion, modifier: Modifier) {
    Box(
        modifier.background(Color.Black.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        when (motion) {
            StationIconMotion.SPINNER -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            }
            StationIconMotion.BARS -> EqualizerBars(animating = rememberAppVisible())
            StationIconMotion.BARS_PAUSED -> EqualizerBars(animating = false)
            StationIconMotion.NONE -> Unit
        }
    }
}

@Composable
private fun rememberAppVisible(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var visible by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { source, _ ->
            visible = source.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return visible
}

@Composable
private fun EqualizerBars(animating: Boolean) {
    if (animating) {
        AnimatedEqualizerBars()
    } else {
        FrozenEqualizerBars()
    }
}

@Composable
private fun AnimatedEqualizerBars() {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val fractions = BAR_DURATIONS.mapIndexed { index, duration ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 80),
            ),
            label = "bar$index",
        ).value
    }
    BarRow(fractions)
}

@Composable
private fun FrozenEqualizerBars() {
    BarRow(FROZEN_FRACTIONS)
}

@Composable
private fun BarRow(fractions: List<Float>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(16.dp),
    ) {
        fractions.forEach { fraction ->
            val scale = fraction.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(3.dp + 13.dp * scale)
                    .background(Color.White, RoundedCornerShape(1.dp)),
            )
        }
    }
}

private val BAR_DURATIONS = listOf(420, 610, 530, 740)
private val FROZEN_FRACTIONS = listOf(0.22f, 0.34f, 0.18f, 0.28f)
