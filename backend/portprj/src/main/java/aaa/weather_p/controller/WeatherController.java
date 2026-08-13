package aaa.weather_p.controller;

import aaa.weather_p.model.WeatherDTO;
import aaa.weather_p.service.WeatherService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    @Resource
    WeatherService service;

    @GetMapping("/current")
    public WeatherDTO current() {
        return service.getCurrentWeather();
    }
}
