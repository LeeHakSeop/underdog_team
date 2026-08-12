package aaa.weather_p.service;

import aaa.weather_p.client.WeatherClient;
import aaa.weather_p.client.WeatherClientException;
import aaa.weather_p.model.WeatherDTO;
import aaa.weather_p.model.WeatherProperties;
import aaa.weather_p.model.WeatherRiskLevel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class WeatherService {
    private static final String WEATHER_UNAVAILABLE_MESSAGE = "부산항 날씨 정보를 불러오지 못했습니다.";
    private static final String STALE_FALLBACK_MESSAGE = "실시간 기상 호출에 실패해 마지막 정상 데이터를 표시합니다.";
    private static final String SOURCE_LABEL = "부산항 날씨";

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
            WeatherDTO current = copy(cachedWeather);
            enrich(current, false);
            return current;
        }

        try {
            WeatherDTO weather = weatherClient.fetchCurrentWeather();
            enrich(weather, false);
            cachedWeather = copy(weather);
            cachedAt = LocalDateTime.now();
            return weather;
        } catch (WeatherClientException e) {
            if (cachedWeather != null && cachedWeather.isAvailable()) {
                WeatherDTO staleWeather = copy(cachedWeather);
                enrich(staleWeather, true);
                staleWeather.setFallbackUsed(true);
                staleWeather.setErrorCode(e.getCode());
                staleWeather.setErrorMessage(STALE_FALLBACK_MESSAGE);
                return staleWeather;
            }

            return unavailable(e.getCode(), e.getUserMessage());
        } catch (RuntimeException e) {
            if (cachedWeather != null && cachedWeather.isAvailable()) {
                WeatherDTO staleWeather = copy(cachedWeather);
                enrich(staleWeather, true);
                staleWeather.setFallbackUsed(true);
                staleWeather.setErrorCode("UNKNOWN_ERROR");
                staleWeather.setErrorMessage(STALE_FALLBACK_MESSAGE);
                return staleWeather;
            }

            return unavailable("UNKNOWN_ERROR", WEATHER_UNAVAILABLE_MESSAGE);
        }
    }

    private WeatherDTO unavailable(String errorCode, String errorMessage) {
        WeatherDTO dto = WeatherDTO.unavailable(errorCode, errorMessage);
        dto.setSourceLabel(SOURCE_LABEL);
        dto.setGuideMessage(weatherRiskService.guideMessage(WeatherRiskLevel.UNKNOWN, false));
        return dto;
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
        weather.setGuideMessage(weatherRiskService.guideMessage(riskLevel, stale));
        weather.setSourceLabel(SOURCE_LABEL);
        weather.setStale(stale);

        if (!stale) {
            weather.setFallbackUsed(false);
            weather.setErrorCode(null);
            weather.setErrorMessage(null);
        }

        if (weather.getForecasts() != null) {
            weather.getForecasts().forEach((forecast) -> {
                WeatherRiskLevel forecastRisk = weatherRiskService.evaluate(forecast);
                forecast.setRiskLevel(forecastRisk);
                forecast.setMessage(weatherRiskService.message(forecastRisk, false));
            });
        }
    }

    private WeatherDTO copy(WeatherDTO source) {
        WeatherDTO dto = new WeatherDTO();
        dto.setAvailable(source.isAvailable());
        dto.setStale(source.isStale());
        dto.setFallbackUsed(source.isFallbackUsed());
        dto.setTemperature(source.getTemperature());
        dto.setRainfall(source.getRainfall());
        dto.setWindSpeed(source.getWindSpeed());
        dto.setVisibility(source.getVisibility());
        dto.setHumidity(source.getHumidity());
        dto.setWeather(source.getWeather());
        dto.setDescription(source.getDescription());
        dto.setRiskLevel(source.getRiskLevel());
        dto.setMessage(source.getMessage());
        dto.setGuideMessage(source.getGuideMessage());
        dto.setSourceLabel(source.getSourceLabel());
        dto.setErrorCode(source.getErrorCode());
        dto.setErrorMessage(source.getErrorMessage());
        dto.setUpdatedAt(source.getUpdatedAt());
        dto.setForecasts(source.getForecasts() == null ? new ArrayList<>() : new ArrayList<>(source.getForecasts()));
        return dto;
    }
}
