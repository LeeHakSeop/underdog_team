<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchCarriers } from '@/api/carrierApi'

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

const tonnageOptions = [
  '1톤',
  '2.5톤',
  '5톤',
  '8.5톤',
  '11톤',
  '18톤',
  '25톤',
]

const approvedCarriers = computed(() =>
  carriers.value.filter(
    (carrier) =>
      carrier.carrierStatus === 'APPROVED' ||
      carrier.carrierStatus === 'ACTIVE',
  ),
)

const updateField = (key, value) => {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value,

    // 기사 회원가입 차량은 트랙터로 고정
    vehicleType: 'TRACTOR',
  })
}

const generateCode = (prefix) => {
  const number = Math.floor(
    Math.random() * 900000 + 100000,
  )

  return `${prefix}-${number}`
}

const generateTractorNo = () => {
  updateField('tractorNo', generateCode('TR'))
}

const generateChassisNo = () => {
  updateField('chassisNo', generateCode('CH'))
}

const loadCarriers = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    carriers.value = (await fetchCarriers()) || []
  } catch (error) {
    errorMessage.value =
      error.message ||
      '운송사 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!props.modelValue.vehicleType) {
    updateField('vehicleType', 'TRACTOR')
  }

  if (!props.modelValue.tonnage) {
    updateField('tonnage', '25톤')
  }

  loadCarriers()
})
</script>

<template>
  <section class="step-section">
    <div class="section-head">
      <p class="section-label">STEP 2</p>

      <h3>기사 및 본인 트랙터 정보</h3>

      <p>
        기사 정보와 가입을 신청할 운송사를 선택하고,
        본인이 운행할 트랙터 정보를 입력하세요.
      </p>
    </div>

    <div
      v-if="errorMessage"
      class="form-message error"
    >
      {{ errorMessage }}
    </div>

    <div class="subsection">
      <div class="subsection-title">
        <h4>기사 기본 정보</h4>

        <span class="status-pill">
          운송사 승인 대상
        </span>
      </div>

      <div class="form-grid">
        <div class="field">
          <label for="driverName">
            기사명
          </label>

          <input
            id="driverName"
            :value="modelValue.driverName"
            placeholder="기사명을 입력하세요"
            @input="
              updateField(
                'driverName',
                $event.target.value,
              )
            "
          />
        </div>

        <div class="field">
          <label for="driverContact">
            연락처
          </label>

          <input
            id="driverContact"
            :value="modelValue.driverContact"
            placeholder="010-0000-0000"
            @input="
              updateField(
                'driverContact',
                $event.target.value,
              )
            "
          />
        </div>

        <div class="field field-full">
          <label for="carrierId">
            가입 신청 운송사
          </label>

          <select
            id="carrierId"
            :value="modelValue.carrierId || ''"
            @change="
              updateField(
                'carrierId',
                Number($event.target.value),
              )
            "
          >
            <option
              disabled
              value=""
            >
              운송사를 선택하세요
            </option>

            <option
              v-for="carrier in approvedCarriers"
              :key="carrier.carrierId"
              :value="carrier.carrierId"
            >
              {{ carrier.carrierName }}
              /
              담당자
              {{ carrier.managerName || '-' }}
            </option>
          </select>
        </div>
      </div>

      <div
        v-if="loading"
        class="loading"
      >
        운송사 목록을 불러오는 중...
      </div>

      <div
        v-else-if="approvedCarriers.length === 0"
        class="empty-message"
      >
        승인된 운송사가 없습니다. 운송사 가입 승인 후
        기사 가입이 가능합니다.
      </div>
    </div>

    <div class="subsection">
      <div class="subsection-title">
        <h4>본인 트랙터 정보</h4>

        <span class="status-pill amber">
          관리자 최종 승인 대상
        </span>
      </div>

      <div class="tractor-guide">
        기사 회원가입 시에는 본인이 운행하는
        <b>트랙터</b>를 등록합니다. 운송사는 이후 별도로
        트레일러를 배정합니다.
      </div>

      <div class="form-grid">
        <div class="field">
          <label for="plateNumber">
            차량번호
          </label>

          <input
            id="plateNumber"
            :value="modelValue.plateNumber"
            placeholder="예: 부산80바1234"
            @input="
              updateField(
                'plateNumber',
                $event.target.value,
              )
            "
          />
        </div>

        <div class="field">
          <label for="vehicleType">
            차량종류
          </label>

          <input
            id="vehicleType"
            value="트랙터"
            disabled
          />
        </div>

        <div class="field">
          <label for="tonnage">
            톤수
          </label>

          <select
            id="tonnage"
            :value="modelValue.tonnage || '25톤'"
            @change="
              updateField(
                'tonnage',
                $event.target.value,
              )
            "
          >
            <option
              v-for="tonnage in tonnageOptions"
              :key="tonnage"
              :value="tonnage"
            >
              {{ tonnage }}
            </option>
          </select>
        </div>

        <div class="field">
          <label for="tractorNo">
            트랙터 관리번호
          </label>

          <div class="inline-field">
            <input
              id="tractorNo"
              :value="modelValue.tractorNo"
              placeholder="예: TR-123456"
              @input="
                updateField(
                  'tractorNo',
                  $event.target.value,
                )
              "
            />

            <button
              type="button"
              @click="generateTractorNo"
            >
              자동
            </button>
          </div>
        </div>

        <div class="field field-full">
          <label for="chassisNo">
            샤시번호
          </label>

          <div class="inline-field">
            <input
              id="chassisNo"
              :value="modelValue.chassisNo"
              placeholder="예: CH-123456"
              @input="
                updateField(
                  'chassisNo',
                  $event.target.value,
                )
              "
            />

            <button
              type="button"
              @click="generateChassisNo"
            >
              자동
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.step-section {
  display: grid;
  gap: 20px;
}

.section-head {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
}

.section-label {
  margin: 0;
  color: var(--blue-700);
  font-size: 12px;
  font-weight: 700;
}

.section-head h3 {
  margin: 4px 0;
}

.section-head p {
  margin: 0;
  color: var(--ink-500);
}

.subsection {
  display: grid;
  gap: 14px;
  padding: 14px;
  background: #f8fbfe;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.subsection-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.subsection-title h4 {
  margin: 0;
  color: var(--ink-900);
  font-size: 15px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.field {
  display: grid;
  gap: 6px;
}

.field-full {
  grid-column: 1 / -1;
}

.field label {
  font-weight: 700;
}

.field input,
.field select {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.field input:disabled {
  color: var(--ink-500);
  background: #edf2f7;
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
  width: 64px;
  color: #ffffff;
  background: var(--blue-700);
  border: 1px solid var(--blue-700);
  border-radius: 4px;
  font-weight: 700;
  cursor: pointer;
}

.tractor-guide {
  padding: 10px 12px;
  color: var(--ink-700);
  background: #eef6ff;
  border: 1px solid #bfd8f1;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.5;
}

.loading,
.empty-message {
  color: var(--ink-500);
  font-size: 13px;
}

.empty-message {
  padding: 10px 12px;
  background: #fff8e6;
  border: 1px solid #f3d28c;
  border-radius: 4px;
}

.form-message {
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 700;
}

.form-message.error {
  color: #991b1b;
  background: #fff1f1;
  border: 1px solid #fecaca;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .field-full {
    grid-column: auto;
  }

  .subsection-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .inline-field {
    flex-direction: column;
  }

  .inline-field button {
    width: 100%;
    min-height: 38px;
  }
}
</style>