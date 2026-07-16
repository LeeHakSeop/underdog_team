package aaa.driver_p.service;

import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.user_p.model.UserMapper;
import aaa.vehicle_p.model.VehicleMapper;
import aaa.work_order_p.model.WorkOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverMapper driverMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private WorkOrderMapper workOrderMapper;

    @InjectMocks
    private DriverService service;

    @Test
    void deleteKeepsHistoryAndRemovesDriverReferences() {
        DriverDTO driver = new DriverDTO();
        driver.setDriverId(10L);
        driver.setUserId(100L);

        when(driverMapper.detail(10L)).thenReturn(driver);
        when(driverMapper.delete(10L)).thenReturn(1);

        assertEquals(1, service.delete(10L));

        verify(workOrderMapper).clearDriverReference(10L);
        verify(vehicleMapper).clearDriverReference(10L);
        verify(vehicleMapper).clearUserReference(100L);
        verify(driverMapper).delete(10L);
        verify(userMapper).delete(100L);
        verifyNoMoreInteractions(driverMapper, userMapper, vehicleMapper, workOrderMapper);
    }
}
