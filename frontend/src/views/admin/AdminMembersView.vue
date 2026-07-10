<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { getUsers, updateUserStatus } from '@/api/authApi'
import { useCarrierStore } from '@/stores/carrierStore'
import { useDriverStore } from '@/stores/driverStore'
import { useVehicleStore } from '@/stores/vehicleStore'

const activeTab = ref('user')
const loading = ref(false)
const errorMessage = ref('')

const users = ref([])

const carrierStore = useCarrierStore()
const driverStore = useDriverStore()
const vehicleStore = useVehicleStore()

const { carriers } = storeToRefs(carrierStore)
const { drivers } = storeToRefs(driverStore)
const { vehicles } = storeToRefs(vehicleStore)

const carrierPendingUsers = computed(() =>
  users.value.filter(
    (user) => user.roleCode === 'CARRIER' && user.status === 'PENDING',
  ),
)

const driverPendingUsers = computed(() =>
  users.value.filter(
    (user) =>
      user.roleCode === 'DRIVER' &&
      (user.status === 'PENDING' || user.status === 'CARRIER_APPROVED'),
  ),
)

const finalApprovalVehicles = computed(() =>
  vehicles.value.filter(
    (vehicle) =>
      vehicle.vehicleStatus === '승인대기' ||
      vehicle.vehicleStatus === 'PENDING',
  ),
)

const tabs = computed(() => [
  {
    key: 'user',
    label: '운송사 가입승인',
    count: carrierPendingUsers.value.length,
  },
  {
    key: 'driverUser',
    label: '기사 최종승인 대상',
    count: driverPendingUsers.value.length,
  },
  {
    key: 'carrier',
    label: '운송사',
    count: carriers.value.length,
  },
  {
    key: 'driver',
    label: '기사',
    count: drivers.value.length,
  },
  {
    key: 'vehicle',
    label: '최종 승인',
    count: finalApprovalVehicles.value.length,
  },
])

const getDriver = (driverId) => {
  return drivers.value.find((driver) => driver.driverId === driverId)
}

const getCarrier = (carrierId) => {
  return carriers.value.find((carrier) => carrier.carrierId === carrierId)
}

const loadUsers = async () => {
  users.value = (await getUsers()) || []
}

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    await Promise.all([
      loadUsers(),
      carrierStore.loadCarriers(),
      driverStore.loadDrivers(),
      vehicleStore.loadVehicles(),
    ])
  } catch (error) {
    errorMessage.value =
      error.message || '관리자 승인 데이터를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const approveCarrier = async (userId) => {
  errorMessage.value = ''

  try {
    await updateUserStatus(userId, 'ACTIVE')
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '운송사 승인에 실패했습니다.'
  }
}

const rejectCarrier = async (userId) => {
  errorMessage.value = ''

  try {
    await updateUserStatus(userId, 'REJECTED')
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '운송사 반려에 실패했습니다.'
  }
}

const rejectDriverUser = async (userId) => {
  errorMessage.value = ''

  try {
    await updateUserStatus(userId, 'REJECTED')
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '기사 반려에 실패했습니다.'
  }
}

const approveVehicle = async (vehicleId) => {
  errorMessage.value = ''

  try {
    await vehicleStore.approveVehicle(vehicleId, true)
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '관리자 최종 승인에 실패했습니다.'
  }
}

