package aaa.carrier_p.service;

import aaa.carrier_p.model.CarrierDTO;
import aaa.carrier_p.model.CarrierMapper;
import aaa.user_p.model.UserMapper;
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
class CarrierServiceTest {

    @Mock
    private CarrierMapper carrierMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CarrierService service;

    @Test
    void deleteDetachesOperationalReferencesAndDeletesCarrierUsers() {
        CarrierDTO carrier = new CarrierDTO();
        carrier.setCarrierId(5L);
        carrier.setUserId(50L);

        when(carrierMapper.detail(5L)).thenReturn(carrier);
        when(carrierMapper.delete(5L)).thenReturn(1);

        assertEquals(1, service.delete(5L));

        verify(carrierMapper).clearWorkOrderDriverReferences(5L);
        verify(carrierMapper).detachVehicles(5L);
        verify(carrierMapper).detachDrivers(5L);
        verify(carrierMapper).delete(5L);
        verify(userMapper).delete(50L);
        verifyNoMoreInteractions(carrierMapper, userMapper);
    }
}
