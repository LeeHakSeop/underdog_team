package aaa.plate_recognition_p.model;

import lombok.Data;

import java.util.List;

@Data
public class FastApiPlateCandidateDTO {
    private String plateNumber;
    private String ocrRaw;
    private Double confidence;
    private Double detectionConfidence;
    private Double ocrConfidence;
    private Boolean needReview;
    private List<String> reviewReasons;
    private String cropPath;
    private String ocrType;
}
