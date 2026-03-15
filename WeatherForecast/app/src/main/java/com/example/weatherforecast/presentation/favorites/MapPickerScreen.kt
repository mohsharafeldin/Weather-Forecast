package com.example.weatherforecast.presentation.favorites

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weatherforecast.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    viewModel: FavoritesViewModel,
    onSave: (name: String, lat: Double, lon: Double) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val defaultPosition = LatLng(30.0444, 31.2357)

    var selectedPosition by remember { mutableStateOf<LatLng?>(null) }
    var resolvedCityName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    var showResults by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 5f)
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length < 2) {
            viewModel.clearSearchResults()
            showResults = false
            return@LaunchedEffect
        }
        delay(300)
        viewModel.searchCity(searchQuery)
    }

    LaunchedEffect(searchResults) {
        showResults = searchResults.isNotEmpty()
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
                title = { Text(stringResource(R.string.pick_a_location)) },
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
                        title = resolvedCityName.ifBlank { stringResource(R.string.selected_location) },
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
                    placeholder = { Text(stringResource(R.string.search_city)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.clearSearchResults()
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
                                    append(", ${Locale("", result.country).getDisplayCountry(Locale.getDefault())}")
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
                            text = stringResource(R.string.tap_on_map),
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
                                pos == null -> errorMessage = context.getString(R.string.error_select_location)
                                resolvedCityName.isBlank() -> errorMessage = context.getString(R.string.error_resolve_location)
                                else -> onSave(resolvedCityName.trim(), pos.latitude, pos.longitude)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedPosition != null
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.save_location), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
