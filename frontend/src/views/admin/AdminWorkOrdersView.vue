<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useCarrierStore } from '@/stores/carrierStore'
import { useContainerStore } from '@/stores/adminStore/containerStore'
import { useDriverStore } from '@/stores/driverStore'
import { useVehicleStore } from '@/stores/vehicleStore'
import { useWorkOrderStore } from '@/stores/adminStore/workOrderStore'
import { fetchYardSectors } from '@/api/adminApi/yardSectorApi'

const workOrderStore = useWorkOrderStore()
const carrierStore = useCarrierStore()
const containerStore = useContainerStore()
const driverStore = useDriverStore()
const vehicleStore = useVehicleStore()
const processingId = ref(null)
const processMessage = ref('')
<<<<<<< HEAD
const requestQuery = ref('')
const requestPage = ref(1)
const requestPageSize = ref(10)
const taskQuery = ref('')
const taskStatus = ref('')
const taskPage = ref(1)
const taskPageSize = ref(10)
=======
const containerQuery = ref('')
const containerMessage = ref('')
const editingContainerId = ref(null)
const containerForm = ref({})
const yardSectors = ref([])
>>>>>>> origin/hakseop
let refreshTimer = null

const pageSizeOptions = [10, 20, 50]
const taskStatusOptions = [
  { value: '', label: '전체 상태' },
  { value: 'APPROVED', label: '입차 대기' },
  { value: 'GATE_IN', label: '입차 완료' },
  { value: 'IN_PROGRESS', label: '작업 진행 중' },
  { value: 'COMPLETED', label: '출차 대기' },
  { value: 'GATE_OUT', label: '출차 완료' },
  { value: 'CANCELED', label: '반려' },
]

const getId = (row, key) => row?.[key] ?? row?.[key.replace(/[A-Z]/g, (match) => `_${match.toLowerCase()}`)]
const getValue = (row, camelKey, snakeKey) => row?.[camelKey] ?? row?.[snakeKey] ?? ''

const getVehicle = (vehicleId) => {
  return vehicleStore.vehicles.find((item) => getId(item, 'vehicleId') === vehicleId) || null
}

const getCarrierName = (order) => {
  const vehicle = getVehicle(getId(order, 'vehicleId')) || getVehicle(getId(order, 'trailerVehicleId'))
  const carrierId = getId(vehicle, 'carrierId')
  const carrier = carrierStore.carriers.find((item) => getId(item, 'carrierId') === carrierId)
  return getValue(carrier, 'carrierName', 'carrier_name') || '-'
}

const getDriverName = (driverId) => {
  const driver = driverStore.drivers.find((item) => getId(item, 'driverId') === driverId)
  return getValue(driver, 'driverName', 'driver_name') || '-'
}

const getPlateNumber = (vehicleId) => {
  const vehicle = getVehicle(vehicleId)
  return getValue(vehicle, 'plateNumber', 'plate_number') || '-'
}

