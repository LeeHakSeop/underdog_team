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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        vehicle.setVehicleType("TRAILER");
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
    void trailerAssignmentCreatesTrailerSubtype() {
        VehicleDTO trailer = new VehicleDTO();
        trailer.setPlateNumber("부산80바9999");
        trailer.setDriverId(10L);
        trailer.setCarrierId(5L);
        trailer.setTonnage("25톤");

        DriverDTO driver = new DriverDTO();
        driver.setDriverId(10L);
        driver.setCarrierId(5L);
        driver.setUserId(100L);
        driver.setIsRegistered(true);
        driver.setCanEnter(false);

        when(vehicleMapper.findByPlateNumber("부산80바9999")).thenReturn(null);
        when(driverMapper.detail(10L)).thenReturn(driver);
        when(vehicleMapper.insert(trailer)).thenAnswer(invocation -> {
            trailer.setVehicleId(20L);
            return 1;
        });

        assertEquals(1, service.insert(trailer));

        verify(vehicleMapper).insert(trailer);
        verify(vehicleMapper).insertTrailerSubtype(20L);
    }

    @Test
    void tractorFinalApprovalAlsoActivatesDriverAndEntryPermission() {
        VehicleDTO vehicle = new VehicleDTO();
        vehicle.setVehicleId(21L);
        vehicle.setVehicleType("TRACTOR");
        vehicle.setDriverId(11L);

        DriverDTO driver = new DriverDTO();
        driver.setDriverId(11L);
        driver.setUserId(101L);

        VehicleDTO approval = new VehicleDTO();
        approval.setIsRegistered(true);

        when(vehicleMapper.detail(21L)).thenReturn(vehicle);
        when(driverMapper.detail(11L)).thenReturn(driver);
        when(vehicleMapper.updateApproval(21L, true, "정상")).thenReturn(1);

        assertEquals(1, service.updateApproval(21L, approval));

        verify(vehicleMapper).detail(21L);
        verify(driverMapper).detail(11L);
        verify(vehicleMapper).updateApproval(21L, true, "정상");
        verify(driverMapper).updateApprovalByDriverId(11L, true, true);
        verify(userMapper).updateStatus(101L, "ACTIVE");
    }

    @Test
    void finalApprovalRejectsUnsupportedVehicleType() {
        VehicleDTO vehicle = new VehicleDTO();
        vehicle.setVehicleId(22L);
        vehicle.setVehicleType("FORKLIFT");

        VehicleDTO approval = new VehicleDTO();
        approval.setIsRegistered(true);

        when(vehicleMapper.detail(22L)).thenReturn(vehicle);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateApproval(22L, approval)
        );

        assertEquals("관리자 최종 승인은 배정된 트랙터 또는 트레일러만 처리할 수 있습니다.", error.getMessage());
        verify(vehicleMapper).detail(22L);
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
