// Branch HW2

package com.example.mobilecomputing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobilecomputing.ui.theme.MobileComputingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileComputingTheme {
                // Create a NavHost with NavController
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") { MainScreen(navController) }
                    composable("complex") { ComplexScreen(navController) }
                }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavController) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Main Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("complex") }) {
                Text("Go to Complex Screen")
            }
        }
    }
}

@Composable
fun ComplexScreen(navController: NavController) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Complex Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                navController.navigate("main") {
                    popUpTo("main") { inclusive = true }
                }
            }) {
                Text("Back to Main Screen")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    MobileComputingTheme { MainScreen(navController = rememberNavController()) }
}

@Preview(showBackground = true)
@Composable
fun PreviewComplexScreen() {
    MobileComputingTheme { ComplexScreen(navController = rememberNavController()) }
}
