<script setup>
import { computed, onMounted, ref } from 'vue'
import { readCurrentUser } from '@/stores/authStore'
import { fetchCarriers } from '@/api/carrierApi'
import { fetchDrivers } from '@/api/driverApi'
import { createVehicle } from '@/api/vehicleApi'

const loading = ref(false)
const saving = ref(false)
const message = ref('')
const errorMessage = ref('')

const carriers = ref([])
const drivers = ref([])

const currentUser = readCurrentUser()

const vehicleTypes = ['트랙터', '트레일러', '카고', '윙바디', '탱크로리', '냉동탑', '기타']
const tonnageOptions = ['1톤', '2.5톤', '5톤', '8.5톤', '11톤', '18톤', '25톤']

const form = ref({
  driverId: null,
  plateNumber: '',
  vehicleType: '트랙터',
  tonnage: '25톤',
  tractorNo: '',
  chassisNo: '',
})

const myCarrier = computed(() =>
  carriers.value.find((carrier) => carrier.userId === currentUser?.userId),
)

const myDrivers = computed(() => {
  if (!myCarrier.value) return []

  return drivers.value.filter(
    (driver) => driver.carrierId === myCarrier.value.carrierId,
  )
})

const assignableDrivers = computed(() =>
  myDrivers.value.filter(
    (driver) =>
      driver.isRegistered === true &&
      driver.canEnter === false,
  ),
)

const selectedDriver = computed(() =>
  assignableDrivers.value.find((driver) => driver.driverId === form.value.driverId),
)

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const [carrierData, driverData] = await Promise.all([
      fetchCarriers(),
      fetchDrivers(),
    ])

    carriers.value = carrierData || []
    drivers.value = driverData || []
  } catch (error) {
    errorMessage.value = error.message || '데이터를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const generateCode = (prefix) => {
  const number = Math.floor(Math.random() * 900000 + 100000)
  return `${prefix}-${number}`
}

const resetForm = () => {
  form.value = {
    driverId: null,
    plateNumber: '',
    vehicleType: '트랙터',
    tonnage: '25톤',
    tractorNo: '',
    chassisNo: '',
  }
}

const validate = () => {
  if (!myCarrier.value) {
    throw new Error('로그인한 운송사 정보를 찾을 수 없습니다.')
  }

  if (!form.value.driverId) {
    throw new Error('트레일러를 배정할 기사를 선택하세요.')
  }

  if (!selectedDriver.value) {
    throw new Error('운송사 승인 완료 및 관리자 최종 승인 전 기사만 배정할 수 있습니다.')
  }

  if (!form.value.plateNumber.trim()) {
    throw new Error('차량번호를 입력하세요.')
  }

  if (!form.value.vehicleType) {
    throw new Error('차량종류를 선택하세요.')
  }

  if (!form.value.tonnage) {
    throw new Error('톤수를 선택하세요.')
  }
}

