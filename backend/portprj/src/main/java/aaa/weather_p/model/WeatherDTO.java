package aaa.weather_p.model;

import java.time.LocalDateTime;

public class WeatherDTO {
    private boolean available;
    private boolean stale;
    private Double temperature;
    private Double rainfall;
    private Double windSpeed;
    private Integer visibility;
    private Integer humidity;
    private String weather;
    private String description;
    private WeatherRiskLevel riskLevel;
    private String message;
    private LocalDateTime updatedAt;

    public static WeatherDTO unavailable(String message) {
        WeatherDTO dto = new WeatherDTO();
        dto.setAvailable(false);
        dto.setStale(false);
        dto.setRiskLevel(WeatherRiskLevel.UNKNOWN);
        dto.setMessage(message);
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getRainfall() {
        return rainfall;
    }

    public void setRainfall(Double rainfall) {
        this.rainfall = rainfall;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Integer getVisibility() {
        return visibility;
    }

    public void setVisibility(Integer visibility) {
        this.visibility = visibility;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WeatherRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(WeatherRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
