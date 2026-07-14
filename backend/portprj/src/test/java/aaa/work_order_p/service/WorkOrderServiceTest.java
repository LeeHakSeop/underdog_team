package aaa.work_order_p.service;

import aaa.container_p.service.ContainerService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderMapper mapper;

    @Mock
    private WorkStatusHistoryMapper historyMapper;

    @Mock
    private ContainerService containerService;

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
