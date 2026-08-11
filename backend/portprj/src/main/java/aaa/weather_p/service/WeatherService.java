package aaa.weather_p.service;

import aaa.weather_p.client.WeatherClient;
import aaa.weather_p.model.WeatherDTO;
import aaa.weather_p.model.WeatherProperties;
import aaa.weather_p.model.WeatherRiskLevel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class WeatherService {
    private static final String WEATHER_UNAVAILABLE_MESSAGE = "날씨 정보를 불러올 수 없습니다.";

    private final WeatherClient weatherClient;
    private final WeatherRiskService weatherRiskService;
    private final WeatherProperties properties;
    private WeatherDTO cachedWeather;
    private LocalDateTime cachedAt;

    public WeatherService(
            WeatherClient weatherClient,
            WeatherRiskService weatherRiskService,
            WeatherProperties properties
    ) {
        this.weatherClient = weatherClient;
        this.weatherRiskService = weatherRiskService;
        this.properties = properties;
    }

    public synchronized WeatherDTO getCurrentWeather() {
        if (isCacheValid()) {
            return cachedWeather;
        }

        try {
            WeatherDTO weather = weatherClient.fetchCurrentWeather();
            enrich(weather, false);
            cachedWeather = weather;
            cachedAt = LocalDateTime.now();
            return weather;
        } catch (RuntimeException e) {
            if (cachedWeather != null && cachedWeather.isAvailable()) {
                WeatherDTO staleWeather = copy(cachedWeather);
                enrich(staleWeather, true);
                return staleWeather;
            }

            return WeatherDTO.unavailable(WEATHER_UNAVAILABLE_MESSAGE);
        }
    }

    private boolean isCacheValid() {
        if (cachedWeather == null || cachedAt == null) {
            return false;
        }

        long ttlSeconds = Math.max(1, properties.getApi().getCacheTtlSeconds());
        return Duration.between(cachedAt, LocalDateTime.now()).getSeconds() < ttlSeconds;
    }

    private void enrich(WeatherDTO weather, boolean stale) {
        WeatherRiskLevel riskLevel = weatherRiskService.evaluate(weather);
        weather.setRiskLevel(riskLevel);
        weather.setMessage(weatherRiskService.message(riskLevel, stale));
        weather.setStale(stale);
    }

    private WeatherDTO copy(WeatherDTO source) {
        WeatherDTO dto = new WeatherDTO();
        dto.setAvailable(source.isAvailable());
        dto.setTemperature(source.getTemperature());
        dto.setRainfall(source.getRainfall());
        dto.setWindSpeed(source.getWindSpeed());
        dto.setVisibility(source.getVisibility());
        dto.setHumidity(source.getHumidity());
        dto.setWeather(source.getWeather());
        dto.setDescription(source.getDescription());
        dto.setRiskLevel(source.getRiskLevel());
        dto.setMessage(source.getMessage());
        dto.setUpdatedAt(source.getUpdatedAt());
        return dto;
    }
}
