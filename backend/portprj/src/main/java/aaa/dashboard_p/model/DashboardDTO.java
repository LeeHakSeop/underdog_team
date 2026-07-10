package aaa.dashboard_p.model;

import lombok.Data;

import java.util.List;

@Data
public class DashboardDTO {
    private DashboardSummaryDTO summary;
    private List<DashboardWorkStatusDTO> workStatusList;
    private List<DashboardRecentWorkDTO> recentWorkOrders;
    private List<DashboardSectorDTO> sectorList;
}
