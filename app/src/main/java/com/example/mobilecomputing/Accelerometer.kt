// Accelerometer related functions (this is from HW4, moved to a separate file)
//
// Records phone movement and sends notifications tracking its numbers

package com.example.mobilecomputing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.abs

@Composable
fun AcceleratorMonitor() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastX = 0f; var lastY = 0f; var lastZ = 0f
        val threshold = 0.01f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]; val y = it.values[1]; val z = it.values[2]
                    if (abs(x - lastX) > threshold ||
                        abs(y - lastY) > threshold ||
                        abs(z - lastZ) > threshold
                    ) {
                        lastX = x; lastY = y; lastZ = z
                        ShowAccelerometerNotification(context, x, y, z)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
}

fun ShowAccelerometerNotification(context: Context, x: Float, y: Float, z: Float) {
    val channelId = "accelerometer_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId, "Accelerometer Alerts", NotificationManager.IMPORTANCE_HIGH
        ).apply { enableVibration(true); enableLights(true) }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Acceleration Detected")
        .setContentText("X: %.2f, Y: %.2f, Z: %.2f m/s²".format(x, y, z))
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                "X: %.2f m/s²\nY: %.2f m/s²\nZ: %.2f m/s²".format(x, y, z)
            )
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(pendingIntent)
        .build()
    val nm = NotificationManagerCompat.from(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) nm.notify(1001, notification)
    } else {
        nm.notify(1001, notification)
    }
}