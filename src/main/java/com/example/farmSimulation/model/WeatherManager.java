package com.example.farmSimulation.model;

import com.example.farmSimulation.config.WeatherConfig;
import lombok.Getter;

import java.util.Random;

/**
 * Quản lý hệ thống Thời tiết
 */
public class WeatherManager {
    @Getter
    private WeatherConfig.WeatherType currentWeather;
    private final Random random;
    private long lastWeatherUpdateTime;
    
    public WeatherManager() {
        this.random = new Random();
        this.currentWeather = WeatherConfig.WeatherType.SUNNY; // Mặc định nắng
        this.lastWeatherUpdateTime = System.nanoTime();
    }
    
    /**
     * Cập nhật thời tiết (gọi mỗi frame hoặc theo interval)
     * @param currentTime Thời gian hiện tại (nanoTime)
     */
    public void updateWeather(long currentTime) {
        long elapsedMs = (currentTime - lastWeatherUpdateTime) / 1_000_000;
        
        // Chỉ cập nhật thời tiết sau mỗi interval
        if (elapsedMs >= WeatherConfig.WEATHER_UPDATE_INTERVAL_MS) {
            changeWeather();
            lastWeatherUpdateTime = currentTime;
        }
    }
    
    /**
     * Thay đổi thời tiết ngẫu nhiên
     */
    private void changeWeather() {
        double chance = random.nextDouble();
        if (chance < WeatherConfig.RAIN_CHANCE) {
            this.currentWeather = WeatherConfig.WeatherType.RAIN;
        } else {
            this.currentWeather = WeatherConfig.WeatherType.SUNNY;
        }
    }
    
    /**
     * Kiểm tra xem có đang mưa không
     */
    public boolean isRaining() {
        return currentWeather == WeatherConfig.WeatherType.RAIN;
    }
    
    /**
     * Kiểm tra xem có nắng không
     */
    public boolean isSunny() {
        return currentWeather == WeatherConfig.WeatherType.SUNNY;
    }
    
    /**
     * Thay đổi thời tiết thủ công (dùng cho test hoặc debug)
     */
    public void setWeather(WeatherConfig.WeatherType weather) {
        this.currentWeather = weather;
    }
}