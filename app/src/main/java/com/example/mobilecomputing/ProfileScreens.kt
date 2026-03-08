// ProfileScreens function
//
// Part of HW3, this is where you can edit your user profile: username
// and profile picture

package com.example.mobilecomputing

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfileEditScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var savedImagePath by remember { mutableStateOf<String?>(null) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        db.userProfileDao().getProfile()?.let { profile ->
            username = profile.username
            savedImagePath = profile.imagePath
        }
    }

    val displayUri: Any? = pickedUri ?: savedImagePath?.let { File(it) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedUri = uri
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Edit Profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            if (displayUri != null) {
                AsyncImage(
                    model = displayUri,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No photo", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                photoPicker.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }) {
                Text("Pick Image from Gallery")
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        val finalPath = pickedUri?.let { copyImageToAppStorage(context, it) }
                            ?: savedImagePath
                        db.userProfileDao().upsertProfile(
                            UserProfile(username = username, imagePath = finalPath)
                        )
                        navController.navigate("profile_view") {
                            popUpTo("profile_edit") { inclusive = true }
                        }
                    }
                },
                enabled = username.isNotBlank()
            ) {
                Text("Save & View Profile")
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { navController.popBackStack() }) {
                Text("Back")
            }
        }
    }
}

@Composable
fun ProfileViewScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)

    var profile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        profile = db.userProfileDao().getProfile()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            profile?.let { p ->
                val imgModel: Any? = p.imagePath?.let { File(it) }
                if (imgModel != null) {
                    AsyncImage(
                        model = imgModel,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No photo")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(p.username, style = MaterialTheme.typography.headlineSmall)
            } ?: Text("No profile saved yet.")

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { navController.navigate("profile_edit") }) {
                Text("Edit Profile")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { navController.popBackStack() }) {
                Text("Back")
            }
        }
    }
}

fun copyImageToAppStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out -> inputStream.copyTo(out) }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}