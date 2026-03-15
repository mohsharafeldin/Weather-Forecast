package com.example.weatherforecast.data.model

import com.google.gson.annotations.SerializedName


data class WeatherResponse(
    @SerializedName("cod") val cod: String,
    @SerializedName("message") val message: Int,
    @SerializedName("cnt") val cnt: Int,
    @SerializedName("list") val list: List<WeatherItem>,
    @SerializedName("city") val city: CityData
)

data class WeatherItem(
    @SerializedName("dt") val dt: Long,
    @SerializedName("main") val main: MainData,
    @SerializedName("weather") val weather: List<WeatherDescription>,
    @SerializedName("clouds") val clouds: CloudsData,
    @SerializedName("wind") val wind: WindData,
    @SerializedName("visibility") val visibility: Int,
    @SerializedName("pop") val pop: Double,
    @SerializedName("dt_txt") val dtTxt: String
)

data class MainData(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    @SerializedName("pressure") val pressure: Int,
    @SerializedName("humidity") val humidity: Int,
    @SerializedName("sea_level") val seaLevel: Int = 0,
    @SerializedName("grnd_level") val grndLevel: Int = 0
)

data class WeatherDescription(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)

data class WindData(
    @SerializedName("speed") val speed: Double,
    @SerializedName("deg") val deg: Int,
    @SerializedName("gust") val gust: Double = 0.0
)

data class CloudsData(
    @SerializedName("all") val all: Int
)

data class CityData(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("coord") val coord: Coord,
    @SerializedName("country") val country: String,
    @SerializedName("population") val population: Int,
    @SerializedName("timezone") val timezone: Int,
    @SerializedName("sunrise") val sunrise: Long,
    @SerializedName("sunset") val sunset: Long
)

data class Coord(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
)
