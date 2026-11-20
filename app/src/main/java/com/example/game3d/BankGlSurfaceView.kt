package com.example.game3d

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
@Composable
fun BankGlSurfaceView(
    snapshot: WorldRenderSnapshot,
    cameraSensitivity: Float,
    onCameraDelta: (deltaYaw: Float, deltaPitch: Float) -> Unit,
    onViewportTap: () -> Unit,
    onMarkersUpdated: (List<ProjectedMarker>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember { BankGlRenderer(onMarkersUpdated) }

    LaunchedEffect(snapshot) {
        renderer.worldSnapshot = snapshot
    }

    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            var lastX = 0f
            var lastY = 0f
            var totalDrag = 0f
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.x
                        lastY = event.y
                        totalDrag = 0f
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - lastX
                        val dy = event.y - lastY
                        lastX = event.x
                        lastY = event.y
                        totalDrag += abs(dx) + abs(dy)
                        val scale = 0.16f * cameraSensitivity
                        onCameraDelta(dx * scale, -dy * scale)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (totalDrag < 18f) {
                            onViewportTap()
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    DisposableEffect(glView) {
        onDispose {
            glView.onPause()
        }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier
    )
}
