package aaa.yard_sector_p.service;

import aaa.yard_sector_p.model.YardSectorDTO;
import aaa.yard_sector_p.model.YardSectorMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YardSectorServiceTest {

    @Mock
    private YardSectorMapper mapper;

    @InjectMocks
    private YardSectorService service;

    @Test
    void updatesCapacityAndReturnsSector() {
        YardSectorDTO sector = sector(1L, 40);
        YardSectorDTO updated = sector(1L, 60);
        when(mapper.detail(1L)).thenReturn(sector, updated);
        when(mapper.updateCapacity(1L, 60)).thenReturn(1);

        YardSectorDTO result = service.updateCapacity(1L, 60);

        assertEquals(60, result.getCapacity());
        verify(mapper).updateCapacity(1L, 60);
    }

    @Test
    void rejectsInvalidCapacity() {
        when(mapper.detail(1L)).thenReturn(sector(1L, 40));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.updateCapacity(1L, 0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rejectsUnknownSector() {
        when(mapper.detail(999L)).thenReturn(null);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.updateCapacity(999L, 40)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    private YardSectorDTO sector(Long sectorId, Integer capacity) {
        YardSectorDTO sector = new YardSectorDTO();
        sector.setSectorId(sectorId);
        sector.setCapacity(capacity);
        return sector;
    }
}
