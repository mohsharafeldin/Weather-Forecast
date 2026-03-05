package com.example.weatherforecast.settings

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMapPickerScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val initialPosition = LatLng(uiState.mapLat, uiState.mapLon)

    var selectedPosition by remember { mutableStateOf<LatLng?>(null) }
    var resolvedCityName by remember { mutableStateOf("") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 5f)
    }

    fun resolveLocationName(latLng: LatLng) {
        try {
            @Suppress("DEPRECATION")
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            resolvedCityName = if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: address.countryName
                    ?: "Lat: ${"%.4f".format(latLng.latitude)}, Lon: ${"%.4f".format(latLng.longitude)}"
            } else {
                "Lat: ${"%.4f".format(latLng.latitude)}, Lon: ${"%.4f".format(latLng.longitude)}"
            }
        } catch (e: Exception) {
            resolvedCityName =
                "Lat: ${"%.4f".format(latLng.latitude)}, Lon: ${"%.4f".format(latLng.longitude)}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick Location") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    selectedPosition = latLng
                    resolveLocationName(latLng)
                }
            ) {
                selectedPosition?.let { position ->
                    Marker(
                        state = MarkerState(position = position),
                        title = resolvedCityName.ifBlank { "Selected Location" },
                        snippet = "Lat: ${"%.4f".format(position.latitude)}, Lon: ${"%.4f".format(position.longitude)}"
                    )
                }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedPosition == null) {
                        Text(
                            text = "Tap on the map to select a location",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = resolvedCityName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Lat: ${"%.4f".format(selectedPosition!!.latitude)}, Lon: ${"%.4f".format(selectedPosition!!.longitude)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                        onClick = {
                            selectedPosition?.let { pos ->
                                viewModel.setMapCoordinates(pos.latitude, pos.longitude)
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedPosition != null
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Location", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
