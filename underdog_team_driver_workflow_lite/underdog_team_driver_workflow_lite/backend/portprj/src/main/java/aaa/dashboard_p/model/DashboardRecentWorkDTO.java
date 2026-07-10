package aaa.dashboard_p.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DashboardRecentWorkDTO {
    private Long workOrderId;
    private String workType;
    private String workStatus;
    private LocalDateTime reservedTime;
    private String plateNumber;
    private String driverName;
    private String carrierName;
    private String containerNumber;
    private String sectorName;
}
