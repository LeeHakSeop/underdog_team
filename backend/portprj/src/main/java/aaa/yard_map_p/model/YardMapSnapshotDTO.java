package aaa.yard_map_p.model;

import lombok.Data;

import java.util.List;

@Data
public class YardMapSnapshotDTO {
    private List<YardMapSectorDTO> sectors;
    private List<YardMapGateDTO> gates;
    private List<YardMapVehicleDTO> vehicles;
}
