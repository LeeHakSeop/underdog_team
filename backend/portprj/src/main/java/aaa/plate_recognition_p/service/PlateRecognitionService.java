package aaa.plate_recognition_p.service;

import aaa.gate_log_p.model.GateLogDTO;
import aaa.plate_recognition_p.model.FastApiPlateResponseDTO;
import aaa.plate_recognition_p.model.PlateRecognitionDTO;
import aaa.plate_recognition_p.model.PlateRecognitionMapper;
import aaa.plate_recognition_p.model.PlateRecognitionResultDTO;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import jakarta.annotation.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PlateRecognitionService {

    private static final String FAST_API_URL = "http://localhost:8000/api/plate-recognition";

    @Resource
    VehicleMapper vehicleMapper;

    @Resource
    PlateRecognitionMapper plateRecognitionMapper;

    public PlateRecognitionResultDTO recognize(MultipartFile file, String ocrType, String plateType) throws IOException {
        RestTemplate restTemplate = new RestTemplate();

        ByteArrayResource imageResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", imageResource);
        body.add("ocrType", ocrType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<FastApiPlateResponseDTO> response = restTemplate.postForEntity(
                FAST_API_URL,
                requestEntity,
                FastApiPlateResponseDTO.class
        );

        FastApiPlateResponseDTO aiResult = response.getBody();

        VehicleDTO vehicle = null;

        if (aiResult != null && aiResult.getPlateNumber() != null) {
            vehicle = vehicleMapper.findByPlateNumber(aiResult.getPlateNumber());
        }

        boolean matched = vehicle != null;
        boolean needReview = !matched;

        if (aiResult == null || Boolean.TRUE.equals(aiResult.getNeedReview())) {
            needReview = true;
        }

        GateLogDTO gateLog = new GateLogDTO();
        gateLog.setVehicleId(vehicle == null ? null : vehicle.getVehicleId());
        gateLog.setTractorVehicleId(getPlateVehicleId(vehicle, plateType, "TRACTOR"));
        gateLog.setTrailerVehicleId(getPlateVehicleId(vehicle, plateType, "TRAILER"));
        gateLog.setGateNumber("G01");
        gateLog.setGateName("AI_GATE");
        gateLog.setEntryTime(LocalDateTime.now());
        gateLog.setInOutType("IN");
        gateLog.setProcessResult(needReview ? "NEED_REVIEW" : "SUCCESS");
        gateLog.setManagerCheck(!needReview);
        plateRecognitionMapper.insertGateLog(gateLog);

        PlateRecognitionDTO plateRecognition = new PlateRecognitionDTO();
        plateRecognition.setGateLogId(gateLog.getGateLogId());
        plateRecognition.setRecognitionTime(LocalDateTime.now());

        if (aiResult != null) {
            plateRecognition.setVehicleImage(aiResult.getCropPath());
            plateRecognition.setRecognizedPlate(aiResult.getPlateNumber());
            plateRecognition.setPlateType(plateType);
            plateRecognition.setIsSuccess(Boolean.TRUE.equals(aiResult.getDetected()) && matched);
            plateRecognition.setConfidence(BigDecimal.valueOf(aiResult.getConfidence() == null ? 0.0 : aiResult.getConfidence()));
            plateRecognition.setErrorMessage(makeErrorMessage(aiResult, matched));
        } else {
            plateRecognition.setIsSuccess(false);
            plateRecognition.setPlateType(plateType);
            plateRecognition.setConfidence(BigDecimal.valueOf(0.0));
            plateRecognition.setErrorMessage("FAST_API_ERROR");
        }

        plateRecognitionMapper.insertPlateRecognition(plateRecognition);

        PlateRecognitionResultDTO result = new PlateRecognitionResultDTO();
        result.setAiResult(aiResult);
        result.setGateLog(gateLog);
        result.setPlateRecognition(plateRecognition);
        result.setVehicle(vehicle);
        result.setMatched(matched);
        result.setNeedReview(needReview);
        result.setMessage(makeMessage(aiResult, matched, needReview));

        return result;
    }

    private String makeErrorMessage(FastApiPlateResponseDTO aiResult, boolean matched) {
        String message = "";

        if (aiResult.getReviewReasons() != null) {
            for (String reason : aiResult.getReviewReasons()) {
                message += reason + ",";
            }
        }

        if (!matched) {
            message += "VEHICLE_NOT_REGISTERED";
        }

        if (message.endsWith(",")) {
            message = message.substring(0, message.length() - 1);
        }

        return message.isBlank() ? null : message;
    }

    private Long getPlateVehicleId(VehicleDTO vehicle, String plateType, String targetType) {
        if (vehicle == null) {
            return null;
        }

        if (plateType == null) {
            return null;
        }

        if (plateType.equalsIgnoreCase(targetType)) {
            return vehicle.getVehicleId();
        }

        return null;
    }

    private String makeMessage(FastApiPlateResponseDTO aiResult, boolean matched, boolean needReview) {
        if (aiResult == null) {
            return "FastAPI 응답이 없습니다.";
        }

        if (!Boolean.TRUE.equals(aiResult.getDetected())) {
            return "번호판을 인식하지 못했습니다.";
        }

        if (!matched) {
            return "번호판은 인식했지만 등록 차량이 아닙니다.";
        }

        if (needReview) {
            return "번호판 인식 결과에 검토가 필요합니다.";
        }

        return "번호판 인식 및 차량 매칭이 완료되었습니다.";
    }
}
