package aaa.plate_recognition_p.model;

import aaa.gate_log_p.model.GateLogDTO;
import aaa.vehicle_p.model.VehicleDTO;
import lombok.Data;

@Data
public class PlateRecognitionResultDTO {
    private FastApiPlateResponseDTO aiResult;
    private PlateRecognitionDTO plateRecognition;
    private GateLogDTO gateLog;
    private VehicleDTO vehicle;
    private Boolean matched;
    private Boolean needReview;
    private String message;
}
