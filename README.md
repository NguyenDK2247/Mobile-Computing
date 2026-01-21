# HW1 description

### 1. This is how I insert the image into the app and subsequently modify it: 
``` kotlin
Image(
painter = painterResource(R.drawable.profile_picture),
contentDescription = null,
modifier = Modifier
.size(40.dp)
.clip(CircleShape)
.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
)
```

- The line `painter = painterResource(R.drawable.profile_picture)` inserts the image into the project,
and calls the image. In this case, the image is labeled as `profile_picture.png` and the file is located
in the `app/src/main/res/drawable` location.
- `size` confines the image to a certain size.
- `clip` confines the image to a shape, this case `CircleShape`.
- `border` provides the image with a width, color and shape.

### 2. 
``` kotlin
Text(
text = msg.body,
modifier = Modifier.padding(all = 4.dp),
// If the message is expanded, we display all its content
// otherwise we only display the first line
maxLines = if (isExpanded) Int.MAX_VALUE else 1,
style = MaterialTheme.typography.bodyMedium
)   
```
- The text can be changed using `Modifier.padding` and your `MaterialTheme`. In this case, the `maxLines`
command helps limit the content of the messages to just `1` line by default, and only shows the full content
when pressed.

### 3. 
``` kotlin
LazyColumn {
    items(messages) { message ->
        MessageCard(message)
    }
}
```
- The `LazyColumn` function introduces the scrolling list that only composes and lays out 
the currently visible items. In my video, the scrolling **is** there, it just takes a bit of work
for it to be visible.
- Basically, this function here reads through every single `message` of the `messages` coming from
`SampleData.kt`

### 4. 
``` kotlin
// We keep track if the message is expanded or not in this
// variable
var isExpanded by remember { mutableStateOf(false) }
// surfaceColor will be updated gradually from one color to the other
val surfaceColor by animateColorAsState(
    if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
)

// We toggle the isExpanded variable when we click on this Column
Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
    Text(
        text = msg.author,
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.titleSmall
    )
```
- To keep track of the animations and state changes, the functions `remember` and `mutableStateOf` are used.
- `clickable` is then used to signify a change via clicking the message box, and the variable to keep track
of the expansion animation is `isExpanded`, altering between true and false.
- In this example, if the message box is expanded fully, the `primary` `colorScheme` is used, else
the `surface` one is used instead. At the same time, both the `color` and `modifier` (or size of text)
will also change accordingly.