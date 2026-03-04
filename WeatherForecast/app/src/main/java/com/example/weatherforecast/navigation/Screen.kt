package com.example.weatherforecast.navigation


sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object MapPicker : Screen("map_picker")
    object Alerts : Screen("alerts")
    object Settings : Screen("settings")

    object FavoriteDetail : Screen("favorite_detail/{lat}/{lon}/{name}") {
        fun createRoute(lat: Double, lon: Double, name: String): String {
            return "favorite_detail/$lat/$lon/$name"
        }
    }
}
