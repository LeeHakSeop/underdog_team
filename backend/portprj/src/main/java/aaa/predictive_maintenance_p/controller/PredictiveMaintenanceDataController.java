package aaa.predictive_maintenance_p.controller;

import aaa.predictive_maintenance_p.model.PredictiveDataSummary;
import aaa.predictive_maintenance_p.model.PredictiveImportResult;
import aaa.predictive_maintenance_p.service.PredictiveMaintenanceImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/predictive-maintenance/sensor-data")
public class PredictiveMaintenanceDataController {

    private final PredictiveMaintenanceImportService importService;

    public PredictiveMaintenanceDataController(PredictiveMaintenanceImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PredictiveImportResult importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return importService.importCsv(file);
    }

    @GetMapping("/summary")
    public PredictiveDataSummary summary() {
        return importService.summary();
    }
}