<<<<<<< HEAD
const getTrailerPlateNumber = (order) => {
  return getPlateNumber(getId(order, 'trailerVehicleId'))
=======
const getVehicleForType = (order, vehicleType) => {
  const vehicleIds = [
    getId(order, 'tractorVehicleId'),
    getId(order, 'trailerVehicleId'),
    getId(order, 'vehicleId'),
  ].filter(Boolean)

  return vehicleIds
    .map((vehicleId) => getVehicle(vehicleId))
    .find((vehicle) => getValue(vehicle, 'vehicleType', 'vehicle_type') === vehicleType) || null
}

const getVehicleApprovalText = (vehicle) => {
  if (!vehicle) return '미연결'
  const isRegistered = getValue(vehicle, 'isRegistered', 'is_registered')
  const vehicleStatus = getValue(vehicle, 'vehicleStatus', 'vehicle_status')
  if (isRegistered === true && vehicleStatus === '정상') return '승인'
  if (vehicleStatus === '승인거절') return '반려'
  return '승인대기'
}

const getVehicleApprovalClass = (vehicle) => {
  const status = getVehicleApprovalText(vehicle)
  if (status === '승인') return 'green'
  if (status === '반려') return 'red'
  return 'amber'
}

const getDriverEntryText = (order) => {
  const driver = driverStore.drivers.find((item) => getId(item, 'driverId') === getId(order, 'driverId'))
  if (!driver) return '미연결'
  return getValue(driver, 'canEnter', 'can_enter') === true ? '가능' : '불가'
>>>>>>> origin/hakseop
}

const getContainer = (containerId) => {
  return containerStore.containers.find((item) => getId(item, 'containerId') === containerId) || null
}

const getContainerNumber = (containerId) => {
  const container = getContainer(containerId)
  return getValue(container, 'containerNumber', 'container_number') || '-'
}

const getYardLocation = (containerId) => {
  const container = getContainer(containerId)
  if (!container) return '-'
  return `${container.block || '-'}-${container.bay || '-'}-${container.rowNo || container.row_no || '-'}`
}

const getStatusText = (workStatus) => {
  if (workStatus === 'DISPATCH_WAITING') return '승인 대기'
  if (workStatus === 'APPROVED') return '입차 대기'
  if (workStatus === 'GATE_IN') return '입차 완료'
  if (workStatus === 'IN_PROGRESS') return '작업 진행 중'
  if (workStatus === 'COMPLETED') return '출차 대기'
  if (workStatus === 'GATE_OUT') return '출차 완료'
  if (workStatus === 'CANCELED') return '반려'
  return workStatus || '-'
}

const getStatusClass = (workStatus) => {
  if (workStatus === 'DISPATCH_WAITING') return 'amber'
  if (workStatus === 'COMPLETED' || workStatus === 'GATE_OUT') return 'green'
  if (workStatus === 'IN_PROGRESS') return 'blue'
  if (workStatus === 'CANCELED') return 'red'
  return ''
}

const getSearchText = (order) => {
  const containerId = getId(order, 'containerId')
  const workStatus = getValue(order, 'workStatus', 'work_status')

  return [
    getId(order, 'workOrderId'),
    getCarrierName(order),
    getTrailerPlateNumber(order),
    getDriverName(getId(order, 'driverId')),
    getContainerNumber(containerId),
    getYardLocation(containerId),
    getValue(order, 'workType', 'work_type'),
    getValue(order, 'reservedTime', 'reserved_time'),
    workStatus,
    getStatusText(workStatus),
  ]
    .join(' ')
    .toLowerCase()
}

const filterByQuery = (orders, query) => {
  const keyword = query.trim().toLowerCase()
  if (!keyword) return orders
  return orders.filter((order) => getSearchText(order).includes(keyword))
}

const getPageCount = (total, pageSize) => Math.max(1, Math.ceil(total / pageSize))

const paginate = (items, page, pageSize) => {
  const start = (page - 1) * pageSize
  return items.slice(start, start + pageSize)
}

const getPageStart = (total, page, pageSize) => {
  if (total === 0) return 0
  return (page - 1) * pageSize + 1
}

const getPageEnd = (total, page, pageSize) => Math.min(total, page * pageSize)

const carrierRequests = computed(() => {
  return workOrderStore.workOrders.filter((order) => {
    const status = getValue(order, 'workStatus', 'work_status')
    const isApproved = getValue(order, 'isApproved', 'is_approved')
    return status === 'DISPATCH_WAITING' && isApproved !== true
  })
})

const processingTasks = computed(() => {
  return workOrderStore.workOrders.filter((order) => !carrierRequests.value.includes(order))
})

<<<<<<< HEAD
const filteredCarrierRequests = computed(() => filterByQuery(carrierRequests.value, requestQuery.value))

const filteredProcessingTasks = computed(() => {
  const status = taskStatus.value
  const statusFiltered = status
    ? processingTasks.value.filter((order) => getValue(order, 'workStatus', 'work_status') === status)
    : processingTasks.value

  return filterByQuery(statusFiltered, taskQuery.value)
})

const requestPageCount = computed(() => getPageCount(filteredCarrierRequests.value.length, requestPageSize.value))
const taskPageCount = computed(() => getPageCount(filteredProcessingTasks.value.length, taskPageSize.value))

const pagedCarrierRequests = computed(() =>
  paginate(filteredCarrierRequests.value, requestPage.value, requestPageSize.value),
)
const pagedProcessingTasks = computed(() =>
  paginate(filteredProcessingTasks.value, taskPage.value, taskPageSize.value),
)

const resetRequestSearch = () => {
  requestQuery.value = ''
  requestPageSize.value = 10
  requestPage.value = 1
}

const resetTaskSearch = () => {
  taskQuery.value = ''
  taskStatus.value = ''
  taskPageSize.value = 10
  taskPage.value = 1
=======
const visibleContainers = computed(() => {
  const query = containerQuery.value.trim().toLowerCase()
  if (!query) return containerStore.containers

  return containerStore.containers.filter((container) =>
    String(getValue(container, 'containerNumber', 'container_number')).toLowerCase().includes(query),
  )
})

const emptyContainerForm = () => ({
  containerNumber: '',
  containerSize: '20FT',
  shippingLine: '',
  containerLocation: '',
  block: '',
  bay: '',
  rowNo: '',
  sectorId: '',
  sealNumber: '',
  canExit: true,
})

const openContainerCreate = () => {
  editingContainerId.value = null
  containerMessage.value = ''
  containerForm.value = emptyContainerForm()
}

const openContainerEdit = (container) => {
  editingContainerId.value = getId(container, 'containerId')
  containerMessage.value = ''
  containerForm.value = {
    containerNumber: getValue(container, 'containerNumber', 'container_number'),
    containerSize: getValue(container, 'containerSize', 'container_size'),
    shippingLine: getValue(container, 'shippingLine', 'shipping_line'),
    containerLocation: getValue(container, 'containerLocation', 'container_location'),
    block: container.block || '',
    bay: container.bay || '',
    rowNo: getValue(container, 'rowNo', 'row_no'),
    sectorId: getId(container, 'sectorId') ?? '',
    sealNumber: getValue(container, 'sealNumber', 'seal_number'),
    canExit: container.canExit ?? container.can_exit ?? true,
  }
}

const closeContainerForm = () => {
  editingContainerId.value = null
  containerForm.value = {}
}

const saveContainer = async () => {
  containerMessage.value = ''
  const payload = {
    ...containerForm.value,
    sectorId: containerForm.value.sectorId == null || containerForm.value.sectorId === ''
      ? null
      : Number(containerForm.value.sectorId),
  }

  try {
    if (editingContainerId.value) {
      await containerStore.editContainer(editingContainerId.value, payload)
      containerMessage.value = '컨테이너 정보를 수정했습니다.'
    } else {
      await containerStore.addContainer(payload)
      containerMessage.value = '컨테이너를 등록했습니다.'
    }
    closeContainerForm()
  } catch (error) {
    containerMessage.value = error.message || '컨테이너 저장에 실패했습니다.'
  }
}

const removeContainer = async (container) => {
  const containerId = getId(container, 'containerId')
  if (!window.confirm(`${getValue(container, 'containerNumber', 'container_number')} 컨테이너를 삭제할까요?`)) return

  containerMessage.value = ''
  try {
    await containerStore.removeContainer(containerId)
    containerMessage.value = '컨테이너를 삭제했습니다.'
  } catch (error) {
    containerMessage.value = error.message || '컨테이너 삭제에 실패했습니다.'
  }
>>>>>>> origin/hakseop
}

const processWorkOrder = async (order, action) => {
  const workOrderId = getId(order, 'workOrderId')
  processingId.value = workOrderId
  processMessage.value = ''

  try {
    if (action === 'approve') {
      await workOrderStore.approve(workOrderId)
      processMessage.value = '작업 승인이 완료되었습니다.'
    }

    if (action === 'reject') {
      await workOrderStore.reject(workOrderId)
      processMessage.value = '작업 요청을 반려했습니다.'
    }

    if (action === 'start') {
      await workOrderStore.start(workOrderId)
      processMessage.value = '작업 시작 처리가 완료되었습니다.'
    }

    if (action === 'complete') {
      await workOrderStore.complete(workOrderId)
      processMessage.value = '작업 완료 처리가 완료되었습니다.'
    }
  } catch (error) {
    processMessage.value = error.message || '작업 처리에 실패했습니다.'
  } finally {
    processingId.value = null
  }
}

const loadData = () => {
  workOrderStore.loadWorkOrders().catch(() => {})
  carrierStore.loadCarriers().catch(() => {})
  containerStore.loadContainers().catch(() => {})
  driverStore.loadDrivers().catch(() => {})
  vehicleStore.loadVehicles().catch(() => {})
  fetchYardSectors().then((data) => { yardSectors.value = data || [] }).catch(() => {})
}

watch([requestQuery, requestPageSize], () => {
  requestPage.value = 1
})

watch([taskQuery, taskStatus, taskPageSize], () => {
  taskPage.value = 1
})

watch(requestPageCount, (pageCount) => {
  if (requestPage.value > pageCount) requestPage.value = pageCount
})

watch(taskPageCount, (pageCount) => {
  if (taskPage.value > pageCount) taskPage.value = pageCount
})

onMounted(() => {
  loadData()
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
        <span class="status-pill amber">배차 대기 {{ filteredCarrierRequests.length }}건</span>
      </div>

      <div class="work-order-toolbar">
        <div class="search-field">
          <label for="requestSearch">조회</label>
          <input
            id="requestSearch"
            v-model="requestQuery"
            type="search"
            placeholder="작업번호, 운송사, 트레일러 번호, 기사, 컨테이너"
          />
        </div>
        <div class="toolbar-actions">
          <label>
            표시
            <select v-model.number="requestPageSize">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}건</option>
            </select>
          </label>
          <button class="ghost-button" type="button" @click="resetRequestSearch">초기화</button>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업번호</th>
              <th>운송사</th>
              <th>트레일러 번호</th>
              <th>기사</th>
              <th>컨테이너</th>
              <th>작업 유형</th>
              <th>예약</th>
              <th>트랙터 승인</th>
              <th>트레일러 승인</th>
              <th>기사 출입</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in pagedCarrierRequests" :key="getId(order, 'workOrderId')">
              <td>{{ getId(order, 'workOrderId') }}</td>
              <td>{{ getCarrierName(order) }}</td>
              <td>{{ getTrailerPlateNumber(order) }}</td>
              <td>{{ getDriverName(getId(order, 'driverId')) }}</td>
              <td>{{ getContainerNumber(getId(order, 'containerId')) }}</td>
              <td>{{ getValue(order, 'workType', 'work_type') }}</td>
              <td>{{ getValue(order, 'reservedTime', 'reserved_time') }}</td>
              <td>
                <span class="status-pill" :class="getVehicleApprovalClass(getVehicleForType(order, 'TRACTOR'))">
                  {{ getVehicleApprovalText(getVehicleForType(order, 'TRACTOR')) }}
                </span>
              </td>
              <td>
                <span class="status-pill" :class="getVehicleApprovalClass(getVehicleForType(order, 'TRAILER'))">
                  {{ getVehicleApprovalText(getVehicleForType(order, 'TRAILER')) }}
                </span>
              </td>
              <td>
                <span class="status-pill" :class="getDriverEntryText(order) === '가능' ? 'green' : 'red'">
                  {{ getDriverEntryText(order) }}
                </span>
              </td>
              <td>
                <span class="status-pill amber">
                  {{ getStatusText(getValue(order, 'workStatus', 'work_status')) }}
                </span>
              </td>
              <td>
                <button
                  class="ghost-button"
                  type="button"
                  :disabled="processingId === getId(order, 'workOrderId')"
                  @click="processWorkOrder(order, 'approve')"
                >
                  {{ processingId === getId(order, 'workOrderId') ? '처리 중' : '작업 승인' }}
                </button>
                <button
                  class="ghost-button reject-button"
                  type="button"
                  :disabled="processingId === getId(order, 'workOrderId')"
                  @click="processWorkOrder(order, 'reject')"
                >
                  반려
                </button>
              </td>
            </tr>
<<<<<<< HEAD
            <tr v-if="filteredCarrierRequests.length === 0">
              <td colspan="9">조회된 배차 대기 작업이 없습니다.</td>
=======
            <tr v-if="carrierRequests.length === 0">
              <td colspan="12">배차 대기 작업이 없습니다.</td>
>>>>>>> origin/hakseop
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-bar">
        <span>
          {{ getPageStart(filteredCarrierRequests.length, requestPage, requestPageSize) }} -
          {{ getPageEnd(filteredCarrierRequests.length, requestPage, requestPageSize) }} /
          {{ filteredCarrierRequests.length }}건
        </span>
        <div class="pagination-controls">
          <button class="ghost-button" type="button" :disabled="requestPage === 1" @click="requestPage -= 1">
            이전
          </button>
          <strong>{{ requestPage }} / {{ requestPageCount }}</strong>
          <button
            class="ghost-button"
            type="button"
            :disabled="requestPage === requestPageCount"
            @click="requestPage += 1"
          >
            다음
          </button>
        </div>
      </div>
    </section>

    <p v-if="processMessage" class="process-message">{{ processMessage }}</p>

    <section class="panel">
      <div class="section-title">
        <h2>기사 할당 작업 관리</h2>
        <span class="status-pill green">작업 처리 {{ filteredProcessingTasks.length }}건</span>
      </div>

      <div class="work-order-toolbar">
        <div class="search-field">
          <label for="taskSearch">조회</label>
          <input
            id="taskSearch"
            v-model="taskQuery"
            type="search"
            placeholder="작업번호, 컨테이너, 트레일러 번호, 기사, 야드 위치"
          />
        </div>
        <div class="toolbar-actions">
          <label>
            상태
            <select v-model="taskStatus">
              <option v-for="option in taskStatusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <label>
            표시
            <select v-model.number="taskPageSize">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}건</option>
            </select>
          </label>
          <button class="ghost-button" type="button" @click="resetTaskSearch">초기화</button>
        </div>
      </div>

      <div class="table-wrap work-table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th>작업번호</th>
              <th>컨테이너</th>
              <th>트레일러 번호</th>
              <th>기사</th>
              <th>야드 위치</th>
              <th>트랙터 승인</th>
              <th>트레일러 승인</th>
              <th>기사 출입</th>
              <th>상태</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in pagedProcessingTasks" :key="getId(order, 'workOrderId')">
              <td>{{ getId(order, 'workOrderId') }}</td>
              <td>{{ getContainerNumber(getId(order, 'containerId')) }}</td>
              <td>{{ getTrailerPlateNumber(order) }}</td>
              <td>{{ getDriverName(getId(order, 'driverId')) }}</td>
              <td>{{ getYardLocation(getId(order, 'containerId')) }}</td>
              <td>
                <span class="status-pill" :class="getVehicleApprovalClass(getVehicleForType(order, 'TRACTOR'))">
                  {{ getVehicleApprovalText(getVehicleForType(order, 'TRACTOR')) }}
                </span>
              </td>
              <td>
                <span class="status-pill" :class="getVehicleApprovalClass(getVehicleForType(order, 'TRAILER'))">
                  {{ getVehicleApprovalText(getVehicleForType(order, 'TRAILER')) }}
                </span>
              </td>
              <td>
                <span class="status-pill" :class="getDriverEntryText(order) === '가능' ? 'green' : 'red'">
                  {{ getDriverEntryText(order) }}
                </span>
              </td>
              <td>
                <span class="status-pill" :class="getStatusClass(getValue(order, 'workStatus', 'work_status'))">
                  {{ getStatusText(getValue(order, 'workStatus', 'work_status')) }}
                </span>
              </td>
              <td>
                <button
                  v-if="getValue(order, 'workStatus', 'work_status') === 'GATE_IN'"
                  class="primary-button"
                  type="button"
                  :disabled="processingId === getId(order, 'workOrderId')"
                  @click="processWorkOrder(order, 'start')"
                >
                  {{ processingId === getId(order, 'workOrderId') ? '처리 중' : '작업 시작' }}
                </button>
                <button
                  v-else-if="getValue(order, 'workStatus', 'work_status') === 'IN_PROGRESS'"
                  class="primary-button"
                  type="button"
                  :disabled="processingId === getId(order, 'workOrderId')"
                  @click="processWorkOrder(order, 'complete')"
                >
                  {{ processingId === getId(order, 'workOrderId') ? '처리 중' : '작업 완료' }}
                </button>
                <span v-else>{{ getStatusText(getValue(order, 'workStatus', 'work_status')) }}</span>
              </td>
            </tr>
<<<<<<< HEAD
            <tr v-if="filteredProcessingTasks.length === 0">
              <td colspan="7">조회된 처리 작업이 없습니다.</td>
=======
            <tr v-if="processingTasks.length === 0">
              <td colspan="10">처리 중인 작업이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>컨테이너 조회</h2>
        <div class="table-tools">
          <input v-model="containerQuery" type="search" placeholder="컨테이너 번호 검색" />
          <span class="status-pill">{{ visibleContainers.length }}건</span>
          <button class="primary-button" type="button" @click="openContainerCreate">컨테이너 추가</button>
        </div>
      </div>

      <form v-if="Object.keys(containerForm).length" class="container-form" @submit.prevent="saveContainer">
        <strong>{{ editingContainerId ? '컨테이너 수정' : '컨테이너 등록' }}</strong>
        <input v-model.trim="containerForm.containerNumber" required placeholder="컨테이너 번호 *" />
        <select v-model="containerForm.containerSize"><option>20FT</option><option>40FT</option><option>45FT</option></select>
        <input v-model.trim="containerForm.shippingLine" placeholder="선사" />
        <input v-model.trim="containerForm.containerLocation" placeholder="현재 위치" />
        <input v-model.trim="containerForm.block" placeholder="블록" />
        <input v-model.trim="containerForm.bay" placeholder="베이" />
        <input v-model.trim="containerForm.rowNo" placeholder="로우" />
        <select v-model="containerForm.sectorId">
          <option value="">야드 섹터 미지정</option>
          <option v-for="sector in yardSectors" :key="sector.sectorId" :value="sector.sectorId">
            {{ sector.sectorName }} (ID {{ sector.sectorId }})
          </option>
        </select>
        <input v-model.trim="containerForm.sealNumber" placeholder="봉인 번호" />
        <label class="exit-check"><input v-model="containerForm.canExit" type="checkbox" /> 반출 가능</label>
        <div class="container-form-actions">
          <button class="primary-button" type="submit" :disabled="containerStore.loading">{{ containerStore.loading ? '저장 중' : '저장' }}</button>
          <button class="ghost-button" type="button" @click="closeContainerForm">취소</button>
        </div>
      </form>

      <p v-if="containerMessage" class="container-message">{{ containerMessage }}</p>

      <div class="table-wrap work-table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th>컨테이너 ID</th>
              <th>컨테이너 번호</th>
              <th>규격</th>
              <th>선사</th>
              <th>현재 위치</th>
              <th>야드 위치</th>
              <th>반출 가능</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="container in visibleContainers" :key="getId(container, 'containerId')">
              <td>{{ getId(container, 'containerId') }}</td>
              <td>{{ getValue(container, 'containerNumber', 'container_number') || '-' }}</td>
              <td>{{ getValue(container, 'containerSize', 'container_size') || '-' }}</td>
              <td>{{ getValue(container, 'shippingLine', 'shipping_line') || '-' }}</td>
              <td>{{ getValue(container, 'containerLocation', 'container_location') || '-' }}</td>
              <td>{{ getYardLocation(getId(container, 'containerId')) }}</td>
              <td>
                <span class="status-pill" :class="{ red: !(container.canExit ?? container.can_exit) }">
                  {{ container.canExit ?? container.can_exit ? '가능' : '보류' }}
                </span>
              </td>
              <td class="container-actions">
                <button class="ghost-button" type="button" @click="openContainerEdit(container)">수정</button>
                <button class="ghost-button reject-button" type="button" @click="removeContainer(container)">삭제</button>
              </td>
            </tr>
            <tr v-if="visibleContainers.length === 0">
              <td colspan="8">컨테이너 데이터가 없습니다.</td>
>>>>>>> origin/hakseop
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-bar">
        <span>
          {{ getPageStart(filteredProcessingTasks.length, taskPage, taskPageSize) }} -
          {{ getPageEnd(filteredProcessingTasks.length, taskPage, taskPageSize) }} /
          {{ filteredProcessingTasks.length }}건
        </span>
        <div class="pagination-controls">
          <button class="ghost-button" type="button" :disabled="taskPage === 1" @click="taskPage -= 1">
            이전
          </button>
          <strong>{{ taskPage }} / {{ taskPageCount }}</strong>
          <button
            class="ghost-button"
            type="button"
            :disabled="taskPage === taskPageCount"
            @click="taskPage += 1"
          >
            다음
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.work-order-toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.search-field {
  display: grid;
  flex: 1;
  max-width: 430px;
  gap: 4px;
}

.search-field label,
.toolbar-actions label {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

.search-field input,
.toolbar-actions select {
  min-height: 30px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid #aeb9c5;
  border-radius: 2px;
  padding: 0 8px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  justify-content: flex-end;
  gap: 8px;
}

.toolbar-actions label {
  display: grid;
  gap: 4px;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 10px;
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 700;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-controls strong {
  min-width: 54px;
  text-align: center;
}

.process-message {
  margin: 0;
  padding: 10px 12px;
  color: var(--ink-700);
  background: #f8fbfe;
  border: 1px solid var(--line);
  font-weight: 800;
}

.reject-button {
  margin-left: 6px;
  color: #9f1d1d;
  border-color: #e4a6a6;
}

<<<<<<< HEAD
button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

@media (max-width: 760px) {
  .work-order-toolbar,
  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-field {
    max-width: none;
  }

  .toolbar-actions,
  .pagination-controls {
    justify-content: flex-start;
  }
=======
.work-table-scroll {
  max-height: 470px;
  overflow: auto;
}

.work-table-scroll thead th {
  position: sticky;
  top: 0;
  z-index: 1;
}

.table-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-tools input {
  width: 200px;
  min-height: 34px;
  padding: 0 10px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 4px;
  font-weight: 700;
}

.container-form {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  align-items: center;
  margin: 0 0 10px;
  padding: 10px;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.container-form strong { color: var(--ink-900); }

.container-form input,
.container-form select {
  min-width: 0;
  min-height: 34px;
  padding: 0 9px;
  color: var(--ink-900);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 4px;
  font-weight: 700;
}

.exit-check { color: var(--ink-700); font-size: 13px; font-weight: 800; }
.container-form-actions, .container-actions { display: flex; gap: 6px; }
.container-message { margin: 0 0 10px; color: var(--ink-700); font-weight: 800; }

@media (max-width: 980px) {
  .container-form { grid-template-columns: repeat(2, minmax(0, 1fr)); }
>>>>>>> origin/hakseop
}
</style>
