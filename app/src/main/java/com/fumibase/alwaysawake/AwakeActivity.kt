package com.fumibase.alwaysawake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class AwakeActivity : ComponentActivity() {

    companion object {

        const val EXTRA_CHARGE_ONLY = "extra_charge_only"

        const val EXTRA_SHOW_AWAKE_ICON = "extra_show_awake_icon"

        const val EXTRA_FINISH_REASON = "extra_finish_reason"
        const val REASON_UNPLUGGED = "reason_unplugged"
    }

    private var chargeOnly = false

    private var showAwakeIcon = false

    private var isReceiverRegistered = false

    private val powerDisconnectedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_POWER_DISCONNECTED) {
                finishBecauseUnplugged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chargeOnly = intent.getBooleanExtra(EXTRA_CHARGE_ONLY, false)
        showAwakeIcon = intent.getBooleanExtra(EXTRA_SHOW_AWAKE_ICON, false)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            BlackScreen(
                showAwakeIcon = showAwakeIcon,
                onDoubleTap = { finish() }
            )
        }
    }

    override fun onResume() {
        super.onResume()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        if (chargeOnly) {

            if (!isPluggedIn()) {
                finishBecauseUnplugged()
                return
            }

            registerReceiver(
                powerDisconnectedReceiver,
                IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
            )
            isReceiverRegistered = true
        }
    }

    override fun onPause() {

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        if (isReceiverRegistered) {
            unregisterReceiver(powerDisconnectedReceiver)
            isReceiverRegistered = false
        }
        super.onPause()
    }

    override fun onStop() {
        super.onStop()

        if (!isChangingConfigurations) {
            finish()
        }
    }


    private fun finishBecauseUnplugged() {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_FINISH_REASON, REASON_UNPLUGGED))
        finish()
    }


    private fun isPluggedIn(): Boolean {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        return plugged != 0
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}


@Composable
private fun BlackScreen(
    showAwakeIcon: Boolean,
    onDoubleTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onDoubleTap() })
            }
    ) {
        if (showAwakeIcon) {
            AwakeIndicator()
        }
    }
}



private const val ICON_FADE_MS = 1000

private const val ICON_CYCLE_MS = 30_000L

private const val ICON_HOLD_MS = ICON_CYCLE_MS - 2 * ICON_FADE_MS

private val CURVE_STROKE_WIDTH = 1.2.dp


private const val CURVE_SAMPLE_COUNT = 900


private const val CURVE_T_MAX = 9f


private const val CURVE_AMPLITUDE_RATIO = 0.46f


private class Harmonograph(
    val fx1: Float, val fx2: Float, val fy1: Float, val fy2: Float,
    val px1: Float, val px2: Float, val py1: Float, val py2: Float,
    val dx1: Float, val dx2: Float, val dy1: Float, val dy2: Float,
)

private fun randomHarmonograph(random: Random): Harmonograph {
    fun frequency() = random.nextInt(1, 4) + (random.nextFloat() - 0.5f) * 0.06f
    fun phase() = random.nextFloat() * (2f * PI.toFloat())
    fun damping() = 0.002f + random.nextFloat() * 0.008f

    return Harmonograph(
        fx1 = frequency(), fx2 = frequency(), fy1 = frequency(), fy2 = frequency(),
        px1 = phase(), px2 = phase(), py1 = phase(), py2 = phase(),
        dx1 = damping(), dx2 = damping(), dy1 = damping(), dy2 = damping(),
    )
}

private fun Harmonograph.toPath(widthPx: Float, heightPx: Float): Path {
    val cx = widthPx / 2f
    val cy = heightPx / 2f
    val ampX = widthPx * CURVE_AMPLITUDE_RATIO / 2f
    val ampY = heightPx * CURVE_AMPLITUDE_RATIO / 2f
    val dt = CURVE_T_MAX / CURVE_SAMPLE_COUNT

    return Path().apply {
        for (i in 0..CURVE_SAMPLE_COUNT) {
            val t = i * dt
            val x = cx + ampX *
                (sin(fx1 * t + px1) * exp(-dx1 * t) + sin(fx2 * t + px2) * exp(-dx2 * t))
            val y = cy + ampY *
                (sin(fy1 * t + py1) * exp(-dy1 * t) + sin(fy2 * t + py2) * exp(-dy2 * t))
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
}

@Composable
private fun AwakeIndicator() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val alpha = remember { Animatable(0f) }
        var seed by remember { mutableStateOf(Random.nextLong()) }

        val path = remember(seed, widthPx, heightPx) {
            randomHarmonograph(Random(seed)).toPath(widthPx, heightPx)
        }

        LaunchedEffect(widthPx, heightPx) {
            while (true) {
                alpha.animateTo(1f, tween(ICON_FADE_MS))
                delay(ICON_HOLD_MS)
                alpha.animateTo(0f, tween(ICON_FADE_MS))
                seed = Random.nextLong()
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = CURVE_STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
            drawPath(path, color = Color.White, alpha = alpha.value, style = stroke)
        }
    }
}
