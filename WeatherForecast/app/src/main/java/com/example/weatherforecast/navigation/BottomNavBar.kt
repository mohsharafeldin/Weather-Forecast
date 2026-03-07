package com.example.weatherforecast.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.weatherforecast.R


data class BottomNavItem(
    val labelResId: Int,
    val icon: ImageVector,
    val route: String
)


@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem(R.string.nav_home, Icons.Default.Home, Screen.Home.route),
        BottomNavItem(R.string.nav_favorites, Icons.Default.Favorite, Screen.Favorites.route),
        BottomNavItem(R.string.nav_alerts, Icons.Default.Notifications, Screen.Alerts.route),
        BottomNavItem(R.string.nav_settings, Icons.Default.Settings, Screen.Settings.route)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            val label = stringResource(item.labelResId)
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
