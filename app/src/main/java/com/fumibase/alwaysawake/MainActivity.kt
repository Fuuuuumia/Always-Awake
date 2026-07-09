package com.fumibase.alwaysawake

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fumibase.alwaysawake.ui.theme.AlwaysAwakeTheme
import kotlinx.coroutines.delay


private const val MESSAGE_DURATION_MS = 3000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlwaysAwakeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StartScreen()
                }
            }
        }
    }
}

@Composable
private fun StartScreen() {
    val context = LocalContext.current


    var chargeOnly by rememberSaveable { mutableStateOf(true) }


    var showUnpluggedMessage by remember { mutableStateOf(false) }


    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val reason = result.data?.getStringExtra(AwakeActivity.EXTRA_FINISH_REASON)
        showUnpluggedMessage = reason == AwakeActivity.REASON_UNPLUGGED
    }


    if (showUnpluggedMessage) {
        LaunchedEffect(Unit) {
            delay(MESSAGE_DURATION_MS)
            showUnpluggedMessage = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.charge_only_option))
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = chargeOnly,
                    onCheckedChange = { chargeOnly = it }
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(onClick = {
                val intent = Intent(context, AwakeActivity::class.java)
                    .putExtra(AwakeActivity.EXTRA_CHARGE_ONLY, chargeOnly)
                launcher.launch(intent)
            }) {
                Text(stringResource(R.string.start_button))
            }

            if (showUnpluggedMessage) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.unplugged_message),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() {
    AlwaysAwakeTheme {
        StartScreen()
    }
}
