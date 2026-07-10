package aaa.work_order_p.service;

import aaa.work_order_p.model.TrailerWorkInfoDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.model.WorkOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WorkOrderService {

    @Resource
    WorkOrderMapper mapper;

    public List<WorkOrderDTO> list() {
        return mapper.list();
    }

    public WorkOrderDTO detail(Long workOrderId) {
        return mapper.detail(workOrderId);
    }

    public int insert(WorkOrderDTO dto) {
        if (dto.getWorkStatus() == null || dto.getWorkStatus().isBlank()) {
            dto.setWorkStatus("DISPATCH_WAITING");
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

        if (!"DISPATCH_WAITING".equals(workOrder.getWorkStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인 대기 작업만 승인할 수 있습니다.");
        }

        mapper.approve(workOrderId);
        return mapper.detail(workOrderId);
    }

    public WorkOrderDTO start(Long workOrderId) {
        WorkOrderDTO workOrder = getWorkOrder(workOrderId);

        if (!Boolean.TRUE.equals(workOrder.getIsApproved())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인된 작업만 시작할 수 있습니다.");
        }

        if (!"GATE_IN".equals(workOrder.getWorkStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입차 완료 작업만 시작할 수 있습니다.");
        }

        mapper.updateStatus(workOrderId, "IN_PROGRESS");
        return mapper.detail(workOrderId);
    }

    public WorkOrderDTO complete(Long workOrderId) {
        WorkOrderDTO workOrder = getWorkOrder(workOrderId);

        if (!"IN_PROGRESS".equals(workOrder.getWorkStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 중인 작업만 완료할 수 있습니다.");
        }

        mapper.updateStatus(workOrderId, "COMPLETED");
        return mapper.detail(workOrderId);
    }

    private WorkOrderDTO getWorkOrder(Long workOrderId) {
        WorkOrderDTO workOrder = mapper.detail(workOrderId);

        if (workOrder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "작업정보를 찾을 수 없습니다.");
        }

        return workOrder;
    }
}
