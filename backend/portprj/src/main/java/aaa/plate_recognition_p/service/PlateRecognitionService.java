package aaa.plate_recognition_p.service;

import aaa.exception_log_p.model.ExceptionLogDTO;
import aaa.exception_log_p.service.ExceptionLogService;
import aaa.gate_log_p.model.GateLogDTO;
import aaa.plate_recognition_p.model.FastApiPlateResponseDTO;
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

    public PlateRecognitionResultDTO recognize(
            MultipartFile file,
            String ocrType,
            String plateType,
            String gateNumber,
            String gateName,
            String inOutType
    ) throws IOException {
        FastApiPlateResponseDTO aiResult = requestPlateRecognition(file, ocrType);

        VehicleDTO vehicle = null;
        TractorVehicleInfoDTO tractorVehicleInfo = null;
        TrailerWorkInfoDTO trailerWorkInfo = null;

        if (aiResult != null && aiResult.getPlateNumber() != null) {
            vehicle = vehicleMapper.findByPlateNumber(aiResult.getPlateNumber());
        }

        boolean matched = vehicle != null;
        boolean isTractor = vehicle != null && "TRACTOR".equalsIgnoreCase(vehicle.getVehicleType());
        boolean isTrailer = vehicle != null && "TRAILER".equalsIgnoreCase(vehicle.getVehicleType());

        if (isTractor) {
            tractorVehicleInfo = vehicleMapper.findTractorInfoByPlateNumber(vehicle.getPlateNumber());
        }

        if (isTrailer) {
            trailerWorkInfo = workOrderService.findTrailerWorkInfo(vehicle.getVehicleId());
        }

        boolean hasRequiredInfo = (isTractor && tractorVehicleInfo != null) || (isTrailer && trailerWorkInfo != null);
        boolean needReview = !matched || !hasRequiredInfo;

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
                isTrailer
        );
        plateRecognitionMapper.insertPlateRecognition(plateRecognition);

        ExceptionLogDTO exceptionLog = createExceptionLog(
                gateLog,
                aiResult,
                vehicle,
                matched,
                isTractor,
                isTrailer,
                hasRequiredInfo
        );

        if (exceptionLog != null) {
            exceptionLogService.insert(exceptionLog);
        }

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

        if (matched) {
            setWorkData(result, vehicle, plateType);
        }

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
        gateLog.setProcessResult(needReview ? "NEED_REVIEW" : "SUCCESS");
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
            boolean isTrailer
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
        plateRecognition.setErrorMessage(makeErrorMessage(aiResult, matched, isTractor, isTrailer, hasRequiredInfo));

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

        if (workOrder.getContainerId() == null) {
            return;
        }

        result.setContainer(plateRecognitionMapper.findContainer(workOrder.getContainerId()));

        if (result.getContainer() != null) {
            result.setYardSector(plateRecognitionMapper.findYardSector(result.getContainer().getSectorId()));
        }
    }

    private ExceptionLogDTO createExceptionLog(
            GateLogDTO gateLog,
            FastApiPlateResponseDTO aiResult,
            VehicleDTO vehicle,
            boolean matched,
            boolean isTractor,
            boolean isTrailer,
            boolean hasRequiredInfo
    ) {
        String exceptionType = null;
        String exceptionMessage = null;

        if (aiResult == null) {
            exceptionType = "AI_SERVER_ERROR";
            exceptionMessage = "AI server response is empty.";
        } else if (!Boolean.TRUE.equals(aiResult.getDetected())) {
            exceptionType = "PLATE_NOT_RECOGNIZED";
            exceptionMessage = "Plate was not recognized.";
        } else if (!matched) {
            exceptionType = "VEHICLE_NOT_REGISTERED";
            exceptionMessage = "Recognized plate is not registered.";
        } else if (isTrailer && !hasRequiredInfo) {
            exceptionType = "WORK_ORDER_NOT_FOUND";
            exceptionMessage = "Assigned work order was not found for trailer.";
        } else if (isTractor && !hasRequiredInfo) {
            exceptionType = "TRACTOR_INFO_NOT_FOUND";
            exceptionMessage = "Tractor detail information was not found.";
        } else if (matched && !isTractor && !isTrailer) {
            exceptionType = "UNKNOWN_VEHICLE_TYPE";
            exceptionMessage = "Vehicle type must be TRACTOR or TRAILER.";
        }

        if (exceptionType == null) {
            return null;
        }

        ExceptionLogDTO exceptionLog = new ExceptionLogDTO();
        exceptionLog.setGateLogId(gateLog.getGateLogId());
        exceptionLog.setVehicleId(vehicle == null ? null : vehicle.getVehicleId());
        exceptionLog.setPlateNumber(aiResult == null ? null : aiResult.getPlateNumber());
        exceptionLog.setExceptionType(exceptionType);
        exceptionLog.setExceptionMessage(exceptionMessage);
        exceptionLog.setOccurredTime(LocalDateTime.now());
        exceptionLog.setProcessStatus("UNPROCESSED");

        return exceptionLog;
    }

    private String makeErrorMessage(
            FastApiPlateResponseDTO aiResult,
            boolean matched,
            boolean isTractor,
            boolean isTrailer,
            boolean hasRequiredInfo
    ) {
        StringBuilder message = new StringBuilder();

        if (aiResult.getReviewReasons() != null) {
            for (String reason : aiResult.getReviewReasons()) {
                message.append(reason).append(",");
            }
        }

        if (!matched) {
            message.append("VEHICLE_NOT_REGISTERED,");
        }

        if (matched && isTractor && !hasRequiredInfo) {
            message.append("TRACTOR_INFO_NOT_FOUND,");
        }

        if (matched && isTrailer && !hasRequiredInfo) {
            message.append("WORK_ORDER_NOT_FOUND,");
        }

        if (matched && !isTractor && !isTrailer) {
            message.append("UNKNOWN_VEHICLE_TYPE,");
        }

        if (message.length() > 0 && message.charAt(message.length() - 1) == ',') {
            message.deleteCharAt(message.length() - 1);
        }

        return message.isEmpty() ? null : message.toString();
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
        if (aiResult == null) {
            return "FastAPI response is empty.";
        }

        if (!Boolean.TRUE.equals(aiResult.getDetected())) {
            return "Plate was not detected.";
        }

        if (!matched) {
            return "Plate recognized, but vehicle is not registered.";
        }

        if (isTractor && !hasRequiredInfo) {
            return "Registered tractor found, but tractor detail information is missing.";
        }

        if (isTrailer && !hasRequiredInfo) {
            return "Registered trailer found, but assigned work order is missing.";
        }

        if (!isTractor && !isTrailer) {
            return "Vehicle is registered, but vehicle type must be TRACTOR or TRAILER.";
        }

        if (needReview) {
            return "Plate recognition result needs review.";
        }

        if (tractorVehicleInfo != null) {
            return "Tractor vehicle lookup completed.";
        }

        if (trailerWorkInfo != null && trailerWorkInfo.getWorkGuideMessage() != null) {
            return trailerWorkInfo.getWorkGuideMessage();
        }

        return "Plate recognition and work order lookup completed.";
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
