// VideoScreen function
//
// Picks a video from your gallery and play it with full controls:
// Play, pause, fast forward/backward, change speed
// Can also clear video and select another video to play

@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.example.mobilecomputing

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController

@OptIn(UnstableApi::class)
@Composable
fun VideoScreen(navController: NavHostController) {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }

    // Build ExoPlayer once; release on dispose
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setSeekForwardIncrementMs(5000)
            .setSeekBackIncrementMs(5000)
            .build()
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // Update player when URI changes
    LaunchedEffect(videoUri) {
        videoUri?.let { uri ->
            val item = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(item)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    // Video picker (any video from gallery)
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> videoUri = uri }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { navController.popBackStack() }) { Text("← Back") }
            Spacer(modifier = Modifier.weight(1f))
            Text("Video", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(1f))
        }

        // ExoPlayer view embedded via AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (videoUri == null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No video selected. Pick one from your gallery.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { videoPicker.launch("video/*") }) {
                Text("Pick Video")
            }
            if (videoUri != null) {
                Button(onClick = {
                    videoUri = null
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                }) {
                    Text("Clear")
                }
            }
        }
    }
}