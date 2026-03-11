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
import com.example.weatherforecast.alerts.AlertsScreen
import com.example.weatherforecast.alerts.AlertsViewModel
import com.example.weatherforecast.alerts.AlertsViewModelFactory
import com.example.weatherforecast.alerts.WeatherAlertWorker
import com.example.weatherforecast.datasource.local.WeatherLocalDataSource
import com.example.weatherforecast.datasource.remote.WeatherRemoteDataSource
import com.example.weatherforecast.db.WeatherDatabase
import com.example.weatherforecast.favorites.FavoriteDetailScreen
import com.example.weatherforecast.favorites.FavoritesScreen
import com.example.weatherforecast.favorites.FavoritesViewModel
import com.example.weatherforecast.favorites.FavoritesViewModelFactory
import com.example.weatherforecast.favorites.MapPickerScreen
import com.example.weatherforecast.home.DayDetailScreen
import com.example.weatherforecast.home.HomeScreen
import com.example.weatherforecast.splash.SplashScreen
import com.example.weatherforecast.home.HomeViewModel
import com.example.weatherforecast.home.HomeViewModelFactory
import com.example.weatherforecast.navigation.BottomNavBar
import com.example.weatherforecast.navigation.Screen
import com.example.weatherforecast.network.ConnectivityObserver
import com.example.weatherforecast.network.RetrofitClient
import com.example.weatherforecast.repository.IWeatherRepository
import com.example.weatherforecast.repository.WeatherRepositoryImpl
import com.example.weatherforecast.settings.LocaleHelper
import com.example.weatherforecast.settings.SettingsDataStore
import com.example.weatherforecast.settings.SettingsScreen
import com.example.weatherforecast.settings.SettingsViewModel
import com.example.weatherforecast.settings.SettingsViewModelFactory
import com.example.weatherforecast.settings.SettingsMapPickerScreen
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

        settingsDataStore = SettingsDataStore(this)
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
            this, HomeViewModelFactory(repository, settingsDataStore, connectivityObserver)
        )[HomeViewModel::class.java]

        settingsViewModel = ViewModelProvider(
            this, SettingsViewModelFactory(settingsDataStore)
        )[SettingsViewModel::class.java]

        favoritesViewModel = ViewModelProvider(
            this, FavoritesViewModelFactory(repository, settingsDataStore)
        )[FavoritesViewModel::class.java]

        alertsViewModel = ViewModelProvider(
            this, AlertsViewModelFactory(repository, applicationContext, settingsDataStore)
        )[AlertsViewModel::class.java]


        scheduleAlertWorker()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }


        val locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fineGranted || coarseGranted) {
                fetchLocationAndLoadForecast()
            } else {

                homeViewModel.fetchForecast()
            }
        }

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
                                repository = repository,
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
                            com.example.weatherforecast.favorites.FavoriteDayDetailScreen(
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


        if (hasLocationPermission()) {
            fetchLocationAndLoadForecast()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun fetchLocationAndLoadForecast() {
        if (!hasLocationPermission()) {
            homeViewModel.fetchForecast()
            return
        }
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    homeViewModel.fetchForecast(location.latitude, location.longitude)
                } else {

                    homeViewModel.fetchForecast()
                }
            }.addOnFailureListener {
                homeViewModel.fetchForecast()
            }
        } catch (e: SecurityException) {
            homeViewModel.fetchForecast()
        }
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