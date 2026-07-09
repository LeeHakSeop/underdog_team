package aaa.plate_recognition_p.model;

import lombok.Data;

import java.util.List;

@Data
public class FastApiPlateResponseDTO {
    private Boolean detected;
    private String plateNumber;
    private String ocrRaw;
    private Double confidence;
    private Double detectionConfidence;
    private Double ocrConfidence;
    private Boolean needReview;
    private List<String> reviewReasons;
    private String cropPath;
    private List<FastApiPlateCandidateDTO> candidates;
    private String ocrType;
}
