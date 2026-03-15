package com.example.weatherforecast.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val label = stringResource(item.labelResId)
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = label) },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (currentRoute == item.route) FontWeight.Bold else FontWeight.Medium
                    )
                },
                selected = currentRoute == item.route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
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
