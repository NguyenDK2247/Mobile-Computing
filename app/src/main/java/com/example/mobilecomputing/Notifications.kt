// Notifications function (from HW4, separated to a file)
//
// This ie only for the accelerometer function from Accelerometer.kt, it doesn't
// trigger the other notifications

package com.example.mobilecomputing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

@Composable
fun EnableNotificationButton() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
            ShowNotification(context)
        } else {
            Toast.makeText(context, "Notifications permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(onClick = { EnableNotifications(context, permissionLauncher) }) {
            Text("Enable Notifications")
        }
    }
}

fun EnableNotifications(
    context: Context,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(context, "Notifications already enabled", Toast.LENGTH_SHORT).show()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    } else {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.areNotificationsEnabled()) {
            Toast.makeText(context, "Notifications already enabled", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }
}

fun ShowNotification(context: Context) {
    val channelId = "default_channel"
    val notificationId = 1

    // create notification channel (required for Android 8.0+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Default Notifications",
            // this shows the notification as a drop-down banner
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Default notification channel"
            enableVibration(true)
            enableLights(true)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // give functionality to tapping on the notification
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    // create PendingIntent
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // build the notification
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Notifications Enabled!")
        .setContentText("You will be notified when the device receives notifications")
        // this shows the notification as a drop-down banner
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        // assign functionality for tapping on the notification
        .setContentIntent(pendingIntent)
        .build()

    // show the notification
    val notificationManager = NotificationManagerCompat.from(context)
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        notificationManager.notify(notificationId, notification)
    }
}