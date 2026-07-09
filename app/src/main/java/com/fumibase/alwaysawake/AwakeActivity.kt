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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class AwakeActivity : ComponentActivity() {

    companion object {

        const val EXTRA_CHARGE_ONLY = "extra_charge_only"

        const val EXTRA_FINISH_REASON = "extra_finish_reason"
        const val REASON_UNPLUGGED = "reason_unplugged"
    }

    private var chargeOnly = false

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

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            BlackScreen(onDoubleTap = { finish() })
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
        // ACTION_BATTERY_CHANGEDはスティッキーなので、null受信で現在値を取得できる
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        return plugged != 0
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        // スワイプ時のみ一時的にシステムバーを表示させる
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}


@Composable
private fun BlackScreen(onDoubleTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onDoubleTap() })
            }
    )
}
