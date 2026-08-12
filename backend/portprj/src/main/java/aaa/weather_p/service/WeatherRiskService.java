package aaa.weather_p.service;

import aaa.weather_p.model.WeatherDTO;
import aaa.weather_p.model.WeatherForecastDTO;
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

    public WeatherRiskLevel evaluate(WeatherForecastDTO forecast) {
        if (forecast == null) {
            return WeatherRiskLevel.UNKNOWN;
        }

        WeatherProperties.Risk risk = properties.getRisk();

        if (isGreaterOrEqual(forecast.getWindSpeed(), risk.getWind().getDanger())
                || isGreaterOrEqual(forecast.getRainfall(), risk.getRain().getDanger())
                || isLessOrEqual(forecast.getVisibility(), risk.getVisibility().getDanger())) {
            return WeatherRiskLevel.DANGER;
        }

        if (isGreaterOrEqual(forecast.getWindSpeed(), risk.getWind().getCaution())
                || isGreaterOrEqual(forecast.getRainfall(), risk.getRain().getCaution())
                || isLessOrEqual(forecast.getVisibility(), risk.getVisibility().getCaution())) {
            return WeatherRiskLevel.CAUTION;
        }

        return WeatherRiskLevel.NORMAL;
    }

    public String message(WeatherRiskLevel riskLevel, boolean stale) {
        String suffix = stale ? " 마지막 정상 조회 데이터를 표시합니다." : "";

        return switch (riskLevel) {
            case NORMAL -> "현재 부산항 작업 기상은 정상 범위입니다." + suffix;
            case CAUTION -> "부산항 기상 주의 상태입니다. 현장 조건을 먼저 확인하세요." + suffix;
            case DANGER -> "부산항 기상 위험 상태입니다. 작업 전 운영 판단이 필요합니다." + suffix;
            case UNKNOWN -> "부산항 날씨 정보를 불러오지 못했습니다.";
        };
    }

    public String guideMessage(WeatherRiskLevel riskLevel, boolean stale) {
        String suffix = stale ? " 실시간 호출에 실패해 마지막 정상 데이터를 기준으로 안내합니다." : "";

        return switch (riskLevel) {
            case NORMAL -> "일반 작업 진행이 가능합니다." + suffix;
            case CAUTION -> "강풍, 강수, 저시정 여부를 확인하고 여유 시간을 두고 이동하세요." + suffix;
            case DANGER -> "배차, 입차, 작업 진행 전 관리자 또는 관제 확인이 필요합니다." + suffix;
            case UNKNOWN -> "현장 공지와 관리자 안내를 먼저 확인하세요.";
        };
    }

    private boolean isGreaterOrEqual(Double value, double threshold) {
        return value != null && value >= threshold;
    }

    private boolean isLessOrEqual(Integer value, double threshold) {
        return value != null && value <= threshold;
    }
}
