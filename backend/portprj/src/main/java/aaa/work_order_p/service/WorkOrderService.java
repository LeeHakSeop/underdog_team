package aaa.work_order_p.service;

import aaa.container_p.service.ContainerService;
import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import aaa.work_order_p.model.TrailerWorkInfoDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.model.WorkOrderMapper;
import aaa.work_order_p.model.WorkOrderProcessResultDTO;
import aaa.work_order_p.model.WorkStatusHistoryDTO;
import aaa.work_order_p.model.WorkStatusHistoryMapper;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class WorkOrderService {

    private static final String STATUS_DISPATCH_WAITING = "DISPATCH_WAITING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_GATE_IN = "GATE_IN";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_GATE_OUT = "GATE_OUT";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String SYSTEM_ACTOR = "SYSTEM";

    @Resource
    WorkOrderMapper mapper;

    @Resource
    WorkStatusHistoryMapper historyMapper;

    @Resource
    ContainerService containerService;

    @Resource
    DriverMapper driverMapper;

    @Resource
    VehicleMapper vehicleMapper;

    public List<WorkOrderDTO> list() {
        return mapper.list();
    }

    public WorkOrderDTO detail(Long workOrderId) {
        return mapper.detail(workOrderId);
    }

    public List<WorkStatusHistoryDTO> history() {
        return historyMapper.list();
    }

    public List<WorkStatusHistoryDTO> historyByDriverUserId(Long userId) {
        return historyMapper.listByDriverUserId(userId);
    }

    public int insert(WorkOrderDTO dto) {
        if (dto.getWorkStatus() == null || dto.getWorkStatus().isBlank()) {
            dto.setWorkStatus(STATUS_DISPATCH_WAITING);
        }

        if (dto.getIsApproved() == null) {
            dto.setIsApproved(false);
        }

        validateDispatchAssignment(dto, null);

        return mapper.insert(dto);
    }

    @Transactional
    public WorkOrderDTO update(WorkOrderDTO dto) {
        WorkOrderDTO current = getWorkOrder(dto.getWorkOrderId());

        if (!isRequestWaiting(current.getWorkStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "승인 대기 중인 작업 지시만 수정할 수 있습니다."
            );
        }

        validateDispatchAssignment(dto, dto.getWorkOrderId());
        int updated = mapper.updateDispatchWaiting(dto);
        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "작업 지시가 이미 변경되어 수정할 수 없습니다."
            );
        }

        return mapper.detail(dto.getWorkOrderId());
    }

    @Transactional
    public WorkOrderDTO cancel(Long workOrderId) {
        WorkOrderDTO current = getWorkOrder(workOrderId);

        if (!isRequestWaiting(current.getWorkStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "승인 대기 중인 작업 지시만 삭제할 수 있습니다."
            );
        }

        int updated = mapper.cancelDispatchWaiting(workOrderId);
        if (updated > 0) {
            saveHistory(current, STATUS_CANCELED, SYSTEM_ACTOR, "운송사 작업 지시 삭제", null);
        }

        return mapper.detail(workOrderId);
    }

    private void validateDispatchAssignment(WorkOrderDTO dto, Long workOrderId) {
        if (dto.getDriverId() != null && mapper.countActiveByDriverId(dto.getDriverId(), workOrderId) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 다른 작업에 배정된 기사입니다."
            );
        }

        if (dto.getDriverId() != null) {
            DriverDTO driver = driverMapper.detail(dto.getDriverId());
            if (driver == null
                    || !Boolean.TRUE.equals(driver.getIsRegistered())
                    || !Boolean.TRUE.equals(driver.getCanEnter())
                    || !"ACTIVE".equals(driver.getUserStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "관리자 최종 승인이 완료된 기사만 작업에 배정할 수 있습니다."
                );
            }

            VehicleDTO tractor = dto.getTractorVehicleId() == null
                    ? null
                    : vehicleMapper.detail(dto.getTractorVehicleId());
            if (tractor == null
                    || !Objects.equals(tractor.getDriverId(), dto.getDriverId())
                    || !Boolean.TRUE.equals(tractor.getIsRegistered())
                    || !isTractor(tractor.getVehicleType())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "관리자 최종 승인이 완료된 기사 소유 트랙터가 필요합니다."
                );
            }

            VehicleDTO trailer = dto.getTrailerVehicleId() == null
                    ? null
                    : vehicleMapper.detail(dto.getTrailerVehicleId());
            if (trailer == null
                    || !Objects.equals(trailer.getDriverId(), dto.getDriverId())
                    || !Boolean.TRUE.equals(trailer.getIsRegistered())
                    || !isTrailer(trailer.getVehicleType())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "관리자 최종 승인이 완료된 기사 배정 트레일러가 필요합니다."
                );
            }
        }

        if (dto.getTrailerVehicleId() != null
                && mapper.countActiveByTrailerVehicleId(dto.getTrailerVehicleId(), workOrderId) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 다른 작업에 배정된 트레일러입니다."
            );
        }
    }

    private boolean isTractor(String vehicleType) {
        return "TRACTOR".equalsIgnoreCase(vehicleType)
                || "트랙터".equals(vehicleType);
    }

    private boolean isTrailer(String vehicleType) {
        return "TRAILER".equalsIgnoreCase(vehicleType)
                || "트레일러".equals(vehicleType);
    }

    public TrailerWorkInfoDTO findTrailerWorkInfo(Long vehicleId) {
        return mapper.findTrailerWorkInfoByVehicleId(vehicleId);
    }

    public WorkOrderDTO findLatestByTractorVehicleId(Long vehicleId) {
        return mapper.findLatestByTractorVehicleId(vehicleId);
    }

    public WorkOrderDTO findLatestByTrailerVehicleId(Long vehicleId) {
        return mapper.findLatestByTrailerVehicleId(vehicleId);
    }

    @Transactional
    public int updateStatus(Long workOrderId, String workStatus) {
        return changeStatus(workOrderId, workStatus, SYSTEM_ACTOR, "작업 상태 변경", null);
    }

    @Transactional
    public WorkOrderDTO approve(Long workOrderId) {
        WorkOrderDTO workOrder = getWorkOrder(workOrderId);

        if (!isRequestWaiting(workOrder.getWorkStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인 대기 작업만 승인할 수 있습니다.");
        }

        int updated = mapper.approve(workOrderId);
        if (updated > 0) {
            saveHistory(workOrder, STATUS_APPROVED, SYSTEM_ACTOR, "관리자 승인", null);
        }
        return mapper.detail(workOrderId);
    }

    @Transactional
    public WorkOrderDTO reject(Long workOrderId) {
        WorkOrderDTO workOrder = getWorkOrder(workOrderId);

        if (!isRequestWaiting(workOrder.getWorkStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인 대기 작업만 반려할 수 있습니다.");
        }

        int updated = mapper.reject(workOrderId);
        if (updated > 0) {
            saveHistory(workOrder, STATUS_CANCELED, SYSTEM_ACTOR, "관리자 반려", null);
        }
        return mapper.detail(workOrderId);
    }

    private boolean isRequestWaiting(String workStatus) {
        return STATUS_DISPATCH_WAITING.equals(workStatus);
    }

    @Transactional
    public int changeStatus(Long workOrderId, String newStatus, String changedBy, String reason, String remark) {
        WorkOrderDTO current = detail(workOrderId);
        if (current == null || Objects.equals(current.getWorkStatus(), newStatus)) {
            return 0;
        }

        if (!isAllowedTransition(current.getWorkStatus(), newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid work-order status transition: " + current.getWorkStatus() + " -> " + newStatus
            );
        }

        int updated = mapper.updateStatus(workOrderId, newStatus);
        if (updated > 0) {
            saveHistory(current, newStatus, changedBy, reason, remark);
        }
        return updated;
    }

    private boolean isAllowedTransition(String currentStatus, String newStatus) {
        if (STATUS_DISPATCH_WAITING.equals(currentStatus)) {
            return STATUS_APPROVED.equals(newStatus) || STATUS_CANCELED.equals(newStatus);
        }
        if (STATUS_APPROVED.equals(currentStatus)) {
            return STATUS_GATE_IN.equals(newStatus) || STATUS_CANCELED.equals(newStatus);
        }
        if (STATUS_GATE_IN.equals(currentStatus)) {
            return STATUS_IN_PROGRESS.equals(newStatus) || STATUS_CANCELED.equals(newStatus);
        }
        if (STATUS_IN_PROGRESS.equals(currentStatus)) {
            return STATUS_COMPLETED.equals(newStatus) || STATUS_CANCELED.equals(newStatus);
        }
        return STATUS_COMPLETED.equals(currentStatus) && STATUS_GATE_OUT.equals(newStatus);
    }

    private void saveHistory(WorkOrderDTO current, String newStatus, String changedBy, String reason, String remark) {
        WorkStatusHistoryDTO history = new WorkStatusHistoryDTO();
        history.setWorkOrderId(current.getWorkOrderId());
        history.setPrevStatus(current.getWorkStatus());
        history.setNewStatus(newStatus);
        history.setChangedTime(LocalDateTime.now());
        history.setChangedBy(changedBy == null || changedBy.isBlank() ? SYSTEM_ACTOR : changedBy);
        history.setReason(reason);
        history.setRemark(remark);
        historyMapper.insert(history);
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
