package aaa.work_order_p.service;

import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import aaa.work_order_p.model.TrailerWorkInfoDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.model.WorkOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class WorkOrderService {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "DISPATCH_WAITING", "APPROVED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
    );

    @Resource WorkOrderMapper mapper;
    @Resource DriverMapper driverMapper;
    @Resource VehicleMapper vehicleMapper;

    public List<WorkOrderDTO> list() { return mapper.list(); }
    public List<WorkOrderDTO> carrierWorkOrders(Long userId) { return mapper.findByCarrierUserId(userId); }

    @Transactional
    public int insert(WorkOrderDTO dto) {
        if (dto == null || dto.getDriverId() == null || dto.getVehicleId() == null) {
            throw new IllegalArgumentException("기사와 트레일러를 선택해야 합니다.");
        }
        DriverDTO driver = driverMapper.detail(dto.getDriverId());
        VehicleDTO trailer = vehicleMapper.detail(dto.getVehicleId());
        if (driver == null) throw new IllegalArgumentException("기사 정보를 찾을 수 없습니다.");
        if (!Boolean.TRUE.equals(driver.getIsRegistered())) throw new IllegalArgumentException("운송사 승인이 완료된 기사만 배정할 수 있습니다.");
        if (trailer == null || !"TRAILER".equalsIgnoreCase(trailer.getVehicleType())) {
            throw new IllegalArgumentException("트레일러 차량을 선택해야 합니다.");
        }
        if (!Boolean.TRUE.equals(trailer.getIsRegistered())) throw new IllegalArgumentException("승인된 트레일러만 배정할 수 있습니다.");
        if (driver.getCarrierId() == null || !driver.getCarrierId().equals(trailer.getCarrierId())) {
            throw new IllegalArgumentException("기사와 트레일러의 운송사가 일치하지 않습니다.");
        }
        if (mapper.countActiveByVehicleId(dto.getVehicleId()) > 0) {
            throw new IllegalStateException("해당 트레일러에는 이미 진행 중인 작업이 있습니다.");
        }
        dto.setTrailerVehicleId(dto.getVehicleId());
        dto.setContainerId(null);
        dto.setWorkStatus("DISPATCH_WAITING");
        dto.setIsApproved(false);
        return mapper.insert(dto);
    }

    public TrailerWorkInfoDTO findTrailerWorkInfo(Long vehicleId) { return null; }

    @Transactional
    public WorkOrderDTO approve(Long workOrderId) {
        WorkOrderDTO order = requireOrder(workOrderId);
        if (!Boolean.TRUE.equals(order.getIsApproved())) mapper.approve(workOrderId);
        return mapper.detail(workOrderId);
    }

    @Transactional
    public WorkOrderDTO updateStatus(Long workOrderId, String workStatus) {
        if (!ALLOWED_STATUSES.contains(workStatus)) throw new IllegalArgumentException("허용되지 않은 작업 상태입니다.");
        WorkOrderDTO order = requireOrder(workOrderId);
        if (!Boolean.TRUE.equals(order.getIsApproved()) && !"CANCELLED".equals(workStatus)) {
            throw new IllegalStateException("관리자 승인 후 작업 상태를 변경할 수 있습니다.");
        }
        if ("IN_PROGRESS".equals(workStatus) && !"APPROVED".equals(order.getWorkStatus())) {
            throw new IllegalStateException("승인 완료 상태에서만 작업을 시작할 수 있습니다.");
        }
        if ("COMPLETED".equals(workStatus) && !"IN_PROGRESS".equals(order.getWorkStatus())) {
            throw new IllegalStateException("진행 중인 작업만 완료할 수 있습니다.");
        }
        mapper.updateStatus(workOrderId, workStatus);
        if ("COMPLETED".equals(workStatus) || "CANCELLED".equals(workStatus)) {
            mapper.resetTrailer(order.getVehicleId());
        }
        return mapper.detail(workOrderId);
    }

    private WorkOrderDTO requireOrder(Long workOrderId) {
        WorkOrderDTO order = mapper.detail(workOrderId);
        if (order == null) throw new IllegalArgumentException("작업 오더를 찾을 수 없습니다.");
        return order;
    }
}
