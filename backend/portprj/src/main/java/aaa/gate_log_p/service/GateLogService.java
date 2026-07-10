package aaa.gate_log_p.service;

import aaa.exception_log_p.model.ExceptionLogDTO;
import aaa.exception_log_p.service.ExceptionLogService;
import aaa.gate_log_p.model.GateProcessRequestDTO;
import aaa.gate_log_p.model.GateProcessResultDTO;
import aaa.gate_log_p.model.GateLogDTO;
import aaa.gate_log_p.model.GateLogMapper;
import aaa.work_order_p.service.WorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GateLogService {

    @Resource
    GateLogMapper mapper;

    @Resource
    WorkOrderService workOrderService;

    @Resource
    ExceptionLogService exceptionLogService;

    public List<GateLogDTO> list() {
        return mapper.list();
    }

    public GateProcessResultDTO process(GateProcessRequestDTO dto) {
        GateProcessResultDTO result = new GateProcessResultDTO();
        String failReason = validateRequest(dto);
        String inOutType = normalizeInOutType(dto == null ? null : dto.getInOutType());

        if (failReason != null) {
            GateLogDTO gateLog = createGateLog(dto, inOutType, "FAIL", false);
            mapper.insert(gateLog);

            insertExceptionLog(gateLog, dto, failReason);

            result.setSuccess(false);
            result.setGateLogId(gateLog.getGateLogId());
            result.setWorkOrderId(dto == null ? null : dto.getWorkOrderId());
            result.setProcessResult("FAIL");
            result.setExceptionType(failReason);
            result.setMessage(makeFailMessage(failReason));
            return result;
        }

        GateLogDTO gateLog = createGateLog(dto, inOutType, "SUCCESS", true);
        mapper.insert(gateLog);

        String workStatus = "OUT".equals(inOutType) ? "GATE_OUT" : "GATE_IN";
        workOrderService.updateStatus(dto.getWorkOrderId(), workStatus);

        result.setSuccess(true);
        result.setGateLogId(gateLog.getGateLogId());
        result.setWorkOrderId(dto.getWorkOrderId());
        result.setWorkStatus(workStatus);
        result.setProcessResult("SUCCESS");
        result.setMessage("OUT".equals(inOutType) ? "출차 처리가 완료되었습니다." : "입차 처리가 완료되었습니다.");
        return result;
    }

    private String validateRequest(GateProcessRequestDTO dto) {
        if (dto == null) {
            return "REQUEST_EMPTY";
        }

        if (dto.getTractorVehicleId() == null) {
            return "TRACTOR_EMPTY";
        }

        if (dto.getTrailerVehicleId() == null) {
            return "TRAILER_EMPTY";
        }

        if (dto.getWorkOrderId() == null) {
            return "WORK_ORDER_EMPTY";
        }

        if (dto.getContainerId() == null) {
            return "CONTAINER_EMPTY";
        }

        if (dto.getSectorId() == null) {
            return "SECTOR_EMPTY";
        }

        return null;
    }

    private GateLogDTO createGateLog(GateProcessRequestDTO dto, String inOutType, String processResult, boolean managerCheck) {
        GateLogDTO gateLog = new GateLogDTO();
        gateLog.setVehicleId(dto == null ? null : dto.getTrailerVehicleId());
        gateLog.setTractorVehicleId(dto == null ? null : dto.getTractorVehicleId());
        gateLog.setTrailerVehicleId(dto == null ? null : dto.getTrailerVehicleId());
        gateLog.setGateNumber(dto == null || dto.getGateNumber() == null || dto.getGateNumber().isBlank() ? "G01" : dto.getGateNumber());
        gateLog.setGateName(dto == null || dto.getGateName() == null || dto.getGateName().isBlank() ? "AI_GATE" : dto.getGateName());
        gateLog.setInOutType(inOutType);
        gateLog.setProcessResult(processResult);
        gateLog.setManagerCheck(managerCheck);

        if ("OUT".equals(inOutType)) {
            gateLog.setExitTime(LocalDateTime.now());
        } else {
            gateLog.setEntryTime(LocalDateTime.now());
        }

        return gateLog;
    }

    private void insertExceptionLog(GateLogDTO gateLog, GateProcessRequestDTO dto, String failReason) {
        ExceptionLogDTO exceptionLog = new ExceptionLogDTO();
        exceptionLog.setGateLogId(gateLog.getGateLogId());
        exceptionLog.setVehicleId(dto == null ? null : dto.getTrailerVehicleId());
        exceptionLog.setExceptionType(failReason);
        exceptionLog.setExceptionMessage(makeFailMessage(failReason));
        exceptionLog.setOccurredTime(LocalDateTime.now());
        exceptionLog.setProcessStatus("UNPROCESSED");
        exceptionLogService.insert(exceptionLog);
    }

    private String normalizeInOutType(String inOutType) {
        if (inOutType != null && "OUT".equalsIgnoreCase(inOutType)) {
            return "OUT";
        }

        return "IN";
    }

    private String makeFailMessage(String failReason) {
        if ("REQUEST_EMPTY".equals(failReason)) {
            return "출입 처리 요청값이 없습니다.";
        }

        if ("TRACTOR_EMPTY".equals(failReason)) {
            return "트랙터 차량 정보가 없습니다.";
        }

        if ("TRAILER_EMPTY".equals(failReason)) {
            return "트레일러 차량 정보가 없습니다.";
        }

        if ("WORK_ORDER_EMPTY".equals(failReason)) {
            return "작업정보가 없습니다.";
        }

        if ("CONTAINER_EMPTY".equals(failReason)) {
            return "컨테이너 정보가 없습니다.";
        }

        if ("SECTOR_EMPTY".equals(failReason)) {
            return "야드 섹터 정보가 없습니다.";
        }

        return "출입 처리를 진행할 수 없습니다.";
    }
}
