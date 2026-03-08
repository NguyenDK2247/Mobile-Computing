# Project branch
## Compensation for HW3
* First, everything has to be built as a foundation for the Database.
I did this in `AppDatabase.kt`:
```kotlin
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val username: String,
    val imagePath: String? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val author: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage)
}

@Database(
    entities = [UserProfile::class, ChatMessage::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .allowMainThreadQueries()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
```

* Then, apply that to the message column that has been part of the `MainScreen` since HW1:
``` kotlin
val db = AppDatabase.getInstance(context)

    var username by remember { mutableStateOf("Lexi") }
    var imagePath by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        db.userProfileDao().getProfile()?.let {
            username = it.username
            imagePath = it.imagePath
        }
    }
```

* The username is set default to "Lexi" and the profile picture 
an anonymous figure. To be able to change those, I created `ProfileScreens.kt`
that can be triggered by clicking the "👤 Profile" button at the bottom
of the `MainScreen`:
```kotlin
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
```

* It allows users to select an image in the phone's gallery and choose a
name they would like. The changes remain even after relaunching the app;
to reset both details, uninstall the app and then rerun the program code.

### Questions for HW3:
> What are you storing in a database in your app? 
* 2 tables:
    * `user_profile`: a single row containing an `username` and `imagePath`.
  `username` is the username and `imagePath` is the file path  (not the image itself)
  to the profile image stored in storage.
    * `chat_messages`: a table containing `author`, `body`, and `timestamp`.
> How many bytes is it estimated to take? 
* `user_profile`:
  * ID: from **2 to 4 bytes**.
  * Username: about **16 to 32 bytes**.
  * Image path: about **16 to 64 bytes**.
* `chat_messages` (per row):
  * ID: from **2 to 4 bytes**.
  * `author`: about **16 to 32 bytes**.
  * `body`: about **128 to 512 bytes**.
  * `timestamp`: from **8 to 12 bytes**.
> What other things could you store in a database that would improve the user experience?
* Save map pins from `MapsScreen`. So far you can drop the pins but they 
reset as soon as the user briefly leaves the maps screen.
* App preferences. Dark mode, zoom level, and better notification preferences
implementation are some of the ideas that can enhance user experience.

## Other implementaions for the project

### 1. A separate and dedicated message screen from scratch
* Can be opened by clicking the "💬 Chat" button.
* The code for `ChatScreen.kt`:
```kotlin
@Composable
fun ChatScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val messages by db.chatMessageDao().getAllMessages().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var authorName by remember { mutableStateOf("Me") }

    LaunchedEffect(Unit) {
        db.userProfileDao().getProfile()?.let { authorName = it.username }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { navController.popBackStack() }) { Text("← Back") }
                Spacer(modifier = Modifier.weight(1f))
                Text("Chat", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.weight(1f))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                items(messages) { msg ->
                    PersistentMessageCard(msg)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type a message…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotEmpty()) {
                            scope.launch {
                                db.chatMessageDao().insertMessage(
                                    ChatMessage(author = authorName, body = text)
                                )
                            }
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank()
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun PersistentMessageCard(msg: ChatMessage) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.profile_picture),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                modifier = Modifier.padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
```

* The user can type anything they want, but the username and profile picture
will only always appear as "Me" and `profile_picture.png` respectively.

### 2. Weather API
* Can be accessed via "🌤 Weather API" button.
* The code for this is from `ApiScreen.kt`:
```kotlin
data class WeatherData(
    val temperature: Double,
    val windspeed: Double,
    val weatherCode: Int
)

fun weatherCodeToDescription(code: Int): String = when (code) {
    0 -> "Clear sky ☀️"
    1, 2, 3 -> "Partly cloudy ⛅"
    45, 48 -> "Foggy 🌫️"
    51, 53, 55 -> "Drizzle 🌦️"
    61, 63, 65 -> "Rain 🌧️"
    71, 73, 75 -> "Snow 🌨️"
    80, 81, 82 -> "Rain showers 🌩️"
    95 -> "Thunderstorm ⛈️"
    else -> "Unknown ($code)"
}

@Composable
fun ApiScreen(navController: NavHostController) {
    var weatherData by remember { mutableStateOf<WeatherData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun fetchWeather() {
        isLoading = true
        error = null
        weatherData = null
        try {
            val result = withContext(Dispatchers.IO) {
                val url = "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=65.0121&longitude=25.4651" +
                        "&current_weather=true"
                val json = URL(url).readText()
                val obj = JSONObject(json).getJSONObject("current_weather")
                WeatherData(
                    temperature = obj.getDouble("temperature"),
                    windspeed = obj.getDouble("windspeed"),
                    weatherCode = obj.getInt("weathercode")
                )
            }
            weatherData = result
        } catch (e: Exception) {
            error = "Failed to fetch: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Fetch on first composition
    LaunchedEffect(Unit) { fetchWeather() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { navController.popBackStack() }) { Text("← Back") }
            Spacer(modifier = Modifier.weight(1f))
            Text("Weather API", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Current Weather — Oulu",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Source: Open-Meteo (open-meteo.com)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            when {
                isLoading -> CircularProgressIndicator()
                error != null -> {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { scope.launch { fetchWeather() } }) { Text("Retry") }
                }
                weatherData != null -> {
                    val w = weatherData!!
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                weatherCodeToDescription(w.weatherCode),
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "🌡 Temperature: ${w.temperature} °C",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "💨 Wind speed: ${w.windspeed} km/h",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { scope.launch { fetchWeather() } }) { Text("Refresh") }
                }
            }
        }
    }
}
```

* Retrieves weather information from [Open-Meteo](https://open-meteo.com/) and in this case
I take the available weather API of Oulu, getting its latitude and longitude numbers.
* Information includes the state of the weather, temperature (in Celsius) and wind speed (in km/h).

### 3. Maps SDK
* Can be accessed via "🗺 Maps" button.
* In this case, the location is set default to Oulu. In other words,
upon opening the map, Oulu will appear in front of the user.
* Here, the user can drop as many markers as they would like, but
the pins will not be saved when the user leaves the `MapsScreen`.
* The code for this is from `MapsScreen.kt`:
```kotlin
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
```

### 4. Animated splash screen
* User can see it upon opening the app. There is a little animation
lasting about 2 seconds before the screen displays the `MainScreen` content.
* The code for this is from `SplashScreen.kt`:
```kotlin
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Run scale and fade-in concurrently using coroutineScope + launch
        coroutineScope {
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            alpha.animateTo(1f, animationSpec = tween(600))
        }

        delay(1200)

        // Fade out
        alpha.animateTo(0f, animationSpec = tween(400))

        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Text(
                text = "📱",
                fontSize = 72.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Mobile Computing",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "If you can see this text, you are very cool ^_^",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
        }
    }
}
```

### 5. Video playback
* Can be accessed via "🎬 Video" button.
* The code for this is from `VideoScreen.kt`:
``` kotlin
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
```
* You can pick a video from the gallery, and there are a few tools that
can be used:
  * Play/pause/forward and backward 5 seconds.
  * Playback speed is adjustable, from 0.25x up to 2x.
  * Finally, you can clear the video and select another one in the gallery.
  
### 6. Runtime permissions
* Every functionality here so far has required notification before using
them.
* For example, `MapsScreen.kt` uses:
``` kotlin
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
```
* The functions that use the same structure of enabling permissions are in
`Notifications.kt`, `ApiScreen.kt` and `VideoScreen.kt`

### 7. Camera

### 8. Microphone

### Weekly design assignments that are included here: 3, 4, 5, 7
* They are included in a `.zip` file that also contains a video recording to the functionalities
of this app.