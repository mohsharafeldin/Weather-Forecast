package com.example.weatherforecast.presentation.navigation


sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object MapPicker : Screen("map_picker")
    object Alerts : Screen("alerts")
    object Settings : Screen("settings")
    object SettingsMapPicker : Screen("settings_map_picker")

    object FavoriteDetail : Screen("favorite_detail/{id}/{name}") {
        fun createRoute(id: Int, name: String): String {
            return "favorite_detail/$id/$name"
        }
    }

    object DayDetail : Screen("day_detail/{date}") {
        fun createRoute(date: String): String {
            return "day_detail/$date"
        }
    }

    object FavoriteDayDetail : Screen("favorite_day_detail/{date}") {
        fun createRoute(date: String): String {
            return "favorite_day_detail/$date"
        }
    }
}
