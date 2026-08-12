package aaa.weather_p.client;

import aaa.weather_p.model.WeatherDTO;
import aaa.weather_p.model.WeatherForecastDTO;
import aaa.weather_p.model.WeatherProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class WeatherClient {
    private final WeatherProperties properties;

    public WeatherClient(WeatherProperties properties) {
        this.properties = properties;
    }

    public WeatherDTO fetchCurrentWeather() {
        WeatherProperties.Api api = properties.getApi();
        String apiKey = resolveApiKey(api);

        if (!StringUtils.hasText(apiKey)) {
            throw new WeatherClientException("API_KEY_MISSING", "부산항 날씨 API 키가 설정되지 않았습니다.");
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(api.getUrl())
                .queryParam("lat", api.getLatitude())
                .queryParam("lon", api.getLongitude())
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .queryParam("lang", "kr")
                .build()
                .toUri();

        try {
            Map<?, ?> response = restTemplate(api.getTimeoutMs()).getForObject(uri, Map.class);
            WeatherDTO weather = parse(response);
            weather.setForecasts(fetchForecasts(api, apiKey));
            return weather;
        } catch (ResourceAccessException e) {
            throw new WeatherClientException("API_TIMEOUT", "부산항 날씨 정보를 제시간에 가져오지 못했습니다.", e);
        } catch (RestClientException e) {
            throw new WeatherClientException("API_REQUEST_FAILED", "부산항 날씨 API 호출에 실패했습니다.", e);
        }
    }

    private List<WeatherForecastDTO> fetchForecasts(WeatherProperties.Api api, String apiKey) {
        if (!StringUtils.hasText(api.getForecastUrl())) {
            return List.of();
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(api.getForecastUrl())
                .queryParam("lat", api.getLatitude())
                .queryParam("lon", api.getLongitude())
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .queryParam("lang", "kr")
                .build()
                .toUri();

        try {
            Map<?, ?> response = restTemplate(api.getTimeoutMs()).getForObject(uri, Map.class);
            List<?> list = asList(response == null ? null : response.get("list"));
            int timezoneOffsetSeconds = asInteger(asMap(response == null ? null : response.get("city")).get("timezone"), 0);
            return list.stream()
                    .limit(5)
                    .map((item) -> parseForecast(asMap(item), timezoneOffsetSeconds))
                    .toList();
        } catch (RestClientException e) {
            return List.of();
        }
    }

    private String resolveApiKey(WeatherProperties.Api api) {
        if (StringUtils.hasText(api.getKey())) {
            return api.getKey().trim();
        }

        if (!StringUtils.hasText(api.getKeyFile())) {
            return "";
        }

        try {
            Path keyPath = Path.of(api.getKeyFile());
            if (!Files.exists(keyPath)) {
                return "";
            }
            return Files.readString(keyPath).trim();
        } catch (IOException e) {
            throw new WeatherClientException("API_KEY_FILE_ERROR", "날씨 API 키 파일을 읽을 수 없습니다.", e);
        }
    }

    private RestTemplate restTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    private WeatherDTO parse(Map<?, ?> response) {
        if (response == null) {
            throw new WeatherClientException("EMPTY_RESPONSE", "부산항 날씨 응답이 비어 있습니다.");
        }

        Map<?, ?> main = asMap(response.get("main"));
        Map<?, ?> wind = asMap(response.get("wind"));
        Map<?, ?> rain = asMap(response.get("rain"));
        List<?> weatherList = asList(response.get("weather"));
        Map<?, ?> weather = weatherList.isEmpty() ? Map.of() : asMap(weatherList.get(0));

        WeatherDTO dto = new WeatherDTO();
        dto.setAvailable(true);
        dto.setStale(false);
        dto.setTemperature(asDouble(main.get("temp")));
        dto.setRainfall(asDoubleOrDefault(rain.get("1h"), 0.0));
        dto.setWindSpeed(asDouble(wind.get("speed")));
        dto.setVisibility(asInteger(response.get("visibility")));
        dto.setHumidity(asInteger(main.get("humidity")));
        dto.setWeather(asString(weather.get("main")));
        dto.setDescription(asString(weather.get("description")));
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    private WeatherForecastDTO parseForecast(Map<?, ?> item, int timezoneOffsetSeconds) {
        Map<?, ?> main = asMap(item.get("main"));
        Map<?, ?> wind = asMap(item.get("wind"));
        Map<?, ?> rain = asMap(item.get("rain"));
        List<?> weatherList = asList(item.get("weather"));
        Map<?, ?> weather = weatherList.isEmpty() ? Map.of() : asMap(weatherList.get(0));

        WeatherForecastDTO dto = new WeatherForecastDTO();
        dto.setForecastAt(parseDateTime(asString(item.get("dt_txt")), timezoneOffsetSeconds));
        dto.setTemperature(asDouble(main.get("temp")));
        dto.setRainfall(asDoubleOrDefault(rain.get("3h"), 0.0));
        dto.setWindSpeed(asDouble(wind.get("speed")));
        dto.setVisibility(asInteger(item.get("visibility")));
        dto.setHumidity(asInteger(main.get("humidity")));
        dto.setWeather(asString(weather.get("main")));
        dto.setDescription(asString(weather.get("description")));
        return dto;
    }

    private LocalDateTime parseDateTime(String value, int timezoneOffsetSeconds) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .plusSeconds(timezoneOffsetSeconds);
    }

    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private Double asDoubleOrDefault(Object value, Double defaultValue) {
        Double parsed = asDouble(value);
        return parsed == null ? defaultValue : parsed;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private int asInteger(Object value, int defaultValue) {
        Integer parsed = asInteger(value);
        return parsed == null ? defaultValue : parsed;
    }
}
