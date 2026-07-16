package aaa.driver_p.service;

import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.user_p.model.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverMapper driverMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private DriverService service;

    @Test
    void withdrawKeepsDriverAndWorkOrderReferences() {
        DriverDTO driver = new DriverDTO();
        driver.setDriverId(10L);
        driver.setUserId(100L);
        driver.setIsRegistered(true);
        driver.setUserStatus("ACTIVE");

        when(driverMapper.detail(10L)).thenReturn(driver);
        when(driverMapper.updateApprovalByDriverId(10L, false, false)).thenReturn(1);
        when(userMapper.updateStatus(100L, "WITHDRAWN")).thenReturn(1);

        assertEquals(1, service.withdraw(10L));

        verify(driverMapper).updateApprovalByDriverId(10L, false, false);
        verify(userMapper).updateStatus(100L, "WITHDRAWN");
    }

    @Test
    void reactivateReturnsDriverToApprovalFlowWithoutDeletingHistory() {
        DriverDTO driver = new DriverDTO();
        driver.setDriverId(10L);
        driver.setUserId(100L);
        driver.setIsRegistered(true);
        driver.setUserStatus("WITHDRAWN");

        when(driverMapper.detail(10L)).thenReturn(driver);
        when(driverMapper.updateApprovalByDriverId(10L, true, false)).thenReturn(1);
        when(userMapper.updateStatus(100L, "CARRIER_APPROVED")).thenReturn(1);

        assertEquals(1, service.reactivate(10L));

        verify(driverMapper).updateApprovalByDriverId(10L, true, false);
        verify(userMapper).updateStatus(100L, "CARRIER_APPROVED");
    }
}
