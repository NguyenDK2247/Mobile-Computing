// This is where everything is concocted and run

package com.example.mobilecomputing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobilecomputing.ui.theme.MobileComputingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileComputingTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(onFinished = {
                            navController.navigate("main") {
                                popUpTo("splash") { inclusive = true }
                            }
                        })
                    }
                    composable("main")         { MainScreen(navController) }
                    composable("secondary")    { SecondaryScreen(navController) }
                    composable("profile_edit") { ProfileEditScreen(navController) }
                    composable("profile_view") { ProfileViewScreen(navController) }
                    composable("chat")         { ChatScreen(navController) }
                    composable("maps")         { MapsScreen(navController) }
                    composable("video")        { VideoScreen(navController) }
                    composable("microphone")   { MicrophoneScreen(navController) }
                    composable("camera")       { CameraScreen(navController) }
                    composable("api")          { ApiScreen(navController) }
                }
            }
        }
    }
}

data class Message(val author: String, val body: String)

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    MobileComputingTheme { MainScreen(rememberNavController()) }
}