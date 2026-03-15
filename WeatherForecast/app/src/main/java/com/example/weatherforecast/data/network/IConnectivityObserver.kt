package com.example.weatherforecast.data.network

import kotlinx.coroutines.flow.Flow

interface IConnectivityObserver {
    val isOnline: Flow<Boolean>
}
