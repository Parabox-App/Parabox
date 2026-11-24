package com.ojhdtapp.parabox.ui.message.chat.contents_layout

import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.ojhdtapp.parabox.core.util.toMSString
import com.ojhdtapp.parabox.core.util.toMediaTimeString
import com.ojhdtapp.parabox.domain.cloud.LocalCloudService
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.origeek.imageViewer.previewer.ImagePreviewerState
import com.origeek.imageViewer.previewer.TransformImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    resourceInfo: ParaboxResourceInfo,
    length: Long,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val player = LocalAudioPlayer.current
    val cloudService = LocalCloudService.current
    val fileUtil = LocalFileUtil.current
    var playerState by remember {
        mutableStateOf<AudioPlayer.Status>(AudioPlayer.Status.Pause(0, length.toInt()))
    }
    var disabled by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        IconButton(enabled = !disabled, onClick = {
            coroutineScope.launch {
                disabled = true
                if (resourceInfo is ParaboxResourceInfo.ParaboxLocalInfo && fileUtil.checkResourceModelAvailable(resourceInfo.getModel())) {
                    fileUtil.getUriFromResourceModel(resourceInfo.getModel())?.let { uri ->
                        disabled = false
                        player.play(uri).collectLatest {
                            playerState = it
                        }
                        return@launch
                    }
                }
                if (resourceInfo is ParaboxResourceInfo.ParaboxRemoteInfo) {
                    val syncResource = cloudService.download(resourceInfo).awaitUntilSuccess(5000)
                    syncResource?.localUri?.let { uri ->
                        disabled = false
                        player.play(uri).collectLatest {
                            playerState = it
                        }
                        return@launch
                    }
                }
                disabled = false
                playerState = AudioPlayer.Status.Error(0, length.toInt())
            }
        }) {
            when (playerState) {
                is AudioPlayer.Status.Pause -> {
                    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "play")
                }
                is AudioPlayer.Status.Playing -> {
                    Icon(imageVector = Icons.Outlined.Pause, contentDescription = "pause")
                }
                is AudioPlayer.Status.Error -> {
                    Icon(imageVector = Icons.Outlined.Replay, contentDescription = "error")
                }
            }
        }
        Text(
            modifier = Modifier,
            text = "${playerState.position.toMSString()} / ${playerState.duration.toMSString()}",
            color = MaterialTheme.colorScheme.primary
        )
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