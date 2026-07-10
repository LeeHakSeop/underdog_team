package aaa.plate_recognition_p.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PlateRecognitionDTO {
    private Long plateRecognitionId;
    private Long gateLogId;
    private String vehicleImage;
    private String recognizedPlate;
    private String plateType;
    private Boolean isSuccess;
    private BigDecimal confidence;
    private String manualCorrection;
    private String errorMessage;
    private LocalDateTime recognitionTime;
}
