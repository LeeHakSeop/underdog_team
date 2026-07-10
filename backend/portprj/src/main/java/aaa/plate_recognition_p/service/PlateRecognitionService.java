package aaa.plate_recognition_p.service;

import aaa.exception_log_p.model.ExceptionLogDTO;
import aaa.exception_log_p.service.ExceptionLogService;
import aaa.gate_log_p.model.GateLogDTO;
import aaa.plate_recognition_p.model.FastApiPlateResponseDTO;
import aaa.plate_recognition_p.model.ManualCorrectionRequestDTO;
import aaa.plate_recognition_p.model.PlateRecognitionDTO;
import aaa.plate_recognition_p.model.PlateRecognitionMapper;
import aaa.plate_recognition_p.model.PlateRecognitionResultDTO;
import aaa.vehicle_p.model.TractorVehicleInfoDTO;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import aaa.work_order_p.model.TrailerWorkInfoDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.service.WorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PlateRecognitionService {

    private static final String RECOGNITION_SUCCESS = "RECOGNITION_SUCCESS";
    private static final String RECOGNITION_NEED_REVIEW = "RECOGNITION_NEED_REVIEW";

    @Value("${ai.plate.url:http://127.0.0.1:8000/api/plate-recognition}")
    String fastApiUrl;

    @Resource
    VehicleMapper vehicleMapper;

    @Resource
    PlateRecognitionMapper plateRecognitionMapper;

    @Resource
    WorkOrderService workOrderService;

    @Resource
    ExceptionLogService exceptionLogService;

    public PlateRecognitionResultDTO recognize(MultipartFile file) throws IOException {
        return recognize(file, "paddle", "TRAILER");
    }

    public PlateRecognitionResultDTO recognize(MultipartFile file, String ocrType, String plateType) throws IOException {
        return recognize(file, ocrType, plateType, "G01", "AI_GATE", "IN");
    }

    @Transactional
    public PlateRecognitionDTO updateManualCorrection(Long plateRecognitionId, ManualCorrectionRequestDTO dto) {
        if (plateRecognitionId == null) {
            throw new IllegalArgumentException("번호판 인식 이력이 없습니다.");
        }

        if (dto == null || isBlank(dto.getManualCorrection())) {
            throw new IllegalArgumentException("수동 보정 번호를 입력하세요.");
        }

        String manualCorrection = dto.getManualCorrection().trim();
        int updated = plateRecognitionMapper.updateManualCorrection(plateRecognitionId, manualCorrection);

        if (updated == 0) {
            throw new IllegalArgumentException("번호판 인식 이력을 찾을 수 없습니다.");
        }

        PlateRecognitionDTO plateRecognition = plateRecognitionMapper.detail(plateRecognitionId);
        exceptionLogService.updateProcessByGateLogId(
                plateRecognition.getGateLogId(),
                "번호판 수동 보정: " + manualCorrection
        );

        return plateRecognition;
    }

    @Transactional
    public PlateRecognitionResultDTO recognize(
            MultipartFile file,
            String ocrType,
            String plateType,
            String gateNumber,
            String gateName,
            String inOutType
    ) throws IOException {
        FastApiPlateResponseDTO aiResult = requestPlateRecognition(file, ocrType);
        normalizePlateNumber(aiResult);
        plateType = normalizePlateType(plateType);

        VehicleDTO vehicle = null;
        TractorVehicleInfoDTO tractorVehicleInfo = null;
        TrailerWorkInfoDTO trailerWorkInfo = null;

        if (aiResult != null && aiResult.getPlateNumber() != null) {
            vehicle = vehicleMapper.findByPlateNumber(aiResult.getPlateNumber());
        }

        boolean matched = vehicle != null;
        boolean isTractor = vehicle != null && "TRACTOR".equalsIgnoreCase(vehicle.getVehicleType());
        boolean isTrailer = vehicle != null && "TRAILER".equalsIgnoreCase(vehicle.getVehicleType());

        PlateRecognitionResultDTO result = new PlateRecognitionResultDTO();
        result.setAiResult(aiResult);
        result.setVehicle(vehicle);
        result.setMatched(matched);

        if (matched) {
            setWorkData(result, vehicle, plateType);
        }

        if (isTractor) {
            tractorVehicleInfo = vehicleMapper.findTractorInfoByPlateNumber(vehicle.getPlateNumber());
        }

        if (isTrailer) {
            trailerWorkInfo = workOrderService.findTrailerWorkInfo(vehicle.getVehicleId());
        }

        result.setTractorVehicleInfo(tractorVehicleInfo);
        result.setTrailerWorkInfo(trailerWorkInfo);

        String businessExceptionType = makeBusinessExceptionType(
                aiResult,
                vehicle,
                plateType,
                matched,
                isTractor,
                isTrailer,
                tractorVehicleInfo,
                trailerWorkInfo,
                result
        );
        boolean hasRequiredInfo = businessExceptionType == null;
        boolean needReview = businessExceptionType != null;

        if (aiResult == null || Boolean.TRUE.equals(aiResult.getNeedReview())) {
            needReview = true;
        }

        printRecognitionCheckLog(aiResult, vehicle, tractorVehicleInfo, trailerWorkInfo);

        GateLogDTO gateLog = createGateLog(vehicle, plateType, needReview, gateNumber, gateName, inOutType);
        plateRecognitionMapper.insertGateLog(gateLog);

        PlateRecognitionDTO plateRecognition = createPlateRecognition(
                gateLog,
                aiResult,
                plateType,
                matched,
                hasRequiredInfo,
                isTractor,
                isTrailer,
                businessExceptionType
        );
        plateRecognitionMapper.insertPlateRecognition(plateRecognition);

        ExceptionLogDTO exceptionLog = createExceptionLog(
                gateLog,
                aiResult,
                vehicle,
                matched,
                isTractor,
                isTrailer,
                businessExceptionType
        );

        if (exceptionLog != null) {
            exceptionLogService.insert(exceptionLog);
        }

        result.setGateLog(gateLog);
        result.setPlateRecognition(plateRecognition);
        result.setNeedReview(needReview);
        result.setMessage(makeMessage(
                aiResult,
                matched,
                isTractor,
                isTrailer,
                hasRequiredInfo,
                needReview,
                tractorVehicleInfo,
                trailerWorkInfo,
                businessExceptionType
        ));

        return result;
    }

    private FastApiPlateResponseDTO requestPlateRecognition(MultipartFile file, String ocrType) throws IOException {
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

        try {
            ResponseEntity<FastApiPlateResponseDTO> response = restTemplate.postForEntity(
                    fastApiUrl,
                    requestEntity,
                    FastApiPlateResponseDTO.class
            );
            return response.getBody();
        } catch (ResourceAccessException e) {
            return null;
        } catch (RestClientException e) {
            return null;
        }
    }

    private GateLogDTO createGateLog(
            VehicleDTO vehicle,
            String plateType,
            boolean needReview,
            String gateNumber,
            String gateName,
            String inOutType
    ) {
        String normalizedInOutType = normalizeInOutType(inOutType);

        GateLogDTO gateLog = new GateLogDTO();
        gateLog.setVehicleId(vehicle == null ? null : vehicle.getVehicleId());
        gateLog.setTractorVehicleId(getPlateVehicleId(vehicle, plateType, "TRACTOR"));
        gateLog.setTrailerVehicleId(getPlateVehicleId(vehicle, plateType, "TRAILER"));
        gateLog.setGateNumber(gateNumber);
        gateLog.setGateName(gateName);
        gateLog.setInOutType(normalizedInOutType);
        gateLog.setProcessResult(needReview ? RECOGNITION_NEED_REVIEW : RECOGNITION_SUCCESS);
        gateLog.setManagerCheck(!needReview);

        if ("OUT".equals(normalizedInOutType)) {
            gateLog.setExitTime(LocalDateTime.now());
        } else {
            gateLog.setEntryTime(LocalDateTime.now());
        }

        return gateLog;
    }

    private String normalizeInOutType(String inOutType) {
        if (inOutType != null && "OUT".equalsIgnoreCase(inOutType)) {
            return "OUT";
        }

        return "IN";
    }

    private PlateRecognitionDTO createPlateRecognition(
            GateLogDTO gateLog,
            FastApiPlateResponseDTO aiResult,
            String plateType,
            boolean matched,
            boolean hasRequiredInfo,
            boolean isTractor,
            boolean isTrailer,
            String businessExceptionType
    ) {
        PlateRecognitionDTO plateRecognition = new PlateRecognitionDTO();
        plateRecognition.setGateLogId(gateLog.getGateLogId());
        plateRecognition.setRecognitionTime(LocalDateTime.now());
        plateRecognition.setPlateType(plateType);

        if (aiResult == null) {
            plateRecognition.setIsSuccess(false);
            plateRecognition.setConfidence(BigDecimal.valueOf(0.0));
            plateRecognition.setErrorMessage("FAST_API_ERROR");
            return plateRecognition;
        }

        plateRecognition.setVehicleImage(aiResult.getCropPath());
        plateRecognition.setRecognizedPlate(aiResult.getPlateNumber());
        plateRecognition.setIsSuccess(Boolean.TRUE.equals(aiResult.getDetected()) && matched && hasRequiredInfo);
        plateRecognition.setConfidence(BigDecimal.valueOf(aiResult.getConfidence() == null ? 0.0 : aiResult.getConfidence()));
        plateRecognition.setErrorMessage(makeErrorMessage(aiResult, matched, isTractor, isTrailer, hasRequiredInfo, businessExceptionType));

        return plateRecognition;
    }

    private Long getPlateVehicleId(VehicleDTO vehicle, String plateType, String targetType) {
        if (vehicle == null || plateType == null) {
            return null;
        }

        if (plateType.equalsIgnoreCase(targetType)) {
            return vehicle.getVehicleId();
        }

        return null;
    }

    private void setWorkData(PlateRecognitionResultDTO result, VehicleDTO vehicle, String plateType) {
        if (plateType == null) {
            return;
        }

        if (plateType.equalsIgnoreCase("TRACTOR")) {
            setTractorData(result, vehicle);
        }

        if (plateType.equalsIgnoreCase("TRAILER")) {
            setTrailerData(result, vehicle);
        }
    }

    private void setTractorData(PlateRecognitionResultDTO result, VehicleDTO vehicle) {
        result.setCarrier(plateRecognitionMapper.findCarrier(vehicle.getCarrierId()));

        WorkOrderDTO workOrder = plateRecognitionMapper.findLatestWorkOrderByTractor(vehicle.getVehicleId());
        result.setWorkOrder(workOrder);

        if (workOrder != null) {
            result.setDriver(plateRecognitionMapper.findDriver(workOrder.getDriverId()));
        }
    }

    private void setTrailerData(PlateRecognitionResultDTO result, VehicleDTO vehicle) {
        WorkOrderDTO workOrder = plateRecognitionMapper.findLatestWorkOrderByTrailer(vehicle.getVehicleId());
        result.setWorkOrder(workOrder);

        if (workOrder == null) {
            return;
        }

        result.setDriver(plateRecognitionMapper.findDriver(workOrder.getDriverId()));

        if (result.getDriver() != null) {
            result.setCarrier(plateRecognitionMapper.findCarrier(result.getDriver().getCarrierId()));
        }

        if (workOrder.getContainerId() == null) {
            return;
        }

        result.setContainer(plateRecognitionMapper.findContainer(workOrder.getContainerId()));

        if (result.getContainer() != null) {
            result.setYardSector(plateRecognitionMapper.findYardSector(result.getContainer().getSectorId()));
        }
    }

    private String makeBusinessExceptionType(
            FastApiPlateResponseDTO aiResult,
            VehicleDTO vehicle,
            String plateType,
            boolean matched,
            boolean isTractor,
            boolean isTrailer,
            TractorVehicleInfoDTO tractorVehicleInfo,
            TrailerWorkInfoDTO trailerWorkInfo,
            PlateRecognitionResultDTO result
    ) {
        if (aiResult == null) {
            return "AI_SERVER_ERROR";
        }

        if (!Boolean.TRUE.equals(aiResult.getDetected())) {
            return "PLATE_NOT_RECOGNIZED";
        }

        if (aiResult.getPlateNumber() == null) {
            return "PLATE_NOT_RECOGNIZED";
        }

        if (!matched) {
            return "VEHICLE_NOT_REGISTERED";
        }

        if (isPlateTypeMismatch(plateType, isTractor, isTrailer)) {
            return "VEHICLE_TYPE_MISMATCH";
        }

        if (matched && !isTractor && !isTrailer) {
            return "UNKNOWN_VEHICLE_TYPE";
        }

        WorkOrderDTO workOrder = result.getWorkOrder();

        if (isTractor && workOrder == null) {
            return "WORK_ORDER_NOT_FOUND";
        }

        if (isTrailer && workOrder == null) {
            return "WORK_ORDER_NOT_FOUND";
        }

        if (workOrder != null && !Boolean.TRUE.equals(workOrder.getIsApproved())) {
            return "WORK_ORDER_NOT_APPROVED";
        }

        if (isTractor && tractorVehicleInfo == null) {
            return "TRACTOR_INFO_NOT_FOUND";
        }

        if (isTractor && (tractorVehicleInfo.getDriverId() == null || isBlank(tractorVehicleInfo.getDriverName()))) {
            return "DRIVER_NOT_FOUND";
        }

        if (workOrder != null && result.getDriver() == null) {
            return "DRIVER_NOT_FOUND";
        }

        if (result.getDriver() != null && !Boolean.TRUE.equals(result.getDriver().getCanEnter())) {
            return "DRIVER_CANNOT_ENTER";
        }

        if (result.getCarrier() != null && !isCarrierActive(result.getCarrier().getCarrierStatus())) {
            return "CARRIER_INACTIVE";
        }

        if (isTrailer && trailerWorkInfo == null) {
            return "WORK_ORDER_NOT_FOUND";
        }

        if (workOrder != null && workOrder.getContainerId() != null && result.getContainer() == null) {
            return "CONTAINER_NOT_FOUND";
        }

        if (result.getContainer() != null && result.getContainer().getSectorId() != null && result.getYardSector() == null) {
            return "YARD_SECTOR_NOT_FOUND";
        }

        return null;
    }

    private boolean isPlateTypeMismatch(String plateType, boolean isTractor, boolean isTrailer) {
        if (plateType == null) {
            return false;
        }

        return (plateType.equalsIgnoreCase("TRACTOR") && !isTractor)
                || (plateType.equalsIgnoreCase("TRAILER") && !isTrailer);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void normalizePlateNumber(FastApiPlateResponseDTO aiResult) {
        if (aiResult == null || aiResult.getPlateNumber() == null) {
            return;
        }

        String plateNumber = aiResult.getPlateNumber().trim();
        aiResult.setPlateNumber(plateNumber.isBlank() ? null : plateNumber);
    }

    private String normalizePlateType(String plateType) {
        if (plateType == null || plateType.isBlank()) {
            return "TRAILER";
        }

        return plateType.trim().toUpperCase();
    }

    private boolean isCarrierActive(String carrierStatus) {
        if (carrierStatus == null) {
            return false;
        }

        return carrierStatus.equalsIgnoreCase("ACTIVE")
                || carrierStatus.equalsIgnoreCase("APPROVED")
                || carrierStatus.equalsIgnoreCase("NORMAL");
    }

    private ExceptionLogDTO createExceptionLog(
            GateLogDTO gateLog,
            FastApiPlateResponseDTO aiResult,
            VehicleDTO vehicle,
            boolean matched,
            boolean isTractor,
            boolean isTrailer,
            String businessExceptionType
    ) {
        String exceptionType = businessExceptionType;

        if (exceptionType == null) {
            return null;
        }

        ExceptionLogDTO exceptionLog = new ExceptionLogDTO();
        exceptionLog.setGateLogId(gateLog.getGateLogId());
        exceptionLog.setVehicleId(vehicle == null ? null : vehicle.getVehicleId());
        exceptionLog.setPlateNumber(aiResult == null ? null : aiResult.getPlateNumber());
        exceptionLog.setExceptionType(exceptionType);
        exceptionLog.setExceptionMessage(makeExceptionMessage(exceptionType));
        exceptionLog.setOccurredTime(LocalDateTime.now());
        exceptionLog.setProcessStatus("UNPROCESSED");

        return exceptionLog;
    }

    private String makeErrorMessage(
            FastApiPlateResponseDTO aiResult,
            boolean matched,
            boolean isTractor,
            boolean isTrailer,
            boolean hasRequiredInfo,
            String businessExceptionType
    ) {
        StringBuilder message = new StringBuilder();

        if ("PLATE_NOT_RECOGNIZED".equals(businessExceptionType)) {
            return "PLATE_NOT_RECOGNIZED";
        }

        if ("AI_SERVER_ERROR".equals(businessExceptionType)) {
            return "AI_SERVER_ERROR";
        }

        if (aiResult.getReviewReasons() != null) {
            for (String reason : aiResult.getReviewReasons()) {
                message.append(reason).append(",");
            }
        }

        if (!matched) {
            message.append("VEHICLE_NOT_REGISTERED,");
        }

        if (businessExceptionType == null && matched && isTractor && !hasRequiredInfo) {
            message.append("TRACTOR_INFO_NOT_FOUND,");
        }

        if (businessExceptionType == null && matched && isTrailer && !hasRequiredInfo) {
            message.append("WORK_ORDER_NOT_FOUND,");
        }

        if (businessExceptionType == null && matched && !isTractor && !isTrailer) {
            message.append("UNKNOWN_VEHICLE_TYPE,");
        }

        if (businessExceptionType != null && message.indexOf(businessExceptionType) < 0) {
            message.append(businessExceptionType).append(",");
        }

        if (message.length() > 0 && message.charAt(message.length() - 1) == ',') {
            message.deleteCharAt(message.length() - 1);
        }

        return message.isEmpty() ? null : message.toString();
    }

    private String makeExceptionMessage(String exceptionType) {
        if ("AI_SERVER_ERROR".equals(exceptionType)) {
            return "AI 서버 응답이 없습니다.";
        }

        if ("PLATE_NOT_RECOGNIZED".equals(exceptionType)) {
            return "번호판을 인식하지 못했습니다.";
        }

        if ("VEHICLE_NOT_REGISTERED".equals(exceptionType)) {
            return "인식된 차량번호가 등록되어 있지 않습니다.";
        }

        if ("VEHICLE_TYPE_MISMATCH".equals(exceptionType)) {
            return "요청한 번호판 유형과 등록된 차량 유형이 일치하지 않습니다.";
        }

        if ("DRIVER_NOT_FOUND".equals(exceptionType)) {
            return "기사 정보를 찾을 수 없습니다.";
        }

        if ("DRIVER_CANNOT_ENTER".equals(exceptionType)) {
            return "기사의 출입이 허용되지 않았습니다.";
        }

        if ("CARRIER_INACTIVE".equals(exceptionType)) {
            return "운송사 상태가 출입 가능한 상태가 아닙니다.";
        }

        if ("WORK_ORDER_NOT_FOUND".equals(exceptionType)) {
            return "배정된 작업지시를 찾을 수 없습니다.";
        }

        if ("WORK_ORDER_NOT_APPROVED".equals(exceptionType)) {
            return "승인되지 않은 작업지시입니다.";
        }

        if ("CONTAINER_NOT_FOUND".equals(exceptionType)) {
            return "컨테이너 정보를 찾을 수 없습니다.";
        }

        if ("YARD_SECTOR_NOT_FOUND".equals(exceptionType)) {
            return "야드 섹터 정보를 찾을 수 없습니다.";
        }

        if ("TRACTOR_INFO_NOT_FOUND".equals(exceptionType)) {
            return "트랙터 상세 정보를 찾을 수 없습니다.";
        }

        if ("UNKNOWN_VEHICLE_TYPE".equals(exceptionType)) {
            return "차량 유형은 TRACTOR 또는 TRAILER여야 합니다.";
        }

        return exceptionType;
    }

    private String makeMessage(
            FastApiPlateResponseDTO aiResult,
            boolean matched,
            boolean isTractor,
            boolean isTrailer,
            boolean hasRequiredInfo,
            boolean needReview,
            TractorVehicleInfoDTO tractorVehicleInfo,
            TrailerWorkInfoDTO trailerWorkInfo,
            String businessExceptionType
    ) {
        if (businessExceptionType != null) {
            return makeExceptionMessage(businessExceptionType);
        }

        if (aiResult == null) {
            return "AI 서버 응답이 없습니다.";
        }

        if (!Boolean.TRUE.equals(aiResult.getDetected())) {
            return "번호판을 인식하지 못했습니다.";
        }

        if (!matched) {
            return "번호판은 인식했지만 등록된 차량이 아닙니다.";
        }

        if (isTractor && !hasRequiredInfo) {
            return "등록된 트랙터이지만 상세 정보가 부족합니다.";
        }

        if (isTrailer && !hasRequiredInfo) {
            return "등록된 트레일러이지만 배정된 작업정보가 없습니다.";
        }

        if (!isTractor && !isTrailer) {
            return "등록된 차량이지만 차량 유형이 TRACTOR 또는 TRAILER가 아닙니다.";
        }

        if (needReview) {
            return "번호판 인식 결과에 관리자 확인이 필요합니다.";
        }

        if (tractorVehicleInfo != null) {
            return "트랙터 차량 조회가 완료되었습니다.";
        }

        if (trailerWorkInfo != null && trailerWorkInfo.getWorkGuideMessage() != null) {
            return trailerWorkInfo.getWorkGuideMessage();
        }

        return "번호판 인식 및 작업정보 조회가 완료되었습니다.";
    }

    private void printRecognitionCheckLog(
            FastApiPlateResponseDTO aiResult,
            VehicleDTO vehicle,
            TractorVehicleInfoDTO tractorVehicleInfo,
            TrailerWorkInfoDTO trailerWorkInfo
    ) {
        System.out.println("========== Plate recognition DB check ==========");

        if (aiResult == null) {
            System.out.println("AI response: empty");
            System.out.println("===============================================");
            return;
        }

        System.out.println("recognizedPlate: " + aiResult.getPlateNumber());

        if (vehicle == null) {
            System.out.println("registeredVehicle: false");
            System.out.println("vehicleLookup: empty");
            System.out.println("===============================================");
            return;
        }

        System.out.println("vehicleType: " + vehicle.getVehicleType());

        if ("TRACTOR".equalsIgnoreCase(vehicle.getVehicleType())) {
            printTractorLog(tractorVehicleInfo);
        } else if ("TRAILER".equalsIgnoreCase(vehicle.getVehicleType())) {
            printTrailerLog(trailerWorkInfo);
        } else {
            System.out.println("plateNumber: " + vehicle.getPlateNumber());
            System.out.println("registeredVehicle: " + Boolean.TRUE.equals(vehicle.getIsRegistered()));
            System.out.println("vehicleStatus: " + valueOrDash(vehicle.getVehicleStatus()));
        }

        System.out.println("===============================================");
    }

    private void printTractorLog(TractorVehicleInfoDTO tractorVehicleInfo) {
        if (tractorVehicleInfo == null) {
            System.out.println("tractorLookup: empty");
            return;
        }

        System.out.println("plateNumber: " + tractorVehicleInfo.getPlateNumber());
        System.out.println("registeredVehicle: " + tractorVehicleInfo.getRegisteredText());
        System.out.println("carrierName: " + valueOrDash(tractorVehicleInfo.getCarrierName()));
        System.out.println("driverName: " + valueOrDash(tractorVehicleInfo.getDriverName()));
        System.out.println("vehicleStatus: " + valueOrDash(tractorVehicleInfo.getVehicleStatus()));
        System.out.println("tractorNo: " + valueOrDash(tractorVehicleInfo.getTractorNo()));
    }

    private void printTrailerLog(TrailerWorkInfoDTO trailerWorkInfo) {
        if (trailerWorkInfo == null) {
            System.out.println("trailerWorkLookup: empty");
            return;
        }

        System.out.println("workOrderId: " + trailerWorkInfo.getWorkOrderId());
        System.out.println("containerNumber: " + valueOrDash(trailerWorkInfo.getContainerNumber()));
        System.out.println("workType: " + valueOrDash(trailerWorkInfo.getWorkType()));
        System.out.println("containerLocation: " + valueOrDash(trailerWorkInfo.getContainerLocation()));
        System.out.println("yardLocation: " + valueOrDash(trailerWorkInfo.getYardLocation()));
        System.out.println("sectorName: " + valueOrDash(trailerWorkInfo.getSectorName()));
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
