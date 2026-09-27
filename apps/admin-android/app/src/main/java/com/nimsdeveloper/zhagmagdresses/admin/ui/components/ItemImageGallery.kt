package com.nimsdeveloper.zhagmagdresses.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.nimsdeveloper.zhagmagdresses.admin.R
import kotlin.math.roundToInt

private enum class GalleryImageState {
    LOADING,
    READY,
    ERROR
}

/**
 * One shared, view-only Item image gallery for every Admin surface.
 *
 * The viewer is deliberately local-only: opening, swiping, zooming, panning and
 * closing never perform a backend request or mutation.
 */
@Composable
fun ItemImageGalleryDialog(
    itemName: String,
    imageUrls: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val images = imageUrls.map(String::trim).filter(String::isNotBlank).distinct()
    if (images.isEmpty()) return

    val safeInitial = initialIndex.coerceIn(0, images.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeInitial) { images.size }
    var controlsVisible by remember { mutableStateOf(true) }
    var zoomedPage by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(images, safeInitial) {
        if (pagerState.currentPage !in images.indices) pagerState.scrollToPage(safeInitial)
    }
    LaunchedEffect(pagerState.currentPage) {
        controlsVisible = true
        zoomedPage = null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = zoomedPage == null,
                verticalAlignment = Alignment.CenterVertically
            ) { page ->
                val imageUrl = images[page]
                var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
                var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
                var viewport by remember(imageUrl) { mutableStateOf(IntSize.Zero) }
                var imageState by remember(imageUrl) { mutableStateOf(GalleryImageState.LOADING) }

                LaunchedEffect(pagerState.currentPage == page) {
                    if (pagerState.currentPage != page) {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }

                fun clampOffset(candidate: Offset, atScale: Float): Offset {
                    if (atScale <= 1.01f || viewport.width <= 0 || viewport.height <= 0) return Offset.Zero
                    val maxX = viewport.width * (atScale - 1f) / 2f
                    val maxY = viewport.height * (atScale - 1f) / 2f
                    return Offset(
                        x = candidate.x.coerceIn(-maxX, maxX),
                        y = candidate.y.coerceIn(-maxY, maxY)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .onSizeChanged { viewport = it }
                        .pointerInput(imageUrl) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var transformOwnsGesture = scale > 1.01f
                                do {
                                    val event = awaitPointerEvent()
                                    val pressedPointers = event.changes.count { it.pressed }
                                    if (!transformOwnsGesture && pressedPointers >= 2) {
                                        transformOwnsGesture = true
                                    }
                                    if (transformOwnsGesture) {
                                        val nextScale = (scale * event.calculateZoom()).coerceIn(1f, 4f)
                                        scale = nextScale
                                        offset = clampOffset(offset + event.calculatePan(), nextScale)
                                        zoomedPage = if (nextScale > 1.01f) page else null
                                        event.changes.forEach { change ->
                                            if (change.pressed) change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .pointerInput(imageUrl) {
                            detectTapGestures(
                                onTap = { controlsVisible = !controlsVisible },
                                onDoubleTap = {
                                    if (scale > 1.01f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                        zoomedPage = null
                                    } else {
                                        scale = 2.5f
                                        offset = Offset.Zero
                                        zoomedPage = page
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "$itemName image ${page + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit,
                        placeholder = painterResource(R.drawable.ic_launcher),
                        error = painterResource(R.drawable.ic_launcher),
                        fallback = painterResource(R.drawable.ic_launcher),
                        onLoading = { imageState = GalleryImageState.LOADING },
                        onSuccess = { imageState = GalleryImageState.READY },
                        onError = { imageState = GalleryImageState.ERROR }
                    )

                    when (imageState) {
                        GalleryImageState.LOADING -> CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        GalleryImageState.ERROR -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Image unavailable",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        GalleryImageState.READY -> Unit
                    }

                    if (controlsVisible && scale > 1.01f) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp),
                            color = Color.Black.copy(alpha = 0.58f),
                            contentColor = Color.White,
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Text(
                                text = "${(scale * 100).roundToInt()}%",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (controlsVisible) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = 260.dp),
                        color = Color.Black.copy(alpha = 0.58f),
                        contentColor = Color.White,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = itemName,
                                modifier = Modifier.weight(1f, fill = false),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pagerState.currentPage + 1} / ${images.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.78f),
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.58f),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close image gallery"
                            )
                        }
                    }
                }
            }
        }
    }
}
