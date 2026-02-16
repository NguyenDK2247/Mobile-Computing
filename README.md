# HW4 description

### 1. To enable/prompt notification permissions with a button, for example, I created this function:
``` kotlin
private fun EnableNotifications(
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
```

* This function's main purpose is to check if the permission is already granted. If not, then the app will prompt the user
to enable/disable notifications. If already have, then the user is met with `"Notifications already enabled"` instead, and no notifications.
* The function is then passed inside a function that makes a button functional. This is where the user can physically
ask the app to enable notifications inside the app:
``` kotlin
private fun EnableNotificationButton() {
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

    Button(
        onClick = {
            EnableNotifications(context, permissionLauncher)
        },
    ) {
        Text("Enable Notifications")
    }
}
```
* Here `EnableNotifications(context, permissionLauncher)` is activated upon clicking
on the button.

### 2. By implementing the `ShowNotification()` function, the user can visibly see the notification sent by the app:
``` kotlin
private fun ShowNotification(context: Context) {
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
```

* Make the ID of the notifications a CONSTANT `val notificationId = 1` so that the notifications are
not triggered every now and then and instead only once when the user does something
to trigger them.

### 3. To make the notification interactable:
* Create `intent` and `pendingIntent` values so that tapping on the notification
actually leads to something; in other words, makes the notification interactable.
* To make the notification visible as a drop-down, in creating the `notification` value,
set priority to high `.setPriority(NotificationCompat.PRIORITY_HIGH)`.
* Finally, assign `pendingIntent` for the notification `.setContentIntent(pendingIntent)`.

### 4. The sensor I used for this assignment is the accelerator:
``` kotlin
@Composable
fun AcceleratorMonitor() {
    val context = LocalContext.current
    var accelData by remember { mutableStateOf("Waiting for accelerometer data...") }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastX = 0f
        var lastY = 0f
        var lastZ = 0f
        val threshold = 0.01f // minimum change to trigger notification (in m/s²)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    // update display with actual current values
                    accelData = "X: %.2f m/s²\nY: %.2f m/s²\nZ: %.2f m/s²".format(x, y, z)

                    // calculate the difference from last reading
                    val deltaX = Math.abs(x - lastX)
                    val deltaY = Math.abs(y - lastY)
                    val deltaZ = Math.abs(z - lastZ)

                    // only send notification if there's a change detected
                    if (deltaX > threshold || deltaY > threshold || deltaZ > threshold) {
                        ShowAccelerometerNotification(context, x, y, z)
                        lastX = x
                        lastY = y
                        lastZ = z
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(
                listener,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
        } else {
            accelData = "Accelerometer not available"
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}

private fun ShowAccelerometerNotification(context: Context, x: Float, y: Float, z: Float) {
    val channelId = "accelerometer_channel"
    val notificationId = 1001

    // create notification channel
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Accelerometer Alerts",
            // this shows the notification as a drop-down banner
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for accelerometer sensor changes"
            enableVibration(true)
            enableLights(true)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // create an Intent to open the app
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // build the notification
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Acceleration Detected")
        .setContentText("X: %.2f, Y: %.2f, Z: %.2f m/s²".format(x, y, z))
        .setStyle(NotificationCompat.BigTextStyle()
            .bigText("Accelerometer reading:\nX-axis: %.2f m/s²\nY-axis: %.2f m/s²\nZ-axis: %.2f m/s²".format(x, y, z)))
        // this shows the notification as a drop-down banner
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        // assign functionality for tapping on the notification
        .setContentIntent(pendingIntent)
        .build()

    // show the notification
    val notificationManager = NotificationManagerCompat.from(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(notificationId, notification)
        }
    } else {
        notificationManager.notify(notificationId, notification)
    }
}
```
* It is built here so that a change of only 0.01 in **any** direction is needed
to trigger the notification `val threshold = 0.01f`, and when it does so, it updates the accelerator status
as a drop-down notification, albeit not exactly when the user stops moving the scroll bars.
* The notification mechanism here works just like when the `EnableNotifications()`
function is implemented.
* Finally, implement the `AcceleratorMonitor()` function by adding it into the very start of `MainScreen()`.

## To start off the screen capture, I set the app to notifications not enabled by default.