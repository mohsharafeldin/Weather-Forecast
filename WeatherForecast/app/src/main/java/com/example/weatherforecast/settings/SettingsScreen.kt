package com.example.weatherforecast.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenMapPicker: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )


        SettingsSectionTitle("Location")
        SettingsRadioGroup(
            options = listOf("gps" to "GPS (Current Location)", "map" to "Choose from Map"),
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
                Text("Pick on Map")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))


        SettingsSectionTitle("Temperature Unit")
        SettingsRadioGroup(
            options = listOf(
                "metric" to "Celsius (°C)",
                "imperial" to "Fahrenheit (°F)",
                "standard" to "Kelvin (K)"
            ),
            selected = uiState.temperatureUnit,
            onSelected = { viewModel.setTemperatureUnit(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))


        SettingsSectionTitle("Wind Speed Unit")
        SettingsRadioGroup(
            options = listOf("m/s" to "Meter/Sec", "mph" to "Miles/Hour"),
            selected = uiState.windSpeedUnit,
            onSelected = { viewModel.setWindSpeedUnit(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))


        SettingsSectionTitle("Language")
        SettingsRadioGroup(
            options = listOf("en" to "English", "ar" to "Arabic"),
            selected = uiState.language,
            onSelected = { viewModel.setLanguage(it) }
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
