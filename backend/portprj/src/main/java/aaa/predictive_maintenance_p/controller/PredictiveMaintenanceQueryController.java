package aaa.predictive_maintenance_p.controller;

import aaa.predictive_maintenance_p.model.PredictiveEquipmentResponse;
import aaa.predictive_maintenance_p.model.PredictiveEventResponse;
import aaa.predictive_maintenance_p.model.PredictiveSensorDataResponse;
import aaa.predictive_maintenance_p.service.PredictiveMaintenanceQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/predictive-maintenance")
public class PredictiveMaintenanceQueryController {

    private final PredictiveMaintenanceQueryService queryService;

    public PredictiveMaintenanceQueryController(PredictiveMaintenanceQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/equipment")
    public List<PredictiveEquipmentResponse> equipment() {
        return queryService.equipment();
    }

    @GetMapping("/events")
    public List<PredictiveEventResponse> events(
            @RequestParam(required = false) String equipmentCode,
            @RequestParam(required = false) String eventType
    ) {
        return queryService.events(equipmentCode, eventType);
    }

    @GetMapping("/sensor-data")
    public List<PredictiveSensorDataResponse> sensorData(
            @RequestParam String equipmentCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return queryService.sensorData(equipmentCode, from, to);
    }
}
