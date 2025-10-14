package com.example.apptest2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.apptest2.ui.theme.Apptest2Theme
import com.example.apptest2.usb.UsbCdcManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Apptest2Theme {
                val context = LocalContext.current
                val usbCdcManager = remember { UsbCdcManager(context) }
                val coroutineScope = rememberCoroutineScope()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FourButtonsScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSend = { data ->
                            coroutineScope.launch {
                                usbCdcManager.sendString(data)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FourButtonsScreen(modifier: Modifier = Modifier, onSend: (String) -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { onSend("Trame 1") }) {
            Text("Button 1")
        }
        Button(onClick = { onSend("Trame 2") }) {
            Text("Button 2")
        }
        Button(onClick = { onSend("Trame 3") }) {
            Text("Button 3")
        }
        Button(onClick = { onSend("Trame 4") }) {
            Text("Button 4")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FourButtonsScreenPreview() {
    Apptest2Theme {
        FourButtonsScreen(onSend = {})
    }
}
