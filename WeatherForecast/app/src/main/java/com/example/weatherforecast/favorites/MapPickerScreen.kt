package com.example.weatherforecast.favorites

import android.location.Geocoder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weatherforecast.model.GeocodingResult
import com.example.weatherforecast.repository.IWeatherRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    repository: IWeatherRepository,
    onSave: (name: String, lat: Double, lon: Double) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val defaultPosition = LatLng(30.0444, 31.2357)

    var selectedPosition by remember { mutableStateOf<LatLng?>(null) }
    var resolvedCityName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocodingResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 5f)
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length < 2) {
            searchResults = emptyList()
            showResults = false
            return@LaunchedEffect
        }
        delay(300)
        isSearching = true
        try {
            val results = repository.searchCity(searchQuery).first()
            searchResults = results
            showResults = results.isNotEmpty()
        } catch (_: Exception) {
            searchResults = emptyList()
            showResults = false
        }
        isSearching = false
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
            resolvedCityName = "Lat: ${"%.4f".format(latLng.latitude)}, Lon: ${"%.4f".format(latLng.longitude)}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick a Location") },
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
                    errorMessage = null
                    showResults = false
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

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search for a city...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                searchResults = emptyList()
                                showResults = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = showResults) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 250.dp)
                        ) {
                            items(searchResults) { result ->
                                val displayName = buildString {
                                    append(result.name)
                                    if (!result.state.isNullOrBlank()) append(", ${result.state}")
                                    append(", ${result.country}")
                                }
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = displayName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = "Lat: ${"%.4f".format(result.lat)}, Lon: ${"%.4f".format(result.lon)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        val latLng = LatLng(result.lat, result.lon)
                                        selectedPosition = latLng
                                        resolvedCityName = result.name
                                        searchQuery = displayName
                                        showResults = false
                                        errorMessage = null
                                        cameraPositionState.move(
                                            CameraUpdateFactory.newLatLngZoom(latLng, 10f)
                                        )
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }

                if (isSearching) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary
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
                            text = "Tap on the map or search for a city",
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

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            val pos = selectedPosition
                            when {
                                pos == null -> errorMessage = "Please tap on the map to select a location"
                                resolvedCityName.isBlank() -> errorMessage = "Unable to resolve location name"
                                else -> onSave(resolvedCityName.trim(), pos.latitude, pos.longitude)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedPosition != null
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Location", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
