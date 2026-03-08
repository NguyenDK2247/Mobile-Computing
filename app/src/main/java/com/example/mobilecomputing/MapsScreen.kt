// MapsScreen function
//
// By default shows user Oulu, can drop pin anywhere on the map

package com.example.mobilecomputing

import android.Manifest
import android.content.pm.PackageManager
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapsScreen(navController: NavHostController) {
    val context = LocalContext.current

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> locationPermissionGranted = granted }

    // Default camera: Helsinki, Finland
    val oulu = LatLng(65.0121, 25.4651)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(oulu, 12f)
    }

    // Track tapped marker positions
    var markers by remember { mutableStateOf(listOf(oulu)) }
    var selectedMarker by remember { mutableStateOf<LatLng?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { navController.popBackStack() }) { Text("← Back") }
            Spacer(modifier = Modifier.weight(1f))
            Text("Map", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(1f))
        }

        if (!locationPermissionGranted) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Location permission is needed for a better map experience.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) {
                    Text("Grant Location Permission")
                }
            }
        }

        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = locationPermissionGranted),
            uiSettings = MapUiSettings(myLocationButtonEnabled = locationPermissionGranted),
            onMapClick = { latLng ->
                markers = markers + latLng
                selectedMarker = latLng
            }
        ) {
            markers.forEachIndexed { idx, pos ->
                Marker(
                    state = MarkerState(position = pos),
                    title = if (idx == 0) "Oulu" else "Pin #$idx",
                    snippet = "Lat: %.4f, Lng: %.4f".format(pos.latitude, pos.longitude),
                    onClick = { _ ->
                        selectedMarker = pos
                        false
                    }
                )
            }
        }

        // Info panel
        selectedMarker?.let { pos ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Selected: %.5f, %.5f".format(pos.latitude, pos.longitude),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Tap anywhere on the map to drop a pin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}