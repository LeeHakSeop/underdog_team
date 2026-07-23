package aaa.work_order_p.service;

import aaa.container_p.service.ContainerService;
import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.model.WorkOrderMapper;
import aaa.work_order_p.model.WorkOrderProcessResultDTO;
import aaa.work_order_p.model.WorkStatusHistoryDTO;
import aaa.work_order_p.model.WorkStatusHistoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderMapper mapper;

    @Mock
    private WorkStatusHistoryMapper historyMapper;

    @Mock
    private ContainerService containerService;

    @Mock
    private DriverMapper driverMapper;

    @Mock
    private VehicleMapper vehicleMapper;

    @InjectMocks
    private WorkOrderService service;

    @Test
    void approveStoresStatusHistory() {
        WorkOrderDTO waiting = order("DISPATCH_WAITING", false, null);
        WorkOrderDTO approved = order("APPROVED", true, null);
        when(mapper.detail(1L)).thenReturn(waiting, approved);
        when(mapper.approve(1L)).thenReturn(1);

        WorkOrderDTO result = service.approve(1L);

        assertEquals("APPROVED", result.getWorkStatus());
        WorkStatusHistoryDTO history = captureHistory();
        assertEquals("DISPATCH_WAITING", history.getPrevStatus());
        assertEquals("APPROVED", history.getNewStatus());
        assertEquals("SYSTEM", history.getChangedBy());
    }

    @Test
    void rejectUsesCanceledAndStoresStatusHistory() {
        WorkOrderDTO waiting = order("DISPATCH_WAITING", false, null);
        WorkOrderDTO canceled = order("CANCELED", false, null);
        when(mapper.detail(2L)).thenReturn(waiting, canceled);
        when(mapper.reject(2L)).thenReturn(1);

        WorkOrderDTO result = service.reject(2L);

        assertEquals("CANCELED", result.getWorkStatus());
        WorkStatusHistoryDTO history = captureHistory();
        assertEquals("DISPATCH_WAITING", history.getPrevStatus());
        assertEquals("CANCELED", history.getNewStatus());
    }

    @Test
    void startRequiresGateInAndStoresHistory() {
        WorkOrderDTO gateIn = order("GATE_IN", true, 100L);
        when(mapper.detail(3L)).thenReturn(gateIn, gateIn);
        when(containerService.blockExit(100L)).thenReturn(1);
        when(mapper.updateStatus(3L, "IN_PROGRESS")).thenReturn(1);

        WorkOrderProcessResultDTO result = service.start(3L);

        assertTrue(result.isSuccess());
        assertEquals("IN_PROGRESS", result.getWorkStatus());
        WorkStatusHistoryDTO history = captureHistory();
        assertEquals("GATE_IN", history.getPrevStatus());
        assertEquals("IN_PROGRESS", history.getNewStatus());
    }

    @Test
    void startIsRejectedBeforeGateIn() {
        WorkOrderDTO approved = order("APPROVED", true, 100L);
        when(mapper.detail(4L)).thenReturn(approved);

        WorkOrderProcessResultDTO result = service.start(4L);

        assertFalse(result.isSuccess());
        assertEquals("WORK_ORDER_NOT_GATE_IN", result.getExceptionType());
        verifyNoInteractions(historyMapper);
        verifyNoInteractions(containerService);
    }

    @Test
    void completeRequiresInProgressAndStoresHistory() {
        WorkOrderDTO inProgress = order("IN_PROGRESS", true, 101L);
        when(mapper.detail(5L)).thenReturn(inProgress, inProgress);
        when(containerService.allowExit(101L)).thenReturn(1);
        when(mapper.updateStatus(5L, "COMPLETED")).thenReturn(1);

        WorkOrderProcessResultDTO result = service.complete(5L);

        assertTrue(result.isSuccess());
        assertEquals("COMPLETED", result.getWorkStatus());
        WorkStatusHistoryDTO history = captureHistory();
        assertEquals("IN_PROGRESS", history.getPrevStatus());
        assertEquals("COMPLETED", history.getNewStatus());
    }

    @Test
    void completeIsRejectedBeforeInProgress() {
        WorkOrderDTO gateIn = order("GATE_IN", true, 101L);
        when(mapper.detail(6L)).thenReturn(gateIn);

        WorkOrderProcessResultDTO result = service.complete(6L);

        assertFalse(result.isSuccess());
        assertEquals("WORK_ORDER_NOT_IN_PROGRESS", result.getExceptionType());
        verifyNoInteractions(historyMapper);
        verifyNoInteractions(containerService);
    }

    @Test
    void insertRejectsDriverAlreadyAssignedToActiveWorkOrder() {
        WorkOrderDTO request = order("DISPATCH_WAITING", false, null);
        request.setDriverId(10L);
        when(mapper.countActiveByDriverId(10L, null)).thenReturn(1);

        assertThrows(ResponseStatusException.class, () -> service.insert(request));
        verify(mapper, never()).insert(any(WorkOrderDTO.class));
    }

    @Test
    void insertRejectsTrailerAlreadyAssignedToActiveWorkOrder() {
        WorkOrderDTO request = order("DISPATCH_WAITING", false, null);
        request.setTrailerVehicleId(20L);
        when(mapper.countActiveByTrailerVehicleId(20L, null)).thenReturn(1);

        assertThrows(ResponseStatusException.class, () -> service.insert(request));
        verify(mapper, never()).insert(any(WorkOrderDTO.class));
    }

    @Test
    void insertRejectsDriverWithoutAdminFinalApproval() {
        WorkOrderDTO request = order("DISPATCH_WAITING", false, null);
        request.setDriverId(10L);

        DriverDTO driver = new DriverDTO();
        driver.setDriverId(10L);
        driver.setIsRegistered(true);
        driver.setCanEnter(false);
        driver.setUserStatus("CARRIER_APPROVED");
        when(driverMapper.detail(10L)).thenReturn(driver);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.insert(request)
        );

        assertEquals(
                "관리자 최종 승인이 완료된 기사만 작업에 배정할 수 있습니다.",
                error.getReason()
        );
        verify(mapper, never()).insert(any(WorkOrderDTO.class));
    }

    @Test
    void insertRejectsTrailerWithoutAdminFinalApproval() {
        WorkOrderDTO request = order("DISPATCH_WAITING", false, null);
        request.setDriverId(10L);
        request.setTractorVehicleId(20L);
        request.setTrailerVehicleId(30L);

        DriverDTO driver = new DriverDTO();
        driver.setDriverId(10L);
        driver.setIsRegistered(true);
        driver.setCanEnter(true);
        driver.setUserStatus("ACTIVE");

        VehicleDTO tractor = new VehicleDTO();
        tractor.setVehicleId(20L);
        tractor.setDriverId(10L);
        tractor.setVehicleType("TRACTOR");
        tractor.setIsRegistered(true);

        VehicleDTO trailer = new VehicleDTO();
        trailer.setVehicleId(30L);
        trailer.setDriverId(10L);
        trailer.setVehicleType("TRAILER");
        trailer.setIsRegistered(false);

        when(driverMapper.detail(10L)).thenReturn(driver);
        when(vehicleMapper.detail(20L)).thenReturn(tractor);
        when(vehicleMapper.detail(30L)).thenReturn(trailer);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.insert(request)
        );

        assertEquals(
                "관리자 최종 승인이 완료된 기사 배정 트레일러가 필요합니다.",
                error.getReason()
        );
        verify(mapper, never()).insert(any(WorkOrderDTO.class));
    }

    @Test
    void cancelOnlyAllowsDispatchWaitingAndStoresHistory() {
        WorkOrderDTO waiting = order("DISPATCH_WAITING", false, null);
        WorkOrderDTO canceled = order("CANCELED", false, null);
        when(mapper.detail(7L)).thenReturn(waiting, canceled);
        when(mapper.cancelDispatchWaiting(7L)).thenReturn(1);

        WorkOrderDTO result = service.cancel(7L);

        assertEquals("CANCELED", result.getWorkStatus());
        WorkStatusHistoryDTO history = captureHistory();
        assertEquals("DISPATCH_WAITING", history.getPrevStatus());
        assertEquals("CANCELED", history.getNewStatus());
    }

    @Test
    void rejectsInvalidStatusTransition() {
        WorkOrderDTO approved = order("APPROVED", true, null);
        when(mapper.detail(8L)).thenReturn(approved);

        assertThrows(ResponseStatusException.class, () -> service.changeStatus(
                8L,
                "COMPLETED",
                "SYSTEM",
                "invalid transition check",
                null
        ));
        verify(mapper, never()).updateStatus(8L, "COMPLETED");
        verifyNoInteractions(historyMapper);
    }

    private WorkStatusHistoryDTO captureHistory() {
        ArgumentCaptor<WorkStatusHistoryDTO> captor = ArgumentCaptor.forClass(WorkStatusHistoryDTO.class);
        verify(historyMapper).insert(captor.capture());
        return captor.getValue();
    }

    private WorkOrderDTO order(String status, boolean approved, Long containerId) {
        WorkOrderDTO order = new WorkOrderDTO();
        order.setWorkOrderId(1L);
        order.setWorkStatus(status);
        order.setIsApproved(approved);
        order.setContainerId(containerId);
        return order;
    }
}
