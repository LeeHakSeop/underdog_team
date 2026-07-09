package aaa.plate_recognition_p.model;

import aaa.carrier_p.model.CarrierDTO;
import aaa.container_p.model.ContainerDTO;
import aaa.driver_p.model.DriverDTO;
import aaa.gate_log_p.model.GateLogDTO;
import aaa.vehicle_p.model.TractorVehicleInfoDTO;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.work_order_p.model.TrailerWorkInfoDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.yard_sector_p.model.YardSectorDTO;
import lombok.Data;

@Data
public class PlateRecognitionResultDTO {
    private FastApiPlateResponseDTO aiResult;
    private PlateRecognitionDTO plateRecognition;
    private GateLogDTO gateLog;
    private VehicleDTO vehicle;
    private TractorVehicleInfoDTO tractorVehicleInfo;
    private TrailerWorkInfoDTO trailerWorkInfo;
    private DriverDTO driver;
    private CarrierDTO carrier;
    private WorkOrderDTO workOrder;
    private ContainerDTO container;
    private YardSectorDTO yardSector;
    private Boolean matched;
    private Boolean needReview;
    private String message;
}