const submitVehicle = async () => {
  message.value = ''
  errorMessage.value = ''

  try {
    validate()
    saving.value = true

    await createVehicle({
      driverId: form.value.driverId,
      carrierId: myCarrier.value.carrierId,
      userId: selectedDriver.value?.userId,
      plateNumber: form.value.plateNumber,
      vehicleType: form.value.vehicleType,
      tonnage: form.value.tonnage,
      tractorNo: form.value.tractorNo,
      chassisNo: form.value.chassisNo,
    })

    message.value = '트레일러 배정 내용이 관리자에게 전달되었습니다. 최종 승인 후 출입 가능합니다.'
    resetForm()
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '트레일러 배정에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>트레일러 배정</h2>
        <span class="status-pill">
          {{ myCarrier?.carrierName || '운송사 조회 중' }}
        </span>
      </div>

      <div v-if="message" class="form-message success">
        {{ message }}
      </div>

      <div v-if="errorMessage" class="form-message error">
        {{ errorMessage }}
      </div>
    </section>

    <section class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>배정 정보 입력</h2>
          <span class="status-pill amber">관리자 최종 승인 요청</span>
        </div>

        <div v-if="loading" class="empty-box">
          불러오는 중...
        </div>

        <form v-else class="form-grid" @submit.prevent="submitVehicle">
          <div class="field full">
            <label for="driverId">배정 기사</label>
            <select id="driverId" v-model.number="form.driverId">
              <option disabled :value="null">운송사 승인 완료 기사를 선택하세요</option>
              <option
                v-for="driver in assignableDrivers"
                :key="driver.driverId"
                :value="driver.driverId"
              >
                {{ driver.driverName }} / {{ driver.driverContact || '-' }}
              </option>
            </select>
          </div>

          <div class="field">
            <label for="plateNumber">차량번호</label>
            <input
              id="plateNumber"
              v-model.trim="form.plateNumber"
              placeholder="예: 부산80바9999"
            />
          </div>

          <div class="field">
            <label for="vehicleType">차량종류</label>
            <select id="vehicleType" v-model="form.vehicleType">
              <option v-for="type in vehicleTypes" :key="type" :value="type">
                {{ type }}
              </option>
            </select>
          </div>

          <div class="field">
            <label for="tonnage">톤수</label>
            <select id="tonnage" v-model="form.tonnage">
              <option v-for="ton in tonnageOptions" :key="ton" :value="ton">
                {{ ton }}
              </option>
            </select>
          </div>

          <div class="field">
            <label for="tractorNo">트랙터 번호</label>
            <div class="inline-field">
              <input id="tractorNo" v-model.trim="form.tractorNo" placeholder="예: TR-999999" />
              <button type="button" @click="form.tractorNo = generateCode('TR')">자동</button>
            </div>
          </div>

          <div class="field full">
            <label for="chassisNo">샤시 번호</label>
            <div class="inline-field">
              <input id="chassisNo" v-model.trim="form.chassisNo" placeholder="예: CH-999999" />
              <button type="button" @click="form.chassisNo = generateCode('CH')">자동</button>
            </div>
          </div>

          <button class="primary-button full submit-button" type="submit" :disabled="saving">
            {{ saving ? '전달 중...' : '관리자에게 최종 승인 요청' }}
          </button>
        </form>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>배정 가능 기사</h2>
          <span class="status-pill green">{{ assignableDrivers.length }}명</span>
        </div>

        <div v-if="myDrivers.length === 0" class="empty-box">
          소속 기사가 없습니다.
        </div>

        <div v-else-if="assignableDrivers.length === 0" class="empty-box">
          배정 가능한 기사가 없습니다. 먼저 기사 가입 승인 화면에서 기사를 승인하세요.
        </div>

        <div v-else class="driver-list">
          <div
            v-for="driver in assignableDrivers"
            :key="driver.driverId"
            class="driver-row"
            :class="{ selected: driver.driverId === form.driverId }"
          >
            <div>
              <b>{{ driver.driverName }}</b>
              <span>연락처 {{ driver.driverContact || '-' }}</span>
            </div>

            <div class="driver-actions">
              <span class="status-pill amber">
                트레일러 배정 가능
              </span>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.full {
  grid-column: 1 / -1;
}

.form-message {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 700;
}

.form-message.success {
  color: #155e3b;
  background: #ecfdf3;
  border: 1px solid #b7ebc9;
}

.form-message.error {
  color: #991b1b;
  background: #fff1f1;
  border: 1px solid #fecaca;
}

.empty-box {
  padding: 24px;
  color: var(--ink-500);
  text-align: center;
  background: #f8fbfe;
  border: 1px solid var(--line);
}

.inline-field {
  display: flex;
  gap: 8px;
}

.inline-field input {
  flex: 1;
  min-width: 0;
}

.inline-field button {
  width: 60px;
  color: #ffffff;
  background: var(--blue-700);
  border: 1px solid var(--blue-700);
  border-radius: 4px;
  font-weight: 700;
}

.submit-button {
  min-height: 44px;
}

.driver-list {
  display: grid;
  gap: 10px;
}

.driver-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.driver-row.selected {
  border-color: var(--blue-700);
  box-shadow: inset 0 0 0 1px var(--blue-700);
}

.driver-row b,
.driver-row span {
  display: block;
}

.driver-row b {
  margin-bottom: 4px;
}

.driver-row div span {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}

.driver-actions {
  display: grid;
  justify-items: end;
  gap: 6px;
}

@media (max-width: 760px) {
  .inline-field {
    flex-direction: column;
  }

  .inline-field button {
    width: 100%;
    min-height: 36px;
  }

  .driver-row {
    align-items: stretch;
    flex-direction: column;
  }

  .driver-actions {
    justify-items: start;
  }
}
</style>