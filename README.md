# HW2 description

### 1. To implement navigation into the application, first it's important to get the right imports:
``` kotlin
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
```

- Then define a `navController` using `rememberNavController()`, the `modifier` to be used for later:
``` kotlin
val modifier = Modifier
val navController = rememberNavController()
```

- Create a `NavHost` block that sets the address of the navigation screens.
Here we set the `startDestination` page as `main`, and navigating to `secondary`:
``` kotlin
NavHost(modifier = modifier, navController = navController, startDestination = "main") {
    composable("main") { MainScreen(navController) }
    composable("secondary") { SecondaryScreen(navController) }
}
```

- Once that is set up, now we have two functions, corresponding with two different screens:
``` kotlin
@Composable
fun MainScreen(navController: NavHostController) {
    var buttonClicked by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
            ) {
                items(SampleData.conversationSample) {
                    message -> MessageCard(message)
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text(
                                text = "Click this button below for a surprise :3",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    buttonClicked = !buttonClicked
                                    navController.navigate("secondary")
                                },
                            ) {
                                Text("Go!")
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- There are a few important implementations here:
  - `fun MainScreen(navController: NavHostController)`: set the previously defined
`navController` in relation to `navHostController`.
  - `navController.navigate("secondary")`, this is for the button that will direct the user to the other page(s).
In this case, it is `secondary`.

- We do similar implementation for the other function, but there is another required addition, which we will get back to later.
- For the button that navigates pages:
  - First define a boolean variable `var buttonClicked by remember { mutableStateOf(false) }`.
  - Then set it inside `Button` where `buttonClicked` is altered between `true` and `false`:
    ``` kotlin
    Button(
        onClick = {
            buttonClicked = !buttonClicked
            navController.navigate("secondary")
        },
    ) {
        Text("Go!")
    }
    ```
    
### 2. Circular navigation prevention
- Only one line is needed:
``` kotlin
navController.popBackStack()
```
This occurs in the `Button` block of the **non-main** function.