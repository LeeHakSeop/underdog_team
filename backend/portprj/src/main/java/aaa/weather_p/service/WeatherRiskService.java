package aaa.weather_p.service;

import aaa.weather_p.model.WeatherDTO;
import aaa.weather_p.model.WeatherProperties;
import aaa.weather_p.model.WeatherRiskLevel;
import org.springframework.stereotype.Service;

@Service
public class WeatherRiskService {
    private final WeatherProperties properties;

    public WeatherRiskService(WeatherProperties properties) {
        this.properties = properties;
    }

    public WeatherRiskLevel evaluate(WeatherDTO weather) {
        if (weather == null || !weather.isAvailable()) {
            return WeatherRiskLevel.UNKNOWN;
        }

        WeatherProperties.Risk risk = properties.getRisk();

        if (isGreaterOrEqual(weather.getWindSpeed(), risk.getWind().getDanger())
                || isGreaterOrEqual(weather.getRainfall(), risk.getRain().getDanger())
                || isLessOrEqual(weather.getVisibility(), risk.getVisibility().getDanger())) {
            return WeatherRiskLevel.DANGER;
        }

        if (isGreaterOrEqual(weather.getWindSpeed(), risk.getWind().getCaution())
                || isGreaterOrEqual(weather.getRainfall(), risk.getRain().getCaution())
                || isLessOrEqual(weather.getVisibility(), risk.getVisibility().getCaution())) {
            return WeatherRiskLevel.CAUTION;
        }

        return WeatherRiskLevel.NORMAL;
    }

    public String message(WeatherRiskLevel riskLevel, boolean stale) {
        String suffix = stale ? " 마지막 정상 조회 데이터를 표시합니다." : "";

        return switch (riskLevel) {
            case NORMAL -> "현재 작업 환경은 정상입니다." + suffix;
            case CAUTION -> "기상 조건 확인이 필요합니다. 작업 전 현장 상태를 확인하세요." + suffix;
            case DANGER -> "위험 기상 조건입니다. 작업 진행 여부를 관리자에게 확인하세요." + suffix;
            case UNKNOWN -> "날씨 정보를 불러올 수 없습니다.";
        };
    }

    private boolean isGreaterOrEqual(Double value, double threshold) {
        return value != null && value >= threshold;
    }

    private boolean isLessOrEqual(Integer value, double threshold) {
        return value != null && value <= threshold;
    }
}
