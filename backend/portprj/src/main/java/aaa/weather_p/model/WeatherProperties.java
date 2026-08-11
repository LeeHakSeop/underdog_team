package aaa.weather_p.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "weather")
public class WeatherProperties {
    private Api api = new Api();
    private Risk risk = new Risk();

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Risk getRisk() {
        return risk;
    }

    public void setRisk(Risk risk) {
        this.risk = risk;
    }

    public static class Api {
        private String url;
        private String key;
        private double latitude;
        private double longitude;
        private long cacheTtlSeconds = 300;
        private int timeoutMs = 3000;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public long getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(long cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Risk {
        private Threshold wind = new Threshold();
        private Threshold rain = new Threshold();
        private Threshold visibility = new Threshold();

        public Threshold getWind() {
            return wind;
        }

        public void setWind(Threshold wind) {
            this.wind = wind;
        }

        public Threshold getRain() {
            return rain;
        }

        public void setRain(Threshold rain) {
            this.rain = rain;
        }

        public Threshold getVisibility() {
            return visibility;
        }

        public void setVisibility(Threshold visibility) {
            this.visibility = visibility;
        }
    }

    public static class Threshold {
        private double caution;
        private double danger;

        public double getCaution() {
            return caution;
        }

        public void setCaution(double caution) {
            this.caution = caution;
        }

        public double getDanger() {
            return danger;
        }

        public void setDanger(double danger) {
            this.danger = danger;
        }
    }
}
