package com.example.weatherforecast

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.*
import com.example.weatherforecast.presentation.alerts.AlertScheduler
import com.example.weatherforecast.presentation.alerts.AlertsScreen
import com.example.weatherforecast.presentation.alerts.AlertsViewModel
import com.example.weatherforecast.presentation.alerts.AlertsViewModelFactory
import com.example.weatherforecast.presentation.alerts.WeatherAlertWorker
import com.example.weatherforecast.data.datasource.local.WeatherLocalDataSource
import com.example.weatherforecast.data.datasource.remote.WeatherRemoteDataSource
import com.example.weatherforecast.data.db.WeatherDatabase
import com.example.weatherforecast.presentation.favorites.FavoriteDayDetailScreen
import com.example.weatherforecast.presentation.favorites.FavoriteDetailScreen
import com.example.weatherforecast.presentation.favorites.FavoritesScreen
import com.example.weatherforecast.presentation.favorites.FavoritesViewModel
import com.example.weatherforecast.presentation.favorites.FavoritesViewModelFactory
import com.example.weatherforecast.presentation.favorites.MapPickerScreen
import com.example.weatherforecast.presentation.home.DayDetailScreen
import com.example.weatherforecast.presentation.home.HomeScreen
import com.example.weatherforecast.presentation.splash.SplashScreen
import com.example.weatherforecast.presentation.home.HomeViewModel
import com.example.weatherforecast.presentation.home.HomeViewModelFactory
import com.example.weatherforecast.presentation.navigation.BottomNavBar
import com.example.weatherforecast.presentation.navigation.Screen
import com.example.weatherforecast.data.network.ConnectivityObserver
import com.example.weatherforecast.data.network.RetrofitClient
import com.example.weatherforecast.data.repository.IWeatherRepository
import com.example.weatherforecast.data.repository.WeatherRepositoryImpl
import com.example.weatherforecast.presentation.settings.LocaleHelper
import com.example.weatherforecast.presentation.settings.SettingsDataStore
import com.example.weatherforecast.presentation.settings.SettingsScreen
import com.example.weatherforecast.presentation.settings.SettingsViewModel
import com.example.weatherforecast.presentation.settings.SettingsViewModelFactory
import com.example.weatherforecast.presentation.settings.SettingsMapPickerScreen
import com.example.weatherforecast.ui.theme.TestWeatherForecastTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var repository: IWeatherRepository
    private lateinit var settingsDataStore: SettingsDataStore

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var favoritesViewModel: FavoritesViewModel
    private lateinit var alertsViewModel: AlertsViewModel

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val prefs = newBase.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
            val lang = prefs.getString("language", "en") ?: "en"
            val context = LocaleHelper.setLocale(newBase, lang)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsDataStore =
            SettingsDataStore(
                this
            )
        val prefs = getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        LocaleHelper.setLocale(this, lang)
        runBlocking { settingsDataStore.setLanguage(lang) }


        val database = WeatherDatabase.getInstance(this)
        val remoteDataSource = WeatherRemoteDataSource(RetrofitClient.weatherApiService)
        val localDataSource = WeatherLocalDataSource(
            database.favoriteLocationDao(),
            database.weatherAlertDao(),
            database.cachedForecastDao()
        )
        repository = WeatherRepositoryImpl(remoteDataSource, localDataSource)


        val connectivityObserver = ConnectivityObserver(this)

        homeViewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(
                repository,
                settingsDataStore,
                connectivityObserver
            )
        )[HomeViewModel::class.java]

        settingsViewModel = ViewModelProvider(
            this,
            SettingsViewModelFactory(
                settingsDataStore
            )
        )[SettingsViewModel::class.java]

        favoritesViewModel = ViewModelProvider(
            this,
            FavoritesViewModelFactory(
                repository,
                repository,
                settingsDataStore
            )
        )[FavoritesViewModel::class.java]

        val alertScheduler =
            AlertScheduler(
                applicationContext
            )
        alertsViewModel = ViewModelProvider(
            this,
            AlertsViewModelFactory(
                repository,
                alertScheduler,
                settingsDataStore
            )
        )[AlertsViewModel::class.java]



        setContent {
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = "system")
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemDark
            }

            TestWeatherForecastTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute != Screen.Splash.route) {
                            BottomNavBar(navController)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Splash.route) {
                            SplashScreen(navController = navController)
                        }

                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onDayClick = { date ->
                                    navController.navigate(Screen.DayDetail.createRoute(date))
                                }
                            )
                        }

                        composable(Screen.Favorites.route) {
                            FavoritesScreen(
                                viewModel = favoritesViewModel,
                                onAddClick = {
                                    navController.navigate(Screen.MapPicker.route)
                                },
                                onFavoriteClick = { id, name ->
                                    favoritesViewModel.loadFavoriteDetail(id)
                                    navController.navigate(
                                        Screen.FavoriteDetail.createRoute(id, name)
                                    )
                                }
                            )
                        }

                        composable(Screen.MapPicker.route) {
                            MapPickerScreen(
                                viewModel = favoritesViewModel,
                                onSave = { name, lat, lon ->
                                    favoritesViewModel.addFavorite(name, lat, lon)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.FavoriteDetail.route,
                            arguments = listOf(
                                navArgument("id") { type = NavType.IntType },
                                navArgument("name") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("name") ?: ""
                            FavoriteDetailScreen(
                                locationName = name,
                                viewModel = favoritesViewModel,
                                onBack = { navController.popBackStack() },
                                onDayClick = { date ->
                                    navController.navigate(Screen.FavoriteDayDetail.createRoute(date))
                                }
                            )
                        }

                        composable(
                            route = Screen.FavoriteDayDetail.route,
                            arguments = listOf(
                                navArgument("date") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val date = backStackEntry.arguments?.getString("date") ?: ""
                            FavoriteDayDetailScreen(
                                date = date,
                                viewModel = favoritesViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Alerts.route) {
                            AlertsScreen(viewModel = alertsViewModel)
                        }

                        composable(
                            route = Screen.DayDetail.route,
                            arguments = listOf(
                                navArgument("date") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val date = backStackEntry.arguments?.getString("date") ?: ""
                            DayDetailScreen(
                                date = date,
                                viewModel = homeViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onOpenMapPicker = {
                                    navController.navigate(Screen.SettingsMapPicker.route)
                                }
                            )
                        }

                        composable(Screen.SettingsMapPicker.route) {
                            SettingsMapPickerScreen(
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        scheduleAlertWorker()
    }

    private fun scheduleAlertWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<WeatherAlertWorker>(
            1, TimeUnit.HOURS
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WeatherAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }
}