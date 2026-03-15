package com.example.weatherforecast.utils

import com.example.weatherforecast.data.model.WeatherItem
import com.example.weatherforecast.data.model.MainData
import com.example.weatherforecast.data.model.WindData
import com.example.weatherforecast.data.model.CloudsData

object WeatherInterpolator {

    fun interpolateWeatherList(rawList: List<WeatherItem>): List<WeatherItem> {
        if (rawList.isEmpty()) return rawList
        
        val hourlyItems = mutableListOf<WeatherItem>()
        
        for (i in 0 until rawList.size - 1) {
            val currentItem = rawList[i]
            val nextItem = rawList[i + 1]
            
            hourlyItems.add(currentItem)
            
            if (nextItem.dt - currentItem.dt == 10800L) {
                hourlyItems.add(interpolateWeatherItems(currentItem, nextItem, 1.0 / 3.0))
                hourlyItems.add(interpolateWeatherItems(currentItem, nextItem, 2.0 / 3.0))
            }
        }
        
        hourlyItems.add(rawList.last())
        return hourlyItems
    }

    private fun interpolateWeatherItems(
        start: WeatherItem,
        end: WeatherItem,
        fraction: Double
    ): WeatherItem {
        val dt = start.dt + ((end.dt - start.dt) * fraction).toLong()
        
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val dtTxt = dateFormat.format(java.util.Date(dt * 1000))

        val temp = start.main.temp + (end.main.temp - start.main.temp) * fraction
        val feelsLike = start.main.feelsLike + (end.main.feelsLike - start.main.feelsLike) * fraction
        val tempMin = start.main.tempMin + (end.main.tempMin - start.main.tempMin) * fraction
        val tempMax = start.main.tempMax + (end.main.tempMax - start.main.tempMax) * fraction
        val pressure = start.main.pressure + ((end.main.pressure - start.main.pressure) * fraction).toInt()
        val humidity = start.main.humidity + ((end.main.humidity - start.main.humidity) * fraction).toInt()
        val seaLevel = start.main.seaLevel + ((end.main.seaLevel - start.main.seaLevel) * fraction).toInt()
        val grndLevel = start.main.grndLevel + ((end.main.grndLevel - start.main.grndLevel) * fraction).toInt()
        
        val speed = start.wind.speed + (end.wind.speed - start.wind.speed) * fraction
        val deg = start.wind.deg + ((end.wind.deg - start.wind.deg) * fraction).toInt()
        val gust = start.wind.gust + (end.wind.gust - start.wind.gust) * fraction
        
        val clouds = start.clouds.all + ((end.clouds.all - start.clouds.all) * fraction).toInt()
        val visibility = start.visibility + ((end.visibility - start.visibility) * fraction).toInt()
        val pop = start.pop + (end.pop - start.pop) * fraction
        
        val weather = if (fraction > 0.5) end.weather else start.weather

        return WeatherItem(
            dt = dt,
            main = MainData(
                temp = temp,
                feelsLike = feelsLike,
                tempMin = tempMin,
                tempMax = tempMax,
                pressure = pressure,
                humidity = humidity,
                seaLevel = seaLevel,
                grndLevel = grndLevel
            ),
            weather = weather,
            clouds = CloudsData(all = clouds),
            wind = WindData(
                speed = speed,
                deg = deg,
                gust = gust
            ),
            visibility = visibility,
            pop = pop,
            dtTxt = dtTxt
        )
    }
}
