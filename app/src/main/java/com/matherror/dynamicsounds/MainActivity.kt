package com.matherror.dynamicsounds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.matherror.dynamicsounds.ui.AppSelectionScreen
import com.matherror.dynamicsounds.ui.HomeScreen
import com.matherror.dynamicsounds.ui.RulesScreen
import com.matherror.dynamicsounds.ui.SequenceScreen
import com.matherror.dynamicsounds.ui.SettingsScreen
import com.matherror.dynamicsounds.ui.theme.RandomNotificationSoundTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.rotate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RandomNotificationSoundTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        Screen.Home,
        Screen.Sequences,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(80.dp),
                containerColor = Color.Transparent
            ) {
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val rotation by animateFloatAsState(
                        targetValue = if (selected) 8f else 0f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    )
                    NavigationBarItem(
                        icon = {
                            if (screen.icon is androidx.compose.ui.graphics.vector.ImageVector) {
                                Icon(
                                    screen.icon as androidx.compose.ui.graphics.vector.ImageVector,
                                    contentDescription = screen.label,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .rotate(rotation)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(screen.icon as Int),
                                    contentDescription = screen.label,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .rotate(rotation)
                                )
                            }
                        },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Sequences.route) { SequenceScreen() }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            composable(Screen.AppSelection.route) { AppSelectionScreen(navController) }
        }
    }
}

sealed class Screen(val route: String, val icon: Any, val label: String) {
    object Home : Screen("home", Icons.Filled.Home, "Home")
    object Sequences : Screen("sequences", R.drawable.ic_sequence, "Sequences")
    object Settings : Screen("settings", Icons.Filled.Settings, "Settings")
    object AppSelection : Screen("app_selection", Icons.Filled.Settings, "App Selection")
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RandomNotificationSoundTheme {
        Greeting("Android")
    }
}