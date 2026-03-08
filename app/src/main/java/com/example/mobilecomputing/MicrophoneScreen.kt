// MicrophoneScreen function
//
// Allows user to record voices and whatever the mic can pick up, and then immediately
// let them play back what is just recorded

package com.example.mobilecomputing

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import java.io.File

@Composable
fun MicrophoneScreen(navController: NavHostController) {
    val context = LocalContext.current

    var micPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> micPermissionGranted = granted }

    var isRecording by remember { mutableStateOf(false) }
    var hasRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Ready") }

    val outputFile = remember { File(context.filesDir, "audio_recording.3gp") }

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.release()
            player?.release()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { navController.popBackStack() }) { Text("← Back") }
            Spacer(modifier = Modifier.weight(1f))
            Text("Microphone", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Permission gate
            if (!micPermissionGranted) {
                Text("Microphone permission is required to record audio.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Grant Microphone Permission")
                }
                return@Column
            }

            // Status
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!isRecording) {
                        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            MediaRecorder(context)
                        } else {
                            @Suppress("DEPRECATION")
                            MediaRecorder()
                        }
                        rec.apply {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                            setOutputFile(outputFile.absolutePath)
                            prepare()
                            start()
                        }
                        recorder = rec
                        isRecording = true
                        statusText = "🔴 Recording…"
                    } else {
                        // Stop recording
                        recorder?.apply { stop(); release() }
                        recorder = null
                        isRecording = false
                        hasRecording = true
                        statusText = "Recording saved ✔"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRecording) "Stop Recording" else "Start Recording")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasRecording) {
                Button(
                    onClick = {
                        if (!isPlaying) {
                            val mp = MediaPlayer().apply {
                                setDataSource(outputFile.absolutePath)
                                prepare()
                                setOnCompletionListener {
                                    isPlaying = false
                                    statusText = "Playback finished"
                                }
                                start()
                            }
                            player = mp
                            isPlaying = true
                            statusText = "▶ Playing…"
                        } else {
                            player?.apply { stop(); release() }
                            player = null
                            isPlaying = false
                            statusText = "Playback stopped"
                        }
                    }
                ) {
                    Text(if (isPlaying) "Stop Playback" else "Play Recording")
                }
            }
        }
    }
}