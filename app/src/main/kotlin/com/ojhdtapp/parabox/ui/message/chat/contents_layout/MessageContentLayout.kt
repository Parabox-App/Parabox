package com.ojhdtapp.parabox.ui.message.chat.contents_layout

import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.CachedMediaHandle
import com.ojhdtapp.parabox.core.util.ExtensionMediaLoader
import com.ojhdtapp.parabox.core.util.LocalFileUtil
import com.ojhdtapp.parabox.core.util.audio.AudioPlayer
import com.ojhdtapp.parabox.core.util.audio.LocalAudioPlayer
import com.ojhdtapp.parabox.core.util.audio.LocalAudioRecorder
import com.ojhdtapp.parabox.core.util.awaitUntilSuccess
import com.ojhdtapp.parabox.core.util.formatAudioTimeMs
import com.ojhdtapp.parabox.core.util.toMSString
import com.ojhdtapp.parabox.core.util.toMediaTimeString
import com.ojhdtapp.parabox.domain.cloud.LocalCloudService
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.origeek.imageViewer.previewer.ImagePreviewerState
import com.origeek.imageViewer.previewer.TransformImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun PlainTextLayout(
    modifier: Modifier = Modifier,
    text: AnnotatedString
) {
    Text(
        modifier = modifier.padding(horizontal = 9.dp, vertical = 9.dp),
        text = text
    )
}

@Composable
fun ImageLayout(
    modifier: Modifier = Modifier,
    model: Any?,
    elementId: Long,
    previewerState: ImagePreviewerState,
    onClick: (elementId: Long) -> Unit,
) {
    var boxWidth by remember {
        mutableStateOf(128.dp)
    }
    var boxHeight by remember {
        mutableStateOf(128.dp)
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(model)
            .size(coil.size.Size.ORIGINAL)
            .crossfade(true)
            .memoryCacheKey("$elementId")
            .build(),
        error = painterResource(id = R.drawable.image_lost),
        fallback = painterResource(id = R.drawable.image_lost),
        contentScale = ContentScale.Crop,
        onSuccess = {
            coroutineScope.launch {
                val bitmap = it.result.drawable.toBitmap()
                val originalWidthDp = with(density) {
                    bitmap.width.toDp()
                }
                val originalHeightDp = with(density) {
                    bitmap.height.toDp()
                }
                if (originalWidthDp != 0.dp) {
                    boxWidth = originalWidthDp.coerceIn(128.dp, 320.dp)
                    boxHeight = (boxWidth / originalWidthDp * originalHeightDp).coerceIn(32.dp, 320.dp)
                }
            }
        },
        onError = {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.image_lost)
            boxWidth = with(density) {
                bitmap.width.toDp()
            }
            boxHeight = with(density) {
                bitmap.height.toDp()
            }
        }
    )
    TransformImageView(
        modifier = modifier
            .size(boxWidth, boxHeight)
            .pointerInput(Unit) {
                detectTapGestures {
                    onClick(elementId)
                }
            },
        key = elementId,
        painter = painter,
        previewerState = previewerState,
    )
}

private fun computeAnimatedMediaBoxSize(
    widthPx: Int,
    heightPx: Int,
    defaultDp: Dp,
    minDp: Dp,
    maxDp: Dp,
): Pair<Dp, Dp> {
    if (widthPx <= 0 || heightPx <= 0) return defaultDp to defaultDp
    val ratio = heightPx.toFloat() / widthPx.toFloat()
    val w = defaultDp.coerceIn(minDp, maxDp)
    val h = (w * ratio).coerceIn(minDp, maxDp)
    return w to h
}

