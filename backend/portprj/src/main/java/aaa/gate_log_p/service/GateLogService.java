package aaa.gate_log_p.service;

import aaa.container_p.model.ContainerDTO;
import aaa.container_p.service.ContainerService;
import aaa.exception_log_p.model.ExceptionLogDTO;
import aaa.exception_log_p.service.ExceptionLogService;
import aaa.gate_log_p.model.GateLogDTO;
import aaa.gate_log_p.model.GateLogMapper;
import aaa.gate_log_p.model.GateProcessRequestDTO;
import aaa.gate_log_p.model.GateProcessResultDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.service.WorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class GateLogService {

    private static final String GATE_SUCCESS = "GATE_SUCCESS";
    private static final String GATE_FAIL = "GATE_FAIL";

    @Resource
    GateLogMapper mapper;

    @Resource
    WorkOrderService workOrderService;

    @Resource
    ContainerService containerService;

    @Resource
    ExceptionLogService exceptionLogService;

    public List<GateLogDTO> list() {
        return mapper.list();
    }

    @Transactional
    public GateProcessResultDTO process(GateProcessRequestDTO dto) {
        String failReason = validateRequest(dto);
        String inOutType = normalizeInOutType(dto == null ? null : dto.getInOutType());

        if (failReason != null) {
            return fail(dto, inOutType, failReason);
        }

        WorkOrderDTO workOrder = workOrderService.detail(dto.getWorkOrderId());
        failReason = validateWorkOrder(dto, workOrder, inOutType);

        if (failReason != null) {
            return fail(dto, inOutType, failReason);
        }

        String workStatus = "OUT".equals(inOutType) ? "GATE_OUT" : "GATE_IN";
        int updated = workOrderService.updateStatus(dto.getWorkOrderId(), workStatus);

        if (updated == 0) {
            return fail(dto, inOutType, "WORK_ORDER_UPDATE_FAILED");
        }

        containerService.blockExit(dto.getContainerId());

        GateLogDTO gateLog = createGateLog(dto, inOutType, GATE_SUCCESS, true);
        mapper.insert(gateLog);

        GateProcessResultDTO result = new GateProcessResultDTO();
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

        WorkOrderDTO workOrder = workOrderService.detail(dto.getWorkOrderId());

        if (workOrder == null) {
            return "WORK_ORDER_NOT_FOUND";
        }

        if (!Boolean.TRUE.equals(workOrder.getIsApproved())) {
            return "WORK_ORDER_NOT_APPROVED";
        }

        WorkOrderDTO tractorWorkOrder = workOrderService.findLatestByTractorVehicleId(dto.getTractorVehicleId());
        WorkOrderDTO trailerWorkOrder = workOrderService.findLatestByTrailerVehicleId(dto.getTrailerVehicleId());

        if (tractorWorkOrder != null && trailerWorkOrder != null) {
            boolean sameWorkOrder = Objects.equals(
                    tractorWorkOrder.getWorkOrderId(),
                    trailerWorkOrder.getWorkOrderId()
            );
            boolean requestMatchesWorkOrder = Objects.equals(
                    dto.getWorkOrderId(),
                    tractorWorkOrder.getWorkOrderId()
            );

            if (!sameWorkOrder || !requestMatchesWorkOrder) {
                return "WORK_ORDER_MISMATCH";
            }
        }

        if (workOrder.getContainerId() != null && !workOrder.getContainerId().equals(dto.getContainerId())) {
            return "CONTAINER_MISMATCH";
        }

        if (workOrder.getTractorVehicleId() != null && !workOrder.getTractorVehicleId().equals(dto.getTractorVehicleId())) {
            return "TRACTOR_MISMATCH";
        }

        if (workOrder.getTrailerVehicleId() != null && !workOrder.getTrailerVehicleId().equals(dto.getTrailerVehicleId())) {
            return "TRAILER_MISMATCH";
        }

        String inOutType = normalizeInOutType(dto.getInOutType());

        if ("IN".equals(inOutType) && !"APPROVED".equals(workOrder.getWorkStatus())) {
            return "INVALID_IN_STATUS";
        }

        if ("OUT".equals(inOutType) && !"COMPLETED".equals(workOrder.getWorkStatus())) {
            return "INVALID_OUT_STATUS";
        }

        return null;
    }

    private String validateWorkOrder(GateProcessRequestDTO dto, WorkOrderDTO workOrder, String inOutType) {
        if (workOrder == null) {
            return "WORK_ORDER_NOT_FOUND";
        }

        if (!matchesVehicle(dto.getTractorVehicleId(), workOrder.getTractorVehicleId(), workOrder.getVehicleId())) {
            return "TRACTOR_MISMATCH";
        }

        if (!matchesVehicle(dto.getTrailerVehicleId(), workOrder.getTrailerVehicleId(), workOrder.getVehicleId())) {
            return "TRAILER_MISMATCH";
        }

        if (!Objects.equals(dto.getContainerId(), workOrder.getContainerId())) {
            return "CONTAINER_MISMATCH";
        }

        ContainerDTO container = containerService.detail(dto.getContainerId());
        if (container == null) {
            return "CONTAINER_NOT_FOUND";
        }

        if (!Objects.equals(dto.getSectorId(), container.getSectorId())) {
            return "SECTOR_MISMATCH";
        }

        if (!Boolean.TRUE.equals(workOrder.getIsApproved())) {
            return "WORK_ORDER_NOT_APPROVED";
        }

        String currentStatus = workOrder.getWorkStatus();

        if ("CANCELED".equalsIgnoreCase(currentStatus)) {
            return "WORK_ORDER_CANCELED";
        }

        if ("IN".equals(inOutType) && isStatusAny(currentStatus, "GATE_OUT", "COMPLETED", "출차완료", "상차완료", "하차완료")) {
            return "WORK_ORDER_ALREADY_CLOSED";
        }

        if ("OUT".equals(inOutType) && !isStatusAny(currentStatus, "COMPLETED", "상차완료", "하차완료")) {
            return "WORK_ORDER_NOT_COMPLETED";
        }

        if ("OUT".equals(inOutType) && !Boolean.TRUE.equals(container.getCanExit())) {
            return "CONTAINER_EXIT_NOT_ALLOWED";
        }

        return null;
    }

    private boolean matchesVehicle(Long requestedVehicleId, Long workOrderVehicleId, Long legacyVehicleId) {
        return Objects.equals(requestedVehicleId, workOrderVehicleId)
                || Objects.equals(requestedVehicleId, legacyVehicleId);
    }

    private boolean isStatusAny(String status, String... targets) {
        if (status == null) {
            return false;
        }

        for (String target : targets) {
            if (status.equalsIgnoreCase(target)) {
                return true;
            }
        }

        return false;
    }

    private GateProcessResultDTO fail(GateProcessRequestDTO dto, String inOutType, String failReason) {
        GateLogDTO gateLog = createGateLog(dto, inOutType, GATE_FAIL, false);
        mapper.insert(gateLog);

        insertExceptionLog(gateLog, dto, failReason);

        GateProcessResultDTO result = new GateProcessResultDTO();
        result.setSuccess(false);
        result.setGateLogId(gateLog.getGateLogId());
        result.setWorkOrderId(dto == null ? null : dto.getWorkOrderId());
        result.setProcessResult("FAIL");
        result.setExceptionType(failReason);
        result.setMessage(makeFailMessage(failReason));
        return result;
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
        if ("WORK_ORDER_NOT_FOUND".equals(failReason)) {
            return "작업지시를 찾을 수 없습니다.";
        }
        if ("WORK_ORDER_NOT_APPROVED".equals(failReason)) {
            return "승인되지 않은 작업지시입니다.";
        }
        if ("WORK_ORDER_MISMATCH".equals(failReason)) {
            return "트랙터와 트레일러가 같은 WorkOrder에 연결되어 있지 않습니다.";
        }
        if ("WORK_ORDER_UPDATE_FAILED".equals(failReason)) {
            return "작업 상태 변경에 실패했습니다.";
        }
        if ("WORK_ORDER_CANCELED".equals(failReason)) {
            return "취소된 작업지시입니다.";
        }
        if ("WORK_ORDER_ALREADY_CLOSED".equals(failReason)) {
            return "이미 종료된 작업지시입니다.";
        }
        if ("WORK_ORDER_NOT_GATE_IN".equals(failReason)) {
            return "입차 처리되지 않은 작업은 출차 처리할 수 없습니다.";
        }
        if ("WORK_ORDER_NOT_COMPLETED".equals(failReason)) {
            return "야드 작업이 완료된 후 출차 처리할 수 있습니다.";
        }
        if ("CONTAINER_EXIT_NOT_ALLOWED".equals(failReason)) {
            return "컨테이너가 출차 가능 상태가 아닙니다.";
        }
        if ("TRACTOR_MISMATCH".equals(failReason)) {
            return "트랙터 차량이 작업지시와 일치하지 않습니다.";
        }
        if ("TRAILER_MISMATCH".equals(failReason)) {
            return "트레일러 차량이 작업지시와 일치하지 않습니다.";
        }
        if ("CONTAINER_MISMATCH".equals(failReason)) {
            return "컨테이너 정보가 작업지시와 일치하지 않습니다.";
        }
        if ("CONTAINER_NOT_FOUND".equals(failReason)) {
            return "컨테이너 정보를 찾을 수 없습니다.";
        }
        if ("CONTAINER_EMPTY".equals(failReason)) {
            return "컨테이너 정보가 없습니다.";
        }
        if ("SECTOR_EMPTY".equals(failReason)) {
            return "야드 섹터 정보가 없습니다.";
        }
        if ("SECTOR_MISMATCH".equals(failReason)) {
            return "야드 섹터 정보가 컨테이너와 일치하지 않습니다.";
        }

        if ("WORK_ORDER_NOT_FOUND".equals(failReason)) {
            return "작업정보를 찾을 수 없습니다.";
        }

        if ("WORK_ORDER_NOT_APPROVED".equals(failReason)) {
            return "승인되지 않은 작업입니다.";
        }

        if ("CONTAINER_MISMATCH".equals(failReason)) {
            return "작업정보와 컨테이너 정보가 일치하지 않습니다.";
        }

        if ("TRACTOR_MISMATCH".equals(failReason)) {
            return "작업정보와 트랙터 정보가 일치하지 않습니다.";
        }

        if ("TRAILER_MISMATCH".equals(failReason)) {
            return "작업정보와 트레일러 정보가 일치하지 않습니다.";
        }

        if ("INVALID_IN_STATUS".equals(failReason)) {
            return "승인 완료 상태의 작업만 입차할 수 있습니다.";
        }

        if ("INVALID_OUT_STATUS".equals(failReason)) {
            return "작업 완료 상태에서만 출차할 수 있습니다.";
        }

        return "출입 처리를 진행할 수 없습니다.";
    }
}
