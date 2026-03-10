package com.example.helloworld

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.helloworld.ui.theme.HelloWorldTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestMicPermission()

        // Start wake word service
        val intent = Intent(this, WakeWordService::class.java)
        startService(intent)

        enableEdgeToEdge()

        setContent {
            HelloWorldTheme {
                JarvisScreen()
            }
        }
    }

    private fun requestMicPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }
    }
}

@Composable
fun JarvisScreen() {

    var status by remember { mutableStateOf("Listening for 'Hello Jarvis'") }

    Scaffold { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Jarvis Assistant",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = status)

            Spacer(modifier = Modifier.height(30.dp))

            Button(onClick = {
                status = "Wake word engine running..."
            }) {

                Text("Start Jarvis")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JarvisPreview() {
    HelloWorldTheme {
        JarvisScreen()
    }
}