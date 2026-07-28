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

const approvedCarriers = computed(() =>
  carriers.value.filter((carrier) =>
    carrier.carrierStatus === 'APPROVED' || carrier.carrierStatus === 'ACTIVE',
  ),
)

const updateField = (key, value) => {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value,
  })
}

const loadCarriers = async () => {
  loading.value = true

  try {
    carriers.value = await fetchCarriers()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(loadCarriers)
</script>

<template>
  <section class="step-section">
    <div class="section-head">
      <p class="section-label">STEP 2</p>
      <h3>기사 정보</h3>
      <p>기사 정보와 가입을 요청할 운송사를 선택하세요.</p>
    </div>

    <div class="form-grid">
      <div class="field">
        <label>기사명</label>
        <input
          :value="modelValue.driverName"
          @input="updateField('driverName', $event.target.value)"
        />
      </div>

      <div class="field">
        <label>연락처</label>
        <input
          placeholder="010-0000-0000"
          :value="modelValue.driverContact"
          @input="updateField('driverContact', $event.target.value)"
        />
      </div>

      <div class="field field-full">
        <label>가입 요청 운송사</label>

        <select
          :value="modelValue.carrierId || ''"
          @change="updateField('carrierId', Number($event.target.value))"
        >
          <option disabled value="">
            운송사를 선택하세요.
          </option>

          <option
            v-for="carrier in approvedCarriers"
            :key="carrier.carrierId"
            :value="carrier.carrierId"
          >
            {{ carrier.carrierName }} / 담당자 {{ carrier.managerName || '-' }}
          </option>
        </select>
      </div>

      <div class="field field-full">
        <label>트랙터 차량번호</label>
        <input
          placeholder="예) 부산80바1234"
          :value="modelValue.plateNumber"
          @input="updateField('plateNumber', $event.target.value)"
        />
      </div>

    </div>

    <div v-if="loading" class="loading">
      운송사 목록을 불러오는 중...
    </div>

    <div v-else-if="approvedCarriers.length === 0" class="empty-message">
      승인된 운송사가 없습니다. 운송사 가입 승인 후 기사 가입이 가능합니다.
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
  color: var(--blue-700);
  font-size: 12px;
  font-weight: 700;
}

.section-head h3 {
  margin: 4px 0;
}

.section-head p {
  color: var(--ink-500);
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
  height: 40px;
  padding: 0 12px;
  background: white;
  border: 1px solid var(--line);
  border-radius: 4px;
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

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .field-full {
    grid-column: auto;
  }

}
</style>
