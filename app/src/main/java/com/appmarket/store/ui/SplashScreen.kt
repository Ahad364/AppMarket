package com.appmarket.store.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appmarket.store.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashTop = Color(0xFF0B1B4A)
private val SplashMid = Color(0xFF0A3D91)
private val SplashBottom = Color(0xFF0E7BD8)

/**
 * Opening animation: the logo springs in, the name rises in, then the tagline appears.
 * The screen then fades out and calls [onFinished].
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }
    val nameOffset = remember { Animatable(28f) }
    val tagAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(450))
        logoScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        delay(120)
        nameAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        nameOffset.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        tagAlpha.animateTo(1f, tween(400))
        delay(900)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SplashTop, SplashMid, SplashBottom))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "AppMarket",
                modifier = Modifier
                    .size(124.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha = logoAlpha.value
                    }
                    .clip(RoundedCornerShape(30.dp))
            )
            Spacer(Modifier.height(26.dp))
            Text(
                "AppMarket",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    alpha = nameAlpha.value
                    translationY = nameOffset.value
                }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "DOWNLOAD  •  EXPLORE  •  ENJOY",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = tagAlpha.value }
            )
        }
    }
}

/**
 * Shows the app behind the splash, then fades the splash away once it finishes.
 * The app starts loading right away, so the first screen is ready when the splash ends.
 */
@Composable
fun AppRoot(startSignedIn: Boolean) {
    var showSplash by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        MarketNavHost(startSignedIn = startSignedIn)

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(tween(550)) + scaleOut(targetScale = 1.08f, animationSpec = tween(550))
        ) {
            SplashScreen(onFinished = { showSplash = false })
        }
    }
}
