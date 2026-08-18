package aaa.yard_map_p.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class YardMapGateDTO {
    private String gateNumber;
    private String gateName;
    private String direction;
    private Long latestGateLogId;
    private Long latestVehicleId;
    private Long latestTractorVehicleId;
    private Long latestTrailerVehicleId;
    private LocalDateTime latestEntryTime;
    private LocalDateTime latestExitTime;
    private String latestProcessResult;
    private Boolean managerCheck;
    private Long todayInCount;
    private Long todayOutCount;
}
