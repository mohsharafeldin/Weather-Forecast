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

    Column(
        modifier = Modifier
            .fillMaxSize()
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


        SettingsSectionTitle(stringResource(R.string.location_section))
        SettingsRadioGroup(
            options = listOf(
                "gps" to stringResource(R.string.gps_option),
                "map" to stringResource(R.string.map_option)
            ),
            selected = uiState.locationMode,
            onSelected = { viewModel.setLocationMode(it) }
        )

        if (uiState.locationMode == "map") {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lat: ${"%.4f".format(uiState.mapLat)}, Lon: ${"%.4f".format(uiState.mapLon)}",
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


        SettingsSectionTitle(stringResource(R.string.temperature_unit_section))
        SettingsRadioGroup(
            options = listOf(
                "metric" to stringResource(R.string.celsius_option),
                "imperial" to stringResource(R.string.fahrenheit_option),
                "standard" to stringResource(R.string.kelvin_option)
            ),
            selected = uiState.temperatureUnit,
            onSelected = { viewModel.setTemperatureUnit(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))


        SettingsSectionTitle(stringResource(R.string.wind_speed_unit_section))
        SettingsRadioGroup(
            options = listOf(
                "m/s" to stringResource(R.string.meter_sec_option),
                "mph" to stringResource(R.string.miles_hour_option)
            ),
            selected = uiState.windSpeedUnit,
            onSelected = { viewModel.setWindSpeedUnit(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))


        SettingsSectionTitle(stringResource(R.string.language_section))
        SettingsRadioGroup(
            options = listOf(
                "en" to stringResource(R.string.english_option),
                "ar" to stringResource(R.string.arabic_option)
            ),
            selected = uiState.language,
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

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsRadioGroup(
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column {
        options.forEach { (value, label) ->
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
                Text(text = label, fontSize = 16.sp)
            }
        }
    }
}
