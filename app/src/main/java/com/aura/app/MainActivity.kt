package com.aura.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.ui.AuraTheme
import com.aura.ui.CoreOrb
import com.aura.app.chat.ChatViewModel

class MainActivity : ComponentActivity() {

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModelRef?.onMicPermissionGranted()
        }

    private var viewModelRef: ChatViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AuraTheme {
                val vm: ChatViewModel = viewModel()
                viewModelRef = vm

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        vm.onMicPermissionGranted()
                    }
                }

                AuraScreen(vm)
            }
        }
    }
}

@Composable
fun AuraScreen(vm: ChatViewModel) {
    val messages by vm.messages.collectAsState()
    val isListening by vm.isListening.collectAsState()
    val isSpeaking by vm.isSpeaking.collectAsState()
    var input by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CoreOrb(active = isListening || isSpeaking)
            }

            Text(
                text = when {
                    isListening -> "Listening…"
                    isSpeaking -> "Speaking…"
                    else -> "Say \"AURA\" or type below"
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(messages) { msg ->
                    ChatBubble(role = msg.role, text = msg.text)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message AURA") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (input.isNotBlank()) {
                        vm.sendText(input)
                        input = ""
                    }
                }) { Text("Send") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { vm.toggleMic() }) {
                    Text(if (isListening) "Stop" else "Mic")
                }
            }
        }
    }
}

@Composable
fun ChatBubble(role: String, text: String) {
    val isUser = role == "user"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(text = text, modifier = Modifier.padding(10.dp))
        }
    }
}