@Composable
fun TgsStickerLayout(
    modifier: Modifier = Modifier,
    model: Any?,
    widthPx: Int = 0,
    heightPx: Int = 0,
) {
    val context = LocalContext.current
    val (boxWidth, boxHeight) = remember(widthPx, heightPx) {
        computeAnimatedMediaBoxSize(
            widthPx = widthPx,
            heightPx = heightPx,
            defaultDp = 160.dp,
            minDp = 96.dp,
            maxDp = 220.dp,
        )
    }
    var lottieJson by remember(model) { mutableStateOf<String?>(null) }
    var loadFailed by remember(model) { mutableStateOf(false) }

    LaunchedEffect(model) {
        loadFailed = false
        lottieJson = null
        val json = ExtensionMediaLoader.loadTgsJson(context, model)
        if (json != null) {
            lottieJson = json
        } else {
            loadFailed = true
        }
    }

    Box(
        modifier = modifier.size(boxWidth, boxHeight),
        contentAlignment = Alignment.Center,
    ) {
        when {
            lottieJson != null -> {
                val composition by rememberLottieComposition(
                    spec = LottieCompositionSpec.JsonString(lottieJson!!),
                    cacheKey = model?.toString(),
                )
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            loadFailed -> {
                Image(
                    painter = painterResource(id = R.drawable.image_lost),
                    contentDescription = "sticker_error",
                    modifier = Modifier.size(64.dp),
                )
            }
            else -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

@Composable
fun LoopingVideoLayout(
    modifier: Modifier = Modifier,
    model: Any?,
    widthPx: Int = 0,
    heightPx: Int = 0,
) {
    val context = LocalContext.current
    var resolvedWidthPx by remember(model, widthPx) { mutableStateOf(widthPx) }
    var resolvedHeightPx by remember(model, heightPx) { mutableStateOf(heightPx) }
    val (boxWidth, boxHeight) = remember(resolvedWidthPx, resolvedHeightPx) {
        computeAnimatedMediaBoxSize(
            widthPx = resolvedWidthPx,
            heightPx = resolvedHeightPx,
            defaultDp = 200.dp,
            minDp = 96.dp,
            maxDp = 280.dp,
        )
    }

    var cachedHandle by remember(model) { mutableStateOf<CachedMediaHandle?>(null) }
    var loadFailed by remember(model) { mutableStateOf(false) }

    LaunchedEffect(model) {
        loadFailed = false
        val handle = ExtensionMediaLoader.openCachedMedia(context, model)
        if (handle != null) {
            cachedHandle = handle
        } else {
            loadFailed = true
        }
    }

    DisposableEffect(cachedHandle) {
        val handle = cachedHandle
        onDispose {
            handle?.close()
        }
    }

    Box(
        modifier = modifier
            .size(boxWidth, boxHeight)
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val file = cachedHandle?.file
        when {
            file != null -> {
                val playerRef = remember(file.absolutePath) { arrayOfNulls<MediaPlayer>(1) }
                val surfaceRef = remember(file.absolutePath) { arrayOfNulls<Surface>(1) }

                DisposableEffect(file.absolutePath) {
                    onDispose {
                        runCatching {
                            playerRef[0]?.stop()
                        }
                        runCatching {
                            playerRef[0]?.release()
                        }
                        playerRef[0] = null
                        runCatching {
                            surfaceRef[0]?.release()
                        }
                        surfaceRef[0] = null
                    }
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            isOpaque = false
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(
                                    surfaceTexture: SurfaceTexture,
                                    width: Int,
                                    height: Int,
                                ) {
                                    runCatching {
                                        playerRef[0]?.release()
                                        surfaceRef[0]?.release()
                                        val surface = Surface(surfaceTexture)
                                        surfaceRef[0] = surface
                                        val mp = MediaPlayer().apply {
                                            setDataSource(file.absolutePath)
                                            setSurface(surface)
                                            setVolume(0f, 0f)
                                            isLooping = true
                                            setOnVideoSizeChangedListener { _, vw, vh ->
                                                if (vw > 0 && vh > 0) {
                                                    resolvedWidthPx = vw
                                                    resolvedHeightPx = vh
                                                }
                                            }
                                            setOnPreparedListener { player ->
                                                player.start()
                                            }
                                            setOnErrorListener { _, _, _ ->
                                                loadFailed = true
                                                true
                                            }
                                            prepareAsync()
                                        }
                                        playerRef[0] = mp
                                    }.onFailure {
                                        loadFailed = true
                                    }
                                }

                                override fun onSurfaceTextureSizeChanged(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int,
                                ) = Unit

                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                    runCatching { playerRef[0]?.stop() }
                                    runCatching { playerRef[0]?.release() }
                                    playerRef[0] = null
                                    runCatching { surfaceRef[0]?.release() }
                                    surfaceRef[0] = null
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                            }
                        }
                    },
                )
            }
            loadFailed -> {
                Image(
                    painter = painterResource(id = R.drawable.image_lost),
                    contentDescription = "animation_error",
                    modifier = Modifier.size(64.dp),
                )
            }
            else -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

@Composable
fun AudioLayout(
    modifier: Modifier = Modifier,
    elementId: Long,
    resourceInfo: ParaboxResourceInfo,
    lengthSeconds: Long,
    fileName: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    isSentByMe: Boolean = false,
) {
    val player = LocalAudioPlayer.current
    val hapticFeedback = LocalHapticFeedback.current
    val globalState by player.playbackState.collectAsState()

    val itemKey = remember(elementId, resourceInfo) {
        "$elementId:${resourceInfo.getModel()?.toString().orEmpty()}"
    }
    val isActive = globalState.activeKey == itemKey
    val status = if (isActive) globalState.status else AudioPlayer.PlayStatus.Idle
    val fallbackDurationMs = remember(lengthSeconds) {
        (lengthSeconds.coerceAtLeast(0L) * 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
    val effectiveDurationMs = if (isActive && globalState.durationMs > 0) {
        globalState.durationMs
    } else {
        fallbackDurationMs
    }
    val currentPositionMs = if (isActive) globalState.positionMs else 0

    var isScrubbing by remember(itemKey) { mutableStateOf(false) }
    var scrubFraction by remember(itemKey) { mutableFloatStateOf(0f) }
    var bubbleWidthPx by remember { mutableIntStateOf(1) }

    val baseFraction = if (effectiveDurationMs > 0) {
        (currentPositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val displayedFraction = if (isScrubbing) scrubFraction else baseFraction
    val displayedPositionMs = if (isScrubbing && effectiveDurationMs > 0) {
        (scrubFraction * effectiveDurationMs).roundToInt().coerceIn(0, effectiveDurationMs)
    } else {
        currentPositionMs
    }

    val bubbleWidthDp = remember(lengthSeconds) {
        val ratio = (lengthSeconds.coerceIn(2L, 60L) - 2L) / 58f
        (196f + ratio * 60f).dp
    }
    val waveformBars = remember(elementId) {
        val rng = Random(elementId xor 0x5A3C9E1BL)
        List(28) { idx ->
            val envelope = 0.65f + 0.35f * kotlin.math.sin((idx + 1) * Math.PI / 29.0).toFloat()
            (0.24f + rng.nextFloat() * 0.76f) * envelope
        }
    }
    val displayTitle = remember(fileName) {
        val clean = fileName?.trim().orEmpty()
        if (clean.isNotEmpty() && !clean.startsWith("tg_voice_") && !clean.startsWith("record_")) {
            clean
        } else {
            null
        }
    }

    val buttonContainerColor = if (isSentByMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val buttonContentColor = if (isSentByMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondary
    }
    val activeBarColor = if (isSentByMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val inactiveBarColor = textColor.copy(alpha = 0.26f)

    Row(
        modifier = modifier
            .width(bubbleWidthDp)
            .onSizeChanged { size ->
                if (size.width > 0) bubbleWidthPx = size.width
            }
            .pointerInput(itemKey, effectiveDurationMs, resourceInfo) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        if (effectiveDurationMs > 0) {
                            isScrubbing = true
                            scrubFraction = baseFraction
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (effectiveDurationMs > 0) {
                            change.consume()
                            val delta = dragAmount / bubbleWidthPx.coerceAtLeast(1).toFloat()
                            scrubFraction = (scrubFraction + delta).coerceIn(0f, 1f)
                        }
                    },
                    onDragEnd = {
                        if (isScrubbing && effectiveDurationMs > 0) {
                            val targetMs = (scrubFraction * effectiveDurationMs).roundToInt()
                                .coerceIn(0, effectiveDurationMs)
                            isScrubbing = false
                            player.seekTo(
                                itemKey = itemKey,
                                positionMs = targetMs,
                                resourceInfo = resourceInfo,
                                fallbackDurationSec = lengthSeconds,
                                fileName = fileName,
                            )
                        } else {
                            isScrubbing = false
                        }
                    },
                    onDragCancel = {
                        isScrubbing = false
                    },
                )
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = buttonContainerColor,
            contentColor = buttonContentColor,
            onClick = {
                player.togglePlayPause(
                    itemKey = itemKey,
                    resourceInfo = resourceInfo,
                    fallbackDurationSec = lengthSeconds,
                    fileName = fileName,
                )
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Crossfade(targetState = status, label = "audio_play_status") { s ->
                    when (s) {
                        AudioPlayer.PlayStatus.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = buttonContentColor,
                                strokeWidth = 2.dp,
                            )
                        }
                        AudioPlayer.PlayStatus.Playing -> {
                            Icon(
                                modifier = Modifier.size(22.dp),
                                imageVector = Icons.Outlined.Pause,
                                contentDescription = "pause",
                            )
                        }
                        AudioPlayer.PlayStatus.Error -> {
                            Icon(
                                modifier = Modifier.size(22.dp),
                                imageVector = Icons.Outlined.Replay,
                                contentDescription = "retry",
                            )
                        }
                        AudioPlayer.PlayStatus.Idle,
                        AudioPlayer.PlayStatus.Paused -> {
                            Icon(
                                modifier = Modifier.size(22.dp),
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = "play",
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (displayTitle != null) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Interactive M3 waveform progress track
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .pointerInput(itemKey, effectiveDurationMs, resourceInfo) {
                        detectTapGestures { offset ->
                            if (effectiveDurationMs > 0 && size.width > 0) {
                                val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val targetMs = (fraction * effectiveDurationMs).roundToInt()
                                    .coerceIn(0, effectiveDurationMs)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                player.seekTo(
                                    itemKey = itemKey,
                                    positionMs = targetMs,
                                    resourceInfo = resourceInfo,
                                    fallbackDurationSec = lengthSeconds,
                                    fileName = fileName,
                                )
                            }
                        }
                    }
            ) {
                val count = waveformBars.size
                if (count == 0 || size.width <= 0f || size.height <= 0f) return@Canvas
                val slotWidth = size.width / count.toFloat()
                val barWidth = (slotWidth * 0.56f).coerceIn(2.dp.toPx(), 4.dp.toPx())
                val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
                val progressX = displayedFraction * size.width

                waveformBars.forEachIndexed { idx, amp ->
                    val centerX = idx * slotWidth + slotWidth / 2f
                    val isPlayed = centerX <= progressX && (displayedFraction > 0f)
                    val isHead = isScrubbing && abs(centerX - progressX) <= slotWidth * 0.7f
                    val barHeight = (size.height * amp).coerceAtLeast(4.dp.toPx())
                    val top = (size.height - barHeight) / 2f
                    drawRoundRect(
                        color = if (isPlayed || isHead) activeBarColor else inactiveBarColor,
                        topLeft = Offset(centerX - barWidth / 2f, top),
                        size = Size(if (isHead) barWidth * 1.25f else barWidth, barHeight),
                        cornerRadius = corner,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val timeLabel = when {
                    status == AudioPlayer.PlayStatus.Error ->
                        globalState.errorMessage ?: "播放失败，点击重试"
                    status == AudioPlayer.PlayStatus.Loading && effectiveDurationMs <= 0 ->
                        "加载中..."
                    isScrubbing || status == AudioPlayer.PlayStatus.Playing || status == AudioPlayer.PlayStatus.Paused -> {
                        if (effectiveDurationMs > 0) {
                            "${displayedPositionMs.formatAudioTimeMs()} / ${effectiveDurationMs.formatAudioTimeMs()}"
                        } else {
                            displayedPositionMs.formatAudioTimeMs()
                        }
                    }
                    effectiveDurationMs > 0 -> effectiveDurationMs.formatAudioTimeMs()
                    else -> "0:00"
                }
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (status == AudioPlayer.PlayStatus.Error) {
                        MaterialTheme.colorScheme.error
                    } else {
                        textColor.copy(alpha = 0.78f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun FileLayout(
    modifier: Modifier = Modifier,
    fileName: String = "",
    fileSize: Long = 0L,
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    val label = buildString {
        append("[文件] ")
        append(fileName.ifBlank { "未知文件" })
        if (fileSize > 0L) {
            val kb = fileSize / 1024.0
            val formatted = if (kb >= 1024.0) String.format("%.1f MB", kb / 1024.0) else String.format("%.1f KB", kb)
            append(" ($formatted)")
        }
    }
    Text(
        modifier = modifier.padding(horizontal = 9.dp, vertical = 9.dp),
        text = label,
        color = textColor,
    )
}

@Composable
fun VideoLayout(
    modifier: Modifier = Modifier,
    fileName: String? = null,
    length: Long = 0L,
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    val label = buildString {
        append("[视频]")
        if (!fileName.isNullOrBlank()) append(" $fileName")
        if (length > 0L) append(" (${length}s)")
    }
    Text(
        modifier = modifier.padding(horizontal = 9.dp, vertical = 9.dp),
        text = label,
        color = textColor,
    )
}

@Composable
fun LocationLayout(
    modifier: Modifier = Modifier,
    name: String? = null,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    val label = if (!name.isNullOrBlank()) {
        "[位置] $name"
    } else {
        String.format("[位置] %.5f, %.5f", latitude, longitude)
    }
    Text(
        modifier = modifier.padding(horizontal = 9.dp, vertical = 9.dp),
        text = label,
        color = textColor,
    )
}

@Composable
fun QuoteReplyLayout(
    modifier: Modifier = Modifier,
    senderName: String? = null,
    summary: String = "",
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onClick: (() -> Unit)? = null,
) {
    val cleanSender = senderName?.trim().orEmpty()
    val cleanSummary = summary.trim()
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(textColor.copy(alpha = 0.08f))
            .then(clickModifier)
            .height(IntrinsicSize.Min)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(textColor.copy(alpha = 0.55f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            if (cleanSender.isNotEmpty()) {
                Text(
                    text = cleanSender,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = when {
                    cleanSummary.isNotEmpty() -> cleanSummary
                    cleanSender.isNotEmpty() -> "[消息]"
                    else -> "回复了一条消息"
                },
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.78f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun UnsupportedLayout(
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier.padding(horizontal = 9.dp, vertical = 9.dp),
        text = "不支持的消息类型",
        color = MaterialTheme.colorScheme.primary
    )
}