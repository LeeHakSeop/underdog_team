<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useLogisticsData } from '@/composables/useLogisticsData'
import { useWorkOrderStore } from '@/stores/adminStore/workOrderStore'

const workOrderStore = useWorkOrderStore()
const processingId = ref(null)
const processMessage = ref('')
let refreshTimer = null

const { workOrders, getCarrierName, getContainer, getContainerNumber, getDriverName, getPlateNumber, getSectorByContainerId } = useLogisticsData()
const carrierRequests = computed(() => {
  return workOrders.value.filter((order) => order.work_status === 'DISPATCH_WAITING')
})

const processingTasks = computed(() => {
  return workOrders.value.filter((order) => order.work_status !== 'DISPATCH_WAITING')
})

const processWorkOrder = async (order, action) => {
  processingId.value = order.work_order_id
  processMessage.value = ''

  try {
    if (action === 'approve') {
      await workOrderStore.approve(order.work_order_id)
      processMessage.value = '작업 승인이 완료되었습니다.'
    }

    if (action === 'start') {
      await workOrderStore.start(order.work_order_id)
      processMessage.value = '작업 시작 처리가 완료되었습니다.'
    }

    if (action === 'complete') {
      await workOrderStore.complete(order.work_order_id)
      processMessage.value = '작업 완료 처리가 완료되었습니다.'
    }
  } catch (error) {
    processMessage.value = error.message || '작업 처리에 실패했습니다.'
  } finally {
    processingId.value = null
  }
}

const getStatusText = (workStatus) => {
  if (workStatus === 'DISPATCH_WAITING') return '승인 대기'
  if (workStatus === 'APPROVED') return '입차 대기'
  if (workStatus === 'GATE_IN') return '입차 완료'
  if (workStatus === 'IN_PROGRESS') return '작업 진행 중'
  if (workStatus === 'COMPLETED') return '출차 대기'
  if (workStatus === 'GATE_OUT') return '출차 완료'
  return workStatus || '-'
}

onMounted(() => {
  refreshTimer = setInterval(() => {
    if (!workOrderStore.loading && processingId.value === null) {
      workOrderStore.loadWorkOrders().catch(() => {})
    }
  }, 5000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>운송사 작업 요청 승인관리</h2>
        <span class="status-pill amber">배차 대기 {{ carrierRequests.length }}건</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업번호</th>
              <th>운송사</th>
              <th>차량번호</th>
              <th>기사</th>
              <th>컨테이너</th>
              <th>작업 유형</th>
              <th>예약</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in carrierRequests" :key="order.work_order_id">
              <td>{{ order.work_order_id }}</td>
              <td>{{ getCarrierName(order.carrier_id) }}</td>
              <td>{{ getPlateNumber(order.vehicle_id) }}</td>
              <td>{{ getDriverName(order.driver_id) }}</td>
              <td>{{ getContainerNumber(order.container_id) }}</td>
              <td>{{ order.work_type }}</td>
              <td>{{ order.reserved_time }}</td>
              <td><span class="status-pill amber">{{ getStatusText(order.work_status) }}</span></td>
              <td>
                <button
                  class="ghost-button"
                  type="button"
                  :disabled="processingId === order.work_order_id"
                  @click="processWorkOrder(order, 'approve')"
                >
                  {{ processingId === order.work_order_id ? '처리 중' : '작업 승인' }}
                </button>
              </td>
            </tr>
            <tr v-if="carrierRequests.length === 0">
              <td colspan="9">배차 대기 작업이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <p v-if="processMessage" class="process-message">{{ processMessage }}</p>

    <section class="panel">
      <div class="section-title">
        <h2>기사 승낙 작업 관리</h2>
        <span class="status-pill green">작업 처리 {{ processingTasks.length }}건</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업번호</th>
              <th>화물 종류</th>
              <th>컨테이너</th>
              <th>차량번호</th>
              <th>기사</th>
              <th>자동 배정 섹터</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in processingTasks" :key="order.work_order_id">
              <td>{{ order.work_order_id }}</td>
              <td>{{ getContainer(order.container_id)?.shipping_line || '-' }}</td>
              <td>{{ getContainerNumber(order.container_id) }}</td>
              <td>{{ getPlateNumber(order.vehicle_id) }}</td>
              <td>{{ getDriverName(order.driver_id) }}</td>
              <td>{{ getSectorByContainerId(order.container_id)?.sector_name || '-' }}</td>
              <td><span class="status-pill green">{{ getStatusText(order.work_status) }}</span></td>
              <td>
                <button
                  v-if="order.work_status === 'GATE_IN'"
                  class="primary-button"
                  type="button"
                  :disabled="processingId === order.work_order_id"
                  @click="processWorkOrder(order, 'start')"
                >
                  {{ processingId === order.work_order_id ? '처리 중' : '작업 시작' }}
                </button>
                <button
                  v-else-if="order.work_status === 'IN_PROGRESS'"
                  class="primary-button"
                  type="button"
                  :disabled="processingId === order.work_order_id"
                  @click="processWorkOrder(order, 'complete')"
                >
                  {{ processingId === order.work_order_id ? '처리 중' : '작업 완료' }}
                </button>
                <span v-else>{{ getStatusText(order.work_status) }}</span>
              </td>
            </tr>
            <tr v-if="processingTasks.length === 0">
              <td colspan="8">처리 중인 작업이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.process-message {
  margin: 0;
  padding: 10px 12px;
  color: var(--ink-700);
  background: #f8fbfe;
  border: 1px solid var(--line);
  font-weight: 800;
}
</style>
