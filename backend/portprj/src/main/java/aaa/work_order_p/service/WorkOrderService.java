package aaa.work_order_p.service;

import aaa.container_p.service.ContainerService;
import aaa.work_order_p.model.TrailerWorkInfoDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.model.WorkOrderMapper;
import aaa.work_order_p.model.WorkOrderProcessResultDTO;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WorkOrderService {

    private static final String STATUS_DISPATCH_WAITING = "DISPATCH_WAITING";
    private static final String STATUS_REQUESTED = "REQUESTED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_GATE_IN = "GATE_IN";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_GATE_OUT = "GATE_OUT";

    @Resource
    WorkOrderMapper mapper;

    @Resource
    ContainerService containerService;

    public List<WorkOrderDTO> list() {
        return mapper.list();
    }

    public WorkOrderDTO detail(Long workOrderId) {
        return mapper.detail(workOrderId);
    }

    public int insert(WorkOrderDTO dto) {
        if (dto.getWorkStatus() == null || dto.getWorkStatus().isBlank()) {
            dto.setWorkStatus(STATUS_DISPATCH_WAITING);
        }

        if (dto.getIsApproved() == null) {
            dto.setIsApproved(false);
        }

        return mapper.insert(dto);
    }

    public TrailerWorkInfoDTO findTrailerWorkInfo(Long vehicleId) {
        return mapper.findTrailerWorkInfoByVehicleId(vehicleId);
    }

    public int updateStatus(Long workOrderId, String workStatus) {
        return mapper.updateStatus(workOrderId, workStatus);
    }

    public WorkOrderDTO approve(Long workOrderId) {
        WorkOrderDTO workOrder = getWorkOrder(workOrderId);

        if (!isRequestWaiting(workOrder.getWorkStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인 대기 작업만 승인할 수 있습니다.");
        }

        mapper.approve(workOrderId);
        return mapper.detail(workOrderId);
    }

    public WorkOrderDTO reject(Long workOrderId) {
        WorkOrderDTO workOrder = getWorkOrder(workOrderId);

        if (!isRequestWaiting(workOrder.getWorkStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인 대기 작업만 반려할 수 있습니다.");
        }

        mapper.reject(workOrderId);
        return mapper.detail(workOrderId);
    }

    private boolean isRequestWaiting(String workStatus) {
        return STATUS_DISPATCH_WAITING.equals(workStatus)
                || STATUS_REQUESTED.equals(workStatus)
                || STATUS_PENDING.equals(workStatus);
    }

    @Transactional
    public WorkOrderProcessResultDTO start(Long workOrderId) {
        WorkOrderDTO workOrder = detail(workOrderId);

        if (workOrder == null) {
            return fail(workOrderId, null, "WORK_ORDER_NOT_FOUND", "작업 지시를 찾을 수 없습니다.");
        }

        if (!Boolean.TRUE.equals(workOrder.getIsApproved())) {
            return fail(workOrderId, workOrder.getWorkStatus(), "WORK_ORDER_NOT_APPROVED", "승인되지 않은 작업입니다.");
        }

        String currentStatus = workOrder.getWorkStatus();

        if (STATUS_IN_PROGRESS.equals(currentStatus)) {
            return fail(workOrderId, currentStatus, "WORK_ORDER_ALREADY_STARTED", "이미 시작된 작업입니다.");
        }

        if (STATUS_COMPLETED.equals(currentStatus)) {
            return fail(workOrderId, currentStatus, "WORK_ORDER_ALREADY_COMPLETED", "이미 완료된 작업입니다.");
        }

        if (STATUS_GATE_OUT.equals(currentStatus)) {
            return fail(workOrderId, currentStatus, "WORK_ORDER_ALREADY_CLOSED", "이미 출차 완료된 작업입니다.");
        }

        if (!STATUS_GATE_IN.equals(currentStatus)) {
            return fail(workOrderId, currentStatus, "WORK_ORDER_NOT_GATE_IN", "입차 완료 작업만 시작할 수 있습니다.");
        }

        if (workOrder.getContainerId() != null) {
            containerService.blockExit(workOrder.getContainerId());
        }

        int updated = updateStatus(workOrderId, STATUS_IN_PROGRESS);

        if (updated == 0) {
            return fail(workOrderId, currentStatus, "WORK_ORDER_UPDATE_FAILED", "작업 상태 변경에 실패했습니다.");
        }

        return success(workOrderId, STATUS_IN_PROGRESS, "작업이 시작되었습니다.");
    }

    @Transactional
    public WorkOrderProcessResultDTO complete(Long workOrderId) {
        WorkOrderDTO workOrder = detail(workOrderId);

        if (workOrder == null) {
            return fail(workOrderId, null, "WORK_ORDER_NOT_FOUND", "작업 지시를 찾을 수 없습니다.");
        }

        if (!Boolean.TRUE.equals(workOrder.getIsApproved())) {
            return fail(workOrderId, workOrder.getWorkStatus(), "WORK_ORDER_NOT_APPROVED", "승인되지 않은 작업입니다.");
        }

        String currentStatus = workOrder.getWorkStatus();

        if (STATUS_COMPLETED.equals(currentStatus)) {
            return fail(workOrderId, currentStatus, "WORK_ORDER_ALREADY_COMPLETED", "이미 완료된 작업입니다.");
        }

        if (STATUS_GATE_OUT.equals(currentStatus)) {
            return fail(workOrderId, currentStatus, "WORK_ORDER_ALREADY_CLOSED", "이미 출차 완료된 작업입니다.");
        }

        if (!STATUS_IN_PROGRESS.equals(currentStatus)) {
            return fail(workOrderId, currentStatus, "WORK_ORDER_NOT_IN_PROGRESS", "작업 진행 중인 작업만 완료할 수 있습니다.");
        }

        if (workOrder.getContainerId() == null) {
            return fail(workOrderId, currentStatus, "CONTAINER_NOT_FOUND", "작업에 연결된 컨테이너 정보가 없습니다.");
        }

        int containerUpdated = containerService.allowExit(workOrder.getContainerId());

        if (containerUpdated == 0) {
            return fail(workOrderId, currentStatus, "CONTAINER_UPDATE_FAILED", "컨테이너 출차 가능 상태 변경에 실패했습니다.");
        }

        int updated = updateStatus(workOrderId, STATUS_COMPLETED);

        if (updated == 0) {
            containerService.blockExit(workOrder.getContainerId());
            return fail(workOrderId, currentStatus, "WORK_ORDER_UPDATE_FAILED", "작업 상태 변경에 실패했습니다.");
        }

        return success(workOrderId, STATUS_COMPLETED, makeCompleteMessage(workOrder.getWorkType()));
    }

    private WorkOrderDTO getWorkOrder(Long workOrderId) {
        WorkOrderDTO workOrder = mapper.detail(workOrderId);

        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "작업 정보를 찾을 수 없습니다.");
        }

        return workOrder;
    }

    private WorkOrderProcessResultDTO success(Long workOrderId, String workStatus, String message) {
        WorkOrderProcessResultDTO result = new WorkOrderProcessResultDTO();
        result.setSuccess(true);
        result.setWorkOrderId(workOrderId);
        result.setWorkStatus(workStatus);
        result.setProcessResult("SUCCESS");
        result.setMessage(message);
        return result;
    }

    private WorkOrderProcessResultDTO fail(Long workOrderId, String workStatus, String exceptionType, String message) {
        WorkOrderProcessResultDTO result = new WorkOrderProcessResultDTO();
        result.setSuccess(false);
        result.setWorkOrderId(workOrderId);
        result.setWorkStatus(workStatus);
        result.setProcessResult("FAIL");
        result.setExceptionType(exceptionType);
        result.setMessage(message);
        return result;
    }

    private String makeCompleteMessage(String workType) {
        if (workType == null) {
            return "작업이 완료되었습니다.";
        }

        if (workType.contains("상차") || "LOAD".equalsIgnoreCase(workType)) {
            return "상차 작업이 완료되었습니다.";
        }

        if (workType.contains("하차") || "UNLOAD".equalsIgnoreCase(workType)) {
            return "하차 작업이 완료되었습니다.";
        }

        return "작업이 완료되었습니다.";
    }
}
