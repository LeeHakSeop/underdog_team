package aaa.vehicle_p.service;

import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.user_p.model.UserMapper;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private DriverMapper driverMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private VehicleService service;

    @Test
    void vehicleFinalApprovalAlsoActivatesDriverAndEntryPermission() {
        VehicleDTO vehicle = new VehicleDTO();
        vehicle.setVehicleId(20L);
        vehicle.setDriverId(10L);

        DriverDTO driver = new DriverDTO();
        driver.setDriverId(10L);
        driver.setUserId(100L);

        VehicleDTO approval = new VehicleDTO();
        approval.setIsRegistered(true);

        when(vehicleMapper.detail(20L)).thenReturn(vehicle);
        when(driverMapper.detail(10L)).thenReturn(driver);
        when(vehicleMapper.updateApproval(20L, true, "정상")).thenReturn(1);

        assertEquals(1, service.updateApproval(20L, approval));

        verify(vehicleMapper).detail(20L);
        verify(driverMapper).detail(10L);
        verify(vehicleMapper).updateApproval(20L, true, "정상");
        verify(driverMapper).updateApprovalByDriverId(10L, true, true);
        verify(userMapper).updateStatus(100L, "ACTIVE");
    }

    @Test
    void tractorPlateUpdateDoesNotRequireTonnage() {
        VehicleDTO tractor = new VehicleDTO();
        tractor.setVehicleId(20L);
        tractor.setPlateNumber("서울01가1234");
        tractor.setVehicleType("TRACTOR");
        tractor.setDriverId(10L);
        tractor.setCarrierId(5L);
        tractor.setTonnage(null);
        tractor.setIsRegistered(true);
        tractor.setVehicleStatus("정상");

        when(vehicleMapper.findByPlateNumber("서울01가1234")).thenReturn(null);
        when(vehicleMapper.update(tractor)).thenReturn(1);

        assertEquals(1, service.update(tractor));
        verify(vehicleMapper).update(tractor);
    }
}
