package com.example.weatherforecast.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weatherforecast.R


@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenMapPicker: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            when (uiState) {
                is SettingsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is SettingsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (uiState as SettingsUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                    }
                }
                is SettingsUiState.Success -> {
                    val settings = uiState as SettingsUiState.Success

                    SettingsSectionTitle(stringResource(R.string.location_section), "📍")
                    SettingsRadioGroup(
                        options = listOf(
                            Triple("gps", stringResource(R.string.gps_option), "🛰️"),
                            Triple("map", stringResource(R.string.map_option), "🗺️")
                        ),
                        selected = settings.locationMode,
                        onSelected = { viewModel.setLocationMode(it) }
                    )

                    if (settings.locationMode == "map") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lat: ${"%.4f".format(settings.mapLat)}, Lon: ${"%.4f".format(settings.mapLon)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                        Button(
                            onClick = onOpenMapPicker,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.pick_on_map))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    SettingsSectionTitle(stringResource(R.string.temperature_unit_section), "🌡️")
                    SettingsRadioGroup(
                        options = listOf(
                            Triple("metric", stringResource(R.string.celsius_option), "°C"),
                            Triple("imperial", stringResource(R.string.fahrenheit_option), "°F"),
                            Triple("standard", stringResource(R.string.kelvin_option), " K")
                        ),
                        selected = settings.temperatureUnit,
                        onSelected = { viewModel.setTemperatureUnit(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    SettingsSectionTitle(stringResource(R.string.wind_speed_unit_section), "🌀")
                    SettingsRadioGroup(
                        options = listOf(
                            Triple("m/s", stringResource(R.string.meter_sec_option), "🌬️"),
                            Triple("mph", stringResource(R.string.miles_hour_option), "💨")
                        ),
                        selected = settings.windSpeedUnit,
                        onSelected = { viewModel.setWindSpeedUnit(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    SettingsSectionTitle(stringResource(R.string.theme_section), "🎨")
                    SettingsRadioGroup(
                        options = listOf(
                            Triple("system", stringResource(R.string.system_default_option), "⚙️"),
                            Triple("light", stringResource(R.string.light_mode_option), "☀️"),
                            Triple("dark", stringResource(R.string.dark_mode_option), "🌙")
                        ),
                        selected = settings.themeMode,
                        onSelected = { viewModel.setThemeMode(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    SettingsSectionTitle(stringResource(R.string.language_section), "🌍")
                    SettingsRadioGroup(
                        options = listOf(
                            Triple("en", stringResource(R.string.english_option), "🇺🇸"),
                            Triple("ar", stringResource(R.string.arabic_option), "🇪🇬")
                        ),
                        selected = settings.language,
                        onSelected = { lang ->
                            viewModel.setLanguage(lang)
                            val activity = context as? Activity
                            if (activity != null) {
                                activity.getSharedPreferences("language_prefs", android.content.Context.MODE_PRIVATE)
                                    .edit().putString("language", lang).commit()
                                LocaleHelper.applyLocaleAndRecreate(activity, lang)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String, icon: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsRadioGroup(
    options: List<Triple<String, String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
            options.forEach { (value, label, icon) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selected == value,
                        onClick = { onSelected(value) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
