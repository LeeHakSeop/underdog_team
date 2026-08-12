package aaa.weather_p.model;

import java.time.LocalDateTime;

public class WeatherForecastDTO {
    private LocalDateTime forecastAt;
    private Double temperature;
    private Double rainfall;
    private Double windSpeed;
    private Integer visibility;
    private Integer humidity;
    private String weather;
    private String description;
    private WeatherRiskLevel riskLevel;
    private String message;

    public LocalDateTime getForecastAt() {
        return forecastAt;
    }

    public void setForecastAt(LocalDateTime forecastAt) {
        this.forecastAt = forecastAt;
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
}
