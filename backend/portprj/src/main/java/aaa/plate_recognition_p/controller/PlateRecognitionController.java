package aaa.plate_recognition_p.controller;

import aaa.plate_recognition_p.model.ManualCorrectionRequestDTO;
import aaa.plate_recognition_p.model.PlateRecognitionDTO;
import aaa.plate_recognition_p.model.PlateRecognitionResultDTO;
import aaa.plate_recognition_p.service.PlateRecognitionService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/plate-recognition")
public class PlateRecognitionController {

    @Resource
    PlateRecognitionService plateRecognitionService;

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlateRecognitionResultDTO recognize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "ocrType", defaultValue = "paddle") String ocrType,
            @RequestParam(value = "plateType", defaultValue = "TRAILER") String plateType,
            @RequestParam(value = "gateNumber", defaultValue = "G01") String gateNumber,
            @RequestParam(value = "gateName", defaultValue = "AI_GATE") String gateName,
            @RequestParam(value = "inOutType", defaultValue = "IN") String inOutType
    ) throws IOException {
        return plateRecognitionService.recognize(file, ocrType, plateType, gateNumber, gateName, inOutType);
    }

    @PatchMapping("/{plateRecognitionId}/manual-correction")
    public PlateRecognitionDTO manualCorrection(
            @PathVariable Long plateRecognitionId,
            @RequestBody ManualCorrectionRequestDTO dto
    ) {
        return plateRecognitionService.updateManualCorrection(plateRecognitionId, dto);
    }
}
