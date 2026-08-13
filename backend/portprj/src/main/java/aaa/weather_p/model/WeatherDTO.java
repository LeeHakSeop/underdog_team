package aaa.weather_p.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WeatherDTO {
    private boolean available;
    private boolean stale;
    private boolean fallbackUsed;
    private Double temperature;
    private Double rainfall;
    private Double windSpeed;
    private Integer visibility;
    private Integer humidity;
    private String weather;
    private String description;
    private WeatherRiskLevel riskLevel;
    private String message;
    private String guideMessage;
    private String sourceLabel;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime updatedAt;
    private List<WeatherForecastDTO> forecasts = new ArrayList<>();

    public static WeatherDTO unavailable(String message) {
        return unavailable("WEATHER_UNAVAILABLE", message);
    }

    public static WeatherDTO unavailable(String errorCode, String message) {
        WeatherDTO dto = new WeatherDTO();
        dto.setAvailable(false);
        dto.setStale(false);
        dto.setFallbackUsed(false);
        dto.setRiskLevel(WeatherRiskLevel.UNKNOWN);
        dto.setMessage(message);
        dto.setGuideMessage("현장 공지와 관리자 안내를 먼저 확인하세요.");
        dto.setSourceLabel("부산항 날씨");
        dto.setErrorCode(errorCode);
        dto.setErrorMessage(message);
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

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
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

    public String getGuideMessage() {
        return guideMessage;
    }

    public void setGuideMessage(String guideMessage) {
        this.guideMessage = guideMessage;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<WeatherForecastDTO> getForecasts() {
        return forecasts;
    }

    public void setForecasts(List<WeatherForecastDTO> forecasts) {
        this.forecasts = forecasts;
    }
}
