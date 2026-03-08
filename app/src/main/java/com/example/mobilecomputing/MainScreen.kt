// The screen that you are greeted with as soon as you open the app
//
// Here, a message column from HW1 is displayed. By default, the author is "Lexi"
// and has default profile picture
//
// HW2 feature with the "Go!" button and HW4 feature with the "Enable Notificaions"
// button are also implemented here
//
// There are a few buttons here at the end of the screen. Crucially, the "👤 Profile"
// button is a HW3 implementation. Users can change username and select an image from
// the phone's gallery as their profile picture
//
// This will change the user's appearance in the message column of MainScreen,
// but not ChatScreen

package com.example.mobilecomputing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun MainScreen(navController: NavHostController) {
    AcceleratorMonitor()

    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)

    var username by remember { mutableStateOf("Lexi") }
    var imagePath by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        db.userProfileDao().getProfile()?.let {
            username = it.username
            imagePath = it.imagePath
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(SampleData.conversationSample) { message ->
                MessageCard(message.copy(author = username), imagePath = imagePath)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Click this button below for a surprise :3",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                CenteredButton("Go!") { navController.navigate("secondary") }
                Spacer(modifier = Modifier.height(8.dp))

                EnableNotificationButton()
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Features",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                val features = listOf(
                    "👤 Profile"     to "profile_edit",
                    "💬 Chat"        to "chat",
                    "🗺 Maps"        to "maps",
                    "🎬 Video"       to "video",
                    "🎙 Mic"         to "microphone",
                    "📷 Camera"      to "camera",
                    "🌤 Weather API" to "api",
                )

                features.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (label, route) ->
                            Button(
                                onClick = { navController.navigate(route) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, textAlign = TextAlign.Center)
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun CenteredButton(label: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(onClick = onClick) { Text(label) }
    }
}