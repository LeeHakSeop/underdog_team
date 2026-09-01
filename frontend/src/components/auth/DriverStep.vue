<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchCarrierSignupOptions } from '@/api/carrierApi'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const carriers = ref([])
const loading = ref(false)
const errorMessage = ref('')

const axleOptions = ['4x2', '6x2', '6x4']
const approvedCarriers = computed(() => carriers.value)

const selectedCarrier = computed(() =>
  approvedCarriers.value.find(
    (carrier) => String(carrier.carrierId) === String(props.modelValue.carrierId),
  ),
)

const updateField = (key, value) => {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value,
    vehicleType: 'TRACTOR',
  })
}

const ensureSystemChassisNo = () => {
  if (props.modelValue.chassisNo) return
  const suffix = Math.floor(Math.random() * 900000 + 100000)
  updateField('chassisNo', `AUTO-${suffix}`)
}

const loadCarriers = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    carriers.value = (await fetchCarrierSignupOptions()) || []
  } catch (error) {
    errorMessage.value = error.message || '운송사 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!props.modelValue.vehicleType) updateField('vehicleType', 'TRACTOR')
  if (!props.modelValue.tonnage) updateField('tonnage', '6x2')
  ensureSystemChassisNo()
  loadCarriers()
})
</script>

<template>
  <section class="step-section">
    <div class="section-head">
      <p class="section-label">STEP 2</p>
      <h3>기사 및 차량 정보</h3>
      <p>터미널 출입과 배차 확인에 필요한 정보입니다.</p>
    </div>

    <div v-if="errorMessage" class="form-message error">
      {{ errorMessage }}
    </div>

    <div class="subsection driver-info">
      <h4>기사 정보</h4>

      <div class="form-grid">
        <div class="field">
          <label for="driverName">이름 *</label>
          <input
            id="driverName"
            :value="modelValue.driverName"
            placeholder="홍길동"
            autocomplete="name"
            @input="updateField('driverName', $event.target.value)"
          />
        </div>

        <div class="field">
          <label for="driverContact">연락처 *</label>
          <input
            id="driverContact"
            :value="modelValue.driverContact"
            placeholder="010-0000-0000"
            inputmode="tel"
            autocomplete="tel"
            @input="updateField('driverContact', $event.target.value)"
          />
        </div>

        <div class="field field-full">
          <label for="carrierId">소속 운송사 *</label>
          <select
            id="carrierId"
            :value="modelValue.carrierId || ''"
            @change="updateField('carrierId', Number($event.target.value))"
          >
            <option disabled value="">운송사를 선택하세요</option>
            <option
              v-for="carrier in approvedCarriers"
              :key="carrier.carrierId"
              :value="carrier.carrierId"
            >
              {{ carrier.carrierName }}
            </option>
          </select>
          <small v-if="selectedCarrier">운송사 승인 후 관리자 최종 승인이 진행됩니다.</small>
        </div>
      </div>

      <div v-if="loading" class="loading">운송사 목록을 불러오는 중입니다.</div>
      <div v-else-if="!errorMessage && approvedCarriers.length === 0" class="empty-message">
        승인된 운송사가 없습니다. 운송사 승인 후 기사 가입을 진행할 수 있습니다.
      </div>
    </div>

    <div class="subsection tractor-info">
      <h4>본인 트랙터</h4>

      <div class="form-grid vehicle-grid">
        <div class="field important">
          <label for="plateNumber">차량번호 *</label>
          <input
            id="plateNumber"
            :value="modelValue.plateNumber"
            placeholder="전북84사8507"
            autocomplete="off"
            @input="updateField('plateNumber', $event.target.value)"
          />
        </div>

        <div class="field">
          <label for="vehicleType">차량유형</label>
          <input id="vehicleType" value="트랙터" disabled />
        </div>

        <div class="field">
          <label for="tonnage">축 형식 *</label>
          <select
            id="tonnage"
            :value="modelValue.tonnage || '6x2'"
            @change="updateField('tonnage', $event.target.value)"
          >
            <option v-for="axle in axleOptions" :key="axle" :value="axle">
              {{ axle }}
            </option>
          </select>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.step-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 24px;
}

.section-head {
  display: grid;
  gap: 6px;
  grid-column: 1 / -1;
}

.section-label {
  margin: 0;
  color: var(--blue-700);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.section-head h3,
.section-head p,
.subsection h4 {
  margin: 0;
}

.section-head h3 {
  color: var(--ink-900);
  font-size: 24px;
  font-weight: 800;
}

.section-head p,
.field small {
  color: var(--ink-500);
  font-size: 14px;
  line-height: 1.45;
}

.subsection {
  display: grid;
  gap: 14px;
  align-content: start;
}

.subsection h4 {
  color: var(--ink-900);
  font-size: 16px;
  font-weight: 800;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.vehicle-grid {
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 0.85fr);
}

.vehicle-grid .important {
  grid-column: 1 / -1;
}

.field {
  display: grid;
  align-content: start;
  gap: 7px;
}

.field-full {
  grid-column: 1 / -1;
}

.field label {
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 800;
}

.field input,
.field select {
  width: 100%;
  min-height: 48px;
  padding: 0 14px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid #cbd7e3;
  border-radius: 8px;
}

.field input:focus,
.field select:focus {
  border-color: var(--blue-700);
  outline: none;
  box-shadow: 0 0 0 3px rgba(40, 103, 166, 0.12);
}

.field.important input {
  border-color: #9bb8d6;
}

.field input:disabled {
  color: var(--ink-500);
  background: #edf2f7;
}

.loading,
.empty-message {
  color: var(--ink-500);
  font-size: 13px;
}

.empty-message,
.form-message {
  padding: 10px 12px;
  border-radius: 8px;
}

.empty-message {
  background: #fff8e6;
  border: 1px solid #f3d28c;
}

.form-message {
  grid-column: 1 / -1;
  font-size: 13px;
  font-weight: 800;
}

.form-message.error {
  color: #991b1b;
  background: #fff1f1;
  border: 1px solid #fecaca;
}

@media (max-width: 860px) {
  .step-section {
    grid-template-columns: 1fr;
  }

  .form-grid,
  .vehicle-grid {
    grid-template-columns: 1fr;
  }

  .field-full,
  .vehicle-grid .important {
    grid-column: auto;
  }
}
</style>
