package aaa.dashboard_p.model;

import lombok.Data;

@Data
public class DashboardSummaryDTO {
    private Integer totalVehicles;
    private Integer pendingUsers;
    private Integer pendingCarriers;
    private Integer pendingDrivers;
    private Integer todayGateIn;
    private Integer todayGateOut;
    private Integer recognitionTotal;
    private Integer recognitionSuccess;
    private Integer recognitionFail;
    private Integer exceptionOpen;
    private Integer workTotal;
    private Integer workReady;
    private Integer workInProgress;
    private Integer workDone;
}
