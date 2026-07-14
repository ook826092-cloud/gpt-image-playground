package com.gptimage.playground.ui.screens.album

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File

/**
 * 全屏图片缩放查看器。
 *
 * 移植自 Web 项目 `src/components/zoom-viewer.tsx`，但用 Compose 原生手势：
 * - pinch 缩放（detectTransformGestures）
 * - 单指拖拽（同上）
 * - 双击切换 1.0x ↔ 2.5x
 * - 单击切换工具栏显隐
 *
 * 不实现画廊左右切换（单张预览场景已经够用）。
 */
@Composable
fun ZoomableImageDialog(
    imagePath: String,
    contentDescription: String?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    var chromeVisible by remember { mutableStateOf(true) }
    // chrome 2.6s 后自动隐藏，对齐 Web 端
    LaunchedEffect(chromeVisible) {
        if (chromeVisible) {
            kotlinx.coroutines.delay(2600)
            chromeVisible = false
        }
    }

    val reset = {
        scope.launch {
            scale.animateTo(1f, spring())
            offsetX.animateTo(0f, spring())
            offsetY.animateTo(0f, spring())
        }
    }

    val toggleZoom = {
        scope.launch {
            if (scale.value > 1.05f) {
                scale.animateTo(1f, spring())
                offsetX.animateTo(0f, spring())
                offsetY.animateTo(0f, spring())
            } else {
                scale.animateTo(2.5f, spring())
            }
        }
        Unit
    }

    val adjustZoom: (Float) -> Unit = { factor ->
        scope.launch {
            val next = (scale.value * factor).coerceIn(0.5f, 6f)
            scale.animateTo(next, spring())
            if (next <= 1f) {
                offsetX.animateTo(0f, spring())
                offsetY.animateTo(0f, spring())
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.96f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // pinch + pan
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scope.launch {
                                val nextScale = (scale.value * zoom).coerceIn(1f, 6f)
                                scale.snapTo(nextScale)
                                if (nextScale > 1f) {
                                    offsetX.snapTo(offsetX.value + pan.x)
                                    offsetY.snapTo(offsetY.value + pan.y)
                                } else {
                                    offsetX.snapTo(0f)
                                    offsetY.snapTo(0f)
                                }
                            }
                        }
                    }
                    // 双击切换缩放 / 单击切换 chrome
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { toggleZoom() },
                            onTap = { chromeVisible = !chromeVisible }
                        )
                    }
            ) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale.value,
                            scaleY = scale.value,
                            translationX = offsetX.value,
                            translationY = offsetY.value
                        ),
                    contentScale = ContentScale.Fit
                )

                // 顶部工具栏
                if (chromeVisible) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Text(
                            text = "${(scale.value * 100).toInt()}%",
                            color = Color.White
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { adjustZoom(0.85f) }) {
                            Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out", tint = Color.White)
                        }
                        IconButton(onClick = { reset() }) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = "Reset", tint = Color.White)
                        }
                        IconButton(onClick = { adjustZoom(1.15f) }) {
                            Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
