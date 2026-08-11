package aaa.weather_p.client;

import aaa.weather_p.model.WeatherDTO;
import aaa.weather_p.model.WeatherProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
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

        if (!StringUtils.hasText(api.getKey())) {
            throw new IllegalStateException("WEATHER_API_KEY is not configured.");
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(api.getUrl())
                .queryParam("lat", api.getLatitude())
                .queryParam("lon", api.getLongitude())
                .queryParam("appid", api.getKey())
                .queryParam("units", "metric")
                .queryParam("lang", "kr")
                .build()
                .toUri();

        try {
            Map<?, ?> response = restTemplate(api.getTimeoutMs()).getForObject(uri, Map.class);
            return parse(response);
        } catch (RestClientException e) {
            throw new IllegalStateException("Weather API request failed.", e);
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
            throw new IllegalStateException("Weather API response is empty.");
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
}
