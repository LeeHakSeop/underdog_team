package aaa.plate_recognition_p.service;

import aaa.gate_log_p.model.GateLogDTO;
import aaa.plate_recognition_p.model.FastApiPlateResponseDTO;
import aaa.plate_recognition_p.model.PlateRecognitionDTO;
import aaa.plate_recognition_p.model.PlateRecognitionMapper;
import aaa.plate_recognition_p.model.PlateRecognitionResultDTO;
import aaa.vehicle_p.model.TractorVehicleInfoDTO;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import aaa.work_order_p.model.TrailerWorkInfoDTO;
import aaa.work_order_p.service.WorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PlateRecognitionService {

    @Value("${ai.plate.url:http://127.0.0.1:8000/api/plate-recognition}")
    String fastApiUrl;

    @Resource
    VehicleMapper vehicleMapper;

    @Resource
    PlateRecognitionMapper plateRecognitionMapper;

<<<<<<< HEAD
    @Resource
    WorkOrderService workOrderService;

    public PlateRecognitionResultDTO recognize(MultipartFile file) throws IOException {
=======
    public PlateRecognitionResultDTO recognize(MultipartFile file, String ocrType, String plateType) throws IOException {
>>>>>>> origin/main
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

        FastApiPlateResponseDTO aiResult;

        try {
            ResponseEntity<FastApiPlateResponseDTO> response = restTemplate.postForEntity(
                    fastApiUrl,
                    requestEntity,
                    FastApiPlateResponseDTO.class
            );
            aiResult = response.getBody();
        } catch (ResourceAccessException e) {
            aiResult = null;
        }

        VehicleDTO vehicle = null;
        TractorVehicleInfoDTO tractorVehicleInfo = null;
        TrailerWorkInfoDTO trailerWorkInfo = null;

        if (aiResult != null && aiResult.getPlateNumber() != null) {
            vehicle = vehicleMapper.findByPlateNumber(aiResult.getPlateNumber());
        }

        boolean isTractor = vehicle != null && "TRACTOR".equalsIgnoreCase(vehicle.getVehicleType());
        boolean isTrailer = vehicle != null && "TRAILER".equalsIgnoreCase(vehicle.getVehicleType());

        if (isTractor) {
            tractorVehicleInfo = vehicleMapper.findTractorInfoByPlateNumber(vehicle.getPlateNumber());
        }

        if (isTrailer) {
            trailerWorkInfo = workOrderService.findTrailerWorkInfo(vehicle.getVehicleId());
        }

        printRecognitionCheckLog(aiResult, vehicle, tractorVehicleInfo, trailerWorkInfo);

        boolean matched = vehicle != null;
        boolean hasTractorInfo = tractorVehicleInfo != null;
        boolean hasTrailerWorkOrder = trailerWorkInfo != null;
        boolean hasRequiredInfo = (isTractor && hasTractorInfo) || (isTrailer && hasTrailerWorkOrder);
        boolean needReview = !matched || !hasRequiredInfo;

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
<<<<<<< HEAD
            plateRecognition.setIsSuccess(Boolean.TRUE.equals(aiResult.getDetected()) && matched && hasRequiredInfo);
=======
            plateRecognition.setPlateType(plateType);
            plateRecognition.setIsSuccess(Boolean.TRUE.equals(aiResult.getDetected()) && matched);
>>>>>>> origin/main
            plateRecognition.setConfidence(BigDecimal.valueOf(aiResult.getConfidence() == null ? 0.0 : aiResult.getConfidence()));
            plateRecognition.setErrorMessage(makeErrorMessage(aiResult, matched, isTractor, isTrailer, hasRequiredInfo));
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
        result.setTractorVehicleInfo(tractorVehicleInfo);
        result.setTrailerWorkInfo(trailerWorkInfo);
        result.setMatched(matched);
        result.setNeedReview(needReview);
        result.setMessage(makeMessage(
                aiResult,
                matched,
                isTractor,
                isTrailer,
                hasRequiredInfo,
                needReview,
                tractorVehicleInfo,
                trailerWorkInfo
        ));

        return result;
    }

    private String makeErrorMessage(
            FastApiPlateResponseDTO aiResult,
            boolean matched,
            boolean isTractor,
            boolean isTrailer,
            boolean hasRequiredInfo
    ) {
        String message = "";

        if (aiResult.getReviewReasons() != null) {
            for (String reason : aiResult.getReviewReasons()) {
                message += reason + ",";
            }
        }

        if (!matched) {
            message += "VEHICLE_NOT_REGISTERED,";
        }

        if (matched && isTractor && !hasRequiredInfo) {
            message += "TRACTOR_INFO_NOT_FOUND,";
        }

        if (matched && isTrailer && !hasRequiredInfo) {
            message += "WORK_ORDER_NOT_FOUND,";
        }

        if (message.endsWith(",")) {
            message = message.substring(0, message.length() - 1);
        }

        return message.isBlank() ? null : message;
    }

<<<<<<< HEAD
    private void printRecognitionCheckLog(
            FastApiPlateResponseDTO aiResult,
            VehicleDTO vehicle,
            TractorVehicleInfoDTO tractorVehicleInfo,
            TrailerWorkInfoDTO trailerWorkInfo
    ) {
        System.out.println("========== 번호판 인식 DB 조회 결과 ==========");

        if (aiResult == null) {
            System.out.println("AI 서버 응답: 없음");
            System.out.println("==========================================");
            return;
        }

        System.out.println("인식 번호판: " + aiResult.getPlateNumber());

        if (vehicle == null) {
            System.out.println("등록차량: 아니오");
            System.out.println("차량 조회 결과: 없음");
            System.out.println("==========================================");
            return;
        }

        System.out.println("차량구분: " + vehicle.getVehicleType());

        if ("TRACTOR".equalsIgnoreCase(vehicle.getVehicleType())) {
            printTractorLog(tractorVehicleInfo);
        } else if ("TRAILER".equalsIgnoreCase(vehicle.getVehicleType())) {
            printTrailerLog(trailerWorkInfo);
        } else {
            System.out.println("차량 번호: " + vehicle.getPlateNumber());
            System.out.println("등록차량: " + (Boolean.TRUE.equals(vehicle.getIsRegistered()) ? "예" : "아니오"));
            System.out.println("차량상태: " + vehicle.getVehicleStatus());
        }

        System.out.println("==========================================");
    }

    private void printTractorLog(TractorVehicleInfoDTO tractorVehicleInfo) {
        if (tractorVehicleInfo == null) {
            System.out.println("트랙터 조회 결과: 없음");
            return;
        }

        System.out.println("번호판: " + tractorVehicleInfo.getPlateNumber());
        System.out.println("등록차량: " + tractorVehicleInfo.getRegisteredText());
        System.out.println("운송사: " + valueOrDash(tractorVehicleInfo.getCarrierName()));
        System.out.println("기사명: " + valueOrDash(tractorVehicleInfo.getDriverName()));
        System.out.println("차량상태: " + valueOrDash(tractorVehicleInfo.getVehicleStatus()));
        System.out.println("트랙터번호: " + valueOrDash(tractorVehicleInfo.getTractorNo()));
    }

    private void printTrailerLog(TrailerWorkInfoDTO trailerWorkInfo) {
        if (trailerWorkInfo == null) {
            System.out.println("트레일러 작업 조회 결과: 없음");
            return;
        }

        System.out.println("트레일러 작업오더: " + trailerWorkInfo.getWorkOrderId());
        System.out.println("컨테이너 번호: " + valueOrDash(trailerWorkInfo.getContainerNumber()));
        System.out.println("해야할 작업: " + valueOrDash(trailerWorkInfo.getWorkType()));
        System.out.println("컨테이너 위치: " + valueOrDash(trailerWorkInfo.getContainerLocation()));
        System.out.println("야드 위치: " + valueOrDash(trailerWorkInfo.getYardLocation()));
        System.out.println("야드 섹터: " + valueOrDash(trailerWorkInfo.getSectorName()));
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String makeMessage(
            FastApiPlateResponseDTO aiResult,
            boolean matched,
            boolean isTractor,
            boolean isTrailer,
            boolean hasRequiredInfo,
            boolean needReview,
            TractorVehicleInfoDTO tractorVehicleInfo,
            TrailerWorkInfoDTO trailerWorkInfo
    ) {
=======
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
>>>>>>> origin/main
        if (aiResult == null) {
            return "FastAPI 응답이 없습니다.";
        }

        if (!Boolean.TRUE.equals(aiResult.getDetected())) {
            return "번호판을 인식하지 못했습니다.";
        }

        if (!matched) {
            return "번호판은 인식했지만 등록 차량이 아닙니다.";
        }

        if (isTractor && !hasRequiredInfo) {
            return "등록 트랙터 차량이지만 상세 조회 정보가 없습니다.";
        }

        if (isTrailer && !hasRequiredInfo) {
            return "등록 트레일러 차량이지만 배정된 작업오더가 없습니다.";
        }

        if (!isTractor && !isTrailer) {
            return "등록 차량이지만 트랙터/트레일러 구분이 필요합니다.";
        }

        if (needReview) {
            return "번호판 인식 결과 검토가 필요합니다.";
        }

        if (tractorVehicleInfo != null) {
            return "트랙터 차량 조회가 완료되었습니다.";
        }

        if (trailerWorkInfo.getWorkGuideMessage() != null) {
            return trailerWorkInfo.getWorkGuideMessage();
        }

        return "번호판 인식 및 작업오더 조회가 완료되었습니다.";
    }
}
