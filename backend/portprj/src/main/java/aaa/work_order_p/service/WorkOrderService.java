package aaa.work_order_p.service;

import aaa.work_order_p.model.TrailerWorkInfoDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.model.WorkOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkOrderService {

    @Resource
    WorkOrderMapper mapper;

    public List<WorkOrderDTO> list() {
        return mapper.list();
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
}