const rejectVehicle = async (vehicleId) => {
  errorMessage.value = ''

  try {
    await vehicleStore.approveVehicle(vehicleId, false)
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '최종 승인 반려에 실패했습니다.'
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>관리자 승인 관리</h2>

        <span class="status-pill">
          운송사 승인대기 {{ carrierPendingUsers.length }} /
          기사 검토대상 {{ driverPendingUsers.length }} /
          최종 승인대기 {{ finalApprovalVehicles.length }}
        </span>
      </div>

      <div v-if="errorMessage" class="form-message error">
        {{ errorMessage }}
      </div>

      <div class="member-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          type="button"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <b>{{ tab.count }}</b>
        </button>

        <button
          class="refresh-button"
          type="button"
          :disabled="loading"
          @click="loadData"
        >
          {{ loading ? '불러오는 중...' : '새로고침' }}
        </button>
      </div>
    </section>

    <section v-if="activeTab === 'user'" class="panel">
      <div class="section-title">
        <h2>운송사 가입 승인</h2>
        <span class="status-pill amber">회원가입 승인 대기</span>
      </div>

      <div v-if="loading" class="empty-box">불러오는 중...</div>

      <div v-else-if="carrierPendingUsers.length === 0" class="empty-box">
        승인 대기 중인 운송사가 없습니다.
      </div>

      <div v-else class="approval-grid">
        <article
          v-for="user in carrierPendingUsers"
          :key="user.userId"
          class="approval-card"
        >
          <div class="approval-head">
            <div>
              <h3>{{ user.userName || user.loginId }}</h3>
              <p>{{ user.loginId }}</p>
            </div>

            <span class="status-pill amber">{{ user.status }}</span>
          </div>

          <dl>
            <div>
              <dt>회원번호</dt>
              <dd>{{ user.userId }}</dd>
            </div>
            <div>
              <dt>역할</dt>
              <dd>{{ user.roleCode }}</dd>
            </div>
            <div>
              <dt>가입일</dt>
              <dd>{{ user.createdAt || '-' }}</dd>
            </div>
          </dl>

          <div class="action-row">
            <button
              class="ghost-button approve"
              type="button"
              @click="approveCarrier(user.userId)"
            >
              승인
            </button>

            <button
              class="ghost-button reject"
              type="button"
              @click="rejectCarrier(user.userId)"
            >
              반려
            </button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeTab === 'driverUser'" class="panel">
      <div class="section-title">
        <h2>기사 최종승인 대상</h2>
        <span class="status-pill amber">
          운송사 승인·차량 배정 확인 대상
        </span>
      </div>

      <div v-if="loading" class="empty-box">불러오는 중...</div>

      <div v-else-if="driverPendingUsers.length === 0" class="empty-box">
        검토 대상 기사가 없습니다.
      </div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>회원 ID</th>
              <th>아이디</th>
              <th>회원명</th>
              <th>상태</th>
              <th>가입일</th>
              <th>관리</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="user in driverPendingUsers"
              :key="user.userId"
            >
              <td>{{ user.userId }}</td>
              <td>{{ user.loginId }}</td>
              <td>{{ user.userName }}</td>
              <td>
                <span class="status-pill amber">
                  {{ user.status }}
                </span>
              </td>
              <td>{{ user.createdAt || '-' }}</td>
              <td>
                <div class="action-row">
                  <span class="status-pill">
                    차량 최종 승인 탭에서 처리
                  </span>

                  <button
                    class="ghost-button reject"
                    type="button"
                    @click="rejectDriverUser(user.userId)"
                  >
                    반려
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else-if="activeTab === 'carrier'" class="panel">
      <div class="section-title">
        <h2>운송사 목록</h2>
        <span class="status-pill green">DB 조회</span>
      </div>

      <div v-if="loading" class="empty-box">불러오는 중...</div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>운송사 ID</th>
              <th>운송사명</th>
              <th>연락처</th>
              <th>담당자명</th>
              <th>가입 상태</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="carrier in carriers" :key="carrier.carrierId">
              <td>{{ carrier.carrierId }}</td>
              <td>{{ carrier.carrierName }}</td>
              <td>{{ carrier.carrierContact || '-' }}</td>
              <td>{{ carrier.managerName || '-' }}</td>
              <td>
                <span
                  class="status-pill"
                  :class="{
                    green:
                      carrier.carrierStatus === 'APPROVED' ||
                      carrier.carrierStatus === 'ACTIVE',
                    amber: carrier.carrierStatus === 'PENDING',
                    red: carrier.carrierStatus === 'REJECTED',
                  }"
                >
                  {{ carrier.carrierStatus }}
                </span>
              </td>
            </tr>

            <tr v-if="carriers.length === 0">
              <td colspan="5">등록된 운송사가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else-if="activeTab === 'driver'" class="panel">
      <div class="section-title">
        <h2>기사 목록</h2>
        <span class="status-pill green">DB 조회</span>
      </div>

      <div v-if="loading" class="empty-box">불러오는 중...</div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>기사 ID</th>
              <th>기사명</th>
              <th>연락처</th>
              <th>운송사 승인</th>
              <th>운송사 ID</th>
              <th>출입 가능</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="driver in drivers" :key="driver.driverId">
              <td>{{ driver.driverId }}</td>
              <td>{{ driver.driverName }}</td>
              <td>{{ driver.driverContact || '-' }}</td>
              <td>
                <span
                  class="status-pill"
                  :class="driver.isRegistered ? 'green' : 'amber'"
                >
                  {{
                    driver.isRegistered
                      ? '운송사 승인 완료'
                      : '운송사 승인 대기'
                  }}
                </span>
              </td>
              <td>{{ driver.carrierId || '-' }}</td>
              <td>
                <span
                  class="status-pill"
                  :class="driver.canEnter ? 'green' : 'red'"
                >
                  {{ driver.canEnter ? '가능' : '불가' }}
                </span>
              </td>
            </tr>

            <tr v-if="drivers.length === 0">
              <td colspan="6">등록된 기사가 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else class="panel">
      <div class="section-title">
        <h2>관리자 최종 승인</h2>
        <span class="status-pill amber">
          운송사 차량 배정 후 승인대기
        </span>
      </div>

      <div v-if="loading" class="empty-box">불러오는 중...</div>

      <div
        v-else-if="finalApprovalVehicles.length === 0"
        class="empty-box"
      >
        최종 승인 대기 중인 차량 배정 내역이 없습니다.
      </div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>차량 ID</th>
              <th>기사</th>
              <th>운송사</th>
              <th>차량 번호</th>
              <th>차량 유형</th>
              <th>톤수</th>
              <th>트랙터 번호</th>
              <th>샤시 번호</th>
              <th>상태</th>
              <th>최종 승인</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="vehicle in finalApprovalVehicles"
              :key="vehicle.vehicleId"
            >
              <td>{{ vehicle.vehicleId }}</td>
              <td>
                {{ getDriver(vehicle.driverId)?.driverName || '-' }}
              </td>
              <td>
                {{ getCarrier(vehicle.carrierId)?.carrierName || '-' }}
              </td>
              <td>{{ vehicle.plateNumber }}</td>
              <td>{{ vehicle.vehicleType }}</td>
              <td>{{ vehicle.tonnage }}</td>
              <td>{{ vehicle.tractorNo || '-' }}</td>
              <td>{{ vehicle.chassisNo || '-' }}</td>
              <td>
                <span class="status-pill amber">
                  {{ vehicle.vehicleStatus }}
                </span>
              </td>
              <td>
                <div class="action-row">
                  <button
                    class="ghost-button approve"
                    type="button"
                    @click="approveVehicle(vehicle.vehicleId)"
                  >
                    최종 승인
                  </button>

                  <button
                    class="ghost-button reject"
                    type="button"
                    @click="rejectVehicle(vehicle.vehicleId)"
                  >
                    반려
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.member-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.member-tabs button {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  color: var(--ink-700);
  background: #eef3f8;
  border: 1px solid var(--line);
  border-radius: 1px;
  font-weight: 700;
}

.member-tabs button.active {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.member-tabs b {
  font-weight: 700;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.form-message.error {
  margin-top: 10px;
  padding: 10px 12px;
  color: #7f1d1d;
  background: #fff1f1;
  border: 1px solid #fecaca;
}

.refresh-button {
  min-height: 32px;
  margin-left: auto;
  padding: 0 12px;
  color: #ffffff;
  background: var(--blue-700);
  border: 1px solid var(--blue-700);
  border-radius: 2px;
  font-weight: 700;
}

.refresh-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.empty-box {
  padding: 24px;
  color: var(--ink-500);
  text-align: center;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.approval-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.approval-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.approval-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.approval-head h3 {
  margin: 0;
  font-size: 16px;
}

.approval-head p {
  margin: 4px 0 0;
  color: var(--ink-500);
}

.approval-card dl {
  display: grid;
  gap: 6px;
  margin: 0;
}

.approval-card dl div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.approval-card dt {
  color: var(--ink-500);
}

.approval-card dd {
  margin: 0;
  font-weight: 700;
}

.ghost-button.approve {
  color: #166534;
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.ghost-button.reject {
  color: #991b1b;
  background: #fff1f2;
  border-color: #fecaca;
}
</style>

