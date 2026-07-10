package aaa.dashboard_p.service;

import aaa.dashboard_p.model.DashboardDTO;
import aaa.dashboard_p.model.DashboardMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    @Resource
    DashboardMapper mapper;

    public DashboardDTO admin() {
        DashboardDTO dto = new DashboardDTO();
        dto.setSummary(mapper.summary());
        dto.setWorkStatusList(mapper.workStatusList());
        dto.setRecentWorkOrders(mapper.recentWorkOrders());
        dto.setSectorList(mapper.sectorList());
        return dto;
    }
}
