package aaa.yard_congestion_p.model;

import lombok.Data;

@Data
public class YardCongestionSummaryDTO {
    private Integer totalSectorCount;
    private Integer normalSectorCount;
    private Integer warningSectorCount;
    private Integer dangerSectorCount;
    private Double averageUsageRate;
    private Integer totalCapacity;
    private Long totalContainerCount;
    private Integer totalWaitingVehicleCount;
    private Long totalWorkOrderCount;
}
