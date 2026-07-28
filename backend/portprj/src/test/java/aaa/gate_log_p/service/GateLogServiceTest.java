package aaa.gate_log_p.service;

import aaa.container_p.model.ContainerDTO;
import aaa.container_p.service.ContainerService;
import aaa.exception_log_p.model.ExceptionLogDTO;
import aaa.exception_log_p.service.ExceptionLogService;
import aaa.gate_log_p.model.GateLogDTO;
import aaa.gate_log_p.model.GateLogMapper;
import aaa.gate_log_p.model.GateProcessRequestDTO;
import aaa.gate_log_p.model.GateProcessResultDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.service.WorkOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GateLogServiceTest {

    @Mock
    private GateLogMapper mapper;

    @Mock
    private WorkOrderService workOrderService;

    @Mock
    private ContainerService containerService;

    @Mock
    private ExceptionLogService exceptionLogService;

    @InjectMocks
    private GateLogService service;

    @Test
    void approvedOrderCanEnterAndChangesToGateIn() {
        WorkOrderDTO order = order("APPROVED", true);
        when(workOrderService.detail(10L)).thenReturn(order);
        when(containerService.detail(20L)).thenReturn(container(20L, 30L, true));
        when(workOrderService.updateStatus(10L, "GATE_IN")).thenReturn(1);
        insertGateLogWithId();

        GateProcessResultDTO result = service.process(request("IN", 30L));

        assertTrue(result.getSuccess());
        assertEquals("GATE_IN", result.getWorkStatus());
        verify(workOrderService).updateStatus(10L, "GATE_IN");
        verify(containerService).blockExit(20L);
        verifyNoInteractions(exceptionLogService);
    }

    @Test
    void completedOrderCanExitAndChangesToGateOut() {
        WorkOrderDTO order = order("COMPLETED", true);
        when(workOrderService.detail(10L)).thenReturn(order);
        when(containerService.detail(20L)).thenReturn(container(20L, 30L, true));
        when(workOrderService.updateStatus(10L, "GATE_OUT")).thenReturn(1);
        insertGateLogWithId();

        GateProcessResultDTO result = service.process(request("OUT", 30L));

        assertTrue(result.getSuccess());
        assertEquals("GATE_OUT", result.getWorkStatus());
        verify(workOrderService).updateStatus(10L, "GATE_OUT");
    }

    @Test
    void entryRequiresApprovedOrder() {
        WorkOrderDTO order = order("DISPATCH_WAITING", false);
        when(workOrderService.detail(10L)).thenReturn(order);
        insertGateLogWithId();

        GateProcessResultDTO result = service.process(request("IN", 30L));

        assertFalse(result.getSuccess());
        assertEquals("WORK_ORDER_NOT_APPROVED", result.getExceptionType());
        verifyExceptionType("WORK_ORDER_NOT_APPROVED");
        verifyNoInteractions(containerService);
        verify(workOrderService).detail(10L);
    }

    @Test
    void entryRequiresTractorAndTrailerToUseTheSameWorkOrder() {
        WorkOrderDTO requestedOrder = order("APPROVED", true);
        WorkOrderDTO tractorOrder = order("APPROVED", true);
        WorkOrderDTO trailerOrder = order("APPROVED", true);
        tractorOrder.setWorkOrderId(11L);
        trailerOrder.setWorkOrderId(12L);

        when(workOrderService.detail(10L)).thenReturn(requestedOrder);
        when(workOrderService.findLatestByTractorVehicleId(1L)).thenReturn(tractorOrder);
        when(workOrderService.findLatestByTrailerVehicleId(2L)).thenReturn(trailerOrder);
        insertGateLogWithId();

        GateProcessResultDTO result = service.process(request("IN", 30L));

        assertFalse(result.getSuccess());
        assertEquals("WORK_ORDER_MISMATCH", result.getExceptionType());
        verifyExceptionType("WORK_ORDER_MISMATCH");
        verify(workOrderService, never()).updateStatus(any(Long.class), any(String.class));
        verifyNoInteractions(containerService);
    }

    @Test
    void exitRequiresCompletedOrder() {
        WorkOrderDTO order = order("GATE_IN", true);
        when(workOrderService.detail(10L)).thenReturn(order);
        insertGateLogWithId();

        GateProcessResultDTO result = service.process(request("OUT", 30L));

        assertFalse(result.getSuccess());
        assertEquals("INVALID_OUT_STATUS", result.getExceptionType());
        verifyExceptionType("INVALID_OUT_STATUS");
        verifyNoInteractions(containerService);
    }

    @Test
    void exitIsBlockedWhenContainerCannotExit() {
        WorkOrderDTO order = order("COMPLETED", true);
        when(workOrderService.detail(10L)).thenReturn(order);
        when(containerService.detail(20L)).thenReturn(container(20L, 30L, false));
        insertGateLogWithId();

        GateProcessResultDTO result = service.process(request("OUT", 30L));

        assertFalse(result.getSuccess());
        assertEquals("CONTAINER_EXIT_NOT_ALLOWED", result.getExceptionType());
        verifyExceptionType("CONTAINER_EXIT_NOT_ALLOWED");
        verify(workOrderService, never()).updateStatus(any(Long.class), any(String.class));
        verify(containerService, never()).blockExit(any(Long.class));
    }

    private void insertGateLogWithId() {
        doAnswer(invocation -> {
            GateLogDTO gateLog = invocation.getArgument(0);
            gateLog.setGateLogId(99L);
            return 1;
        }).when(mapper).insert(any(GateLogDTO.class));
    }

    private void verifyExceptionType(String expected) {
        ArgumentCaptor<ExceptionLogDTO> captor = ArgumentCaptor.forClass(ExceptionLogDTO.class);
        verify(exceptionLogService).insert(captor.capture());
        assertEquals(expected, captor.getValue().getExceptionType());
    }

    private GateProcessRequestDTO request(String inOutType, Long sectorId) {
        GateProcessRequestDTO request = new GateProcessRequestDTO();
        request.setTractorVehicleId(1L);
        request.setTrailerVehicleId(2L);
        request.setWorkOrderId(10L);
        request.setContainerId(20L);
        request.setSectorId(sectorId);
        request.setInOutType(inOutType);
        return request;
    }

    private WorkOrderDTO order(String status, boolean approved) {
        WorkOrderDTO order = new WorkOrderDTO();
        order.setWorkOrderId(10L);
        order.setWorkStatus(status);
        order.setIsApproved(approved);
        order.setVehicleId(1L);
        order.setTractorVehicleId(1L);
        order.setTrailerVehicleId(2L);
        order.setContainerId(20L);
        return order;
    }

    private ContainerDTO container(Long containerId, Long sectorId, boolean canExit) {
        ContainerDTO container = new ContainerDTO();
        container.setContainerId(containerId);
        container.setSectorId(sectorId);
        container.setCanExit(canExit);
        return container;
    }
}
