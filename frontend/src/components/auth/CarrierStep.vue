<script setup>
const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const updateField = (key, value) => {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value,
  })
}
</script>

<template>
  <section class="step-section">
    <div class="section-head">
      <p class="section-label">STEP 2</p>
      <h3>운송사 정보</h3>
      <p>기사와 차량을 관리할 운송사 정보를 입력하세요.</p>
    </div>

    <div class="form-grid">
      <div class="field">
        <label for="carrierName">운송사명</label>
        <input
          id="carrierName"
          :value="modelValue.carrierName"
          @input="updateField('carrierName', $event.target.value)"
        />
      </div>

      <div class="field">
        <label for="managerName">담당자명</label>
        <input
          id="managerName"
          :value="modelValue.managerName"
          @input="updateField('managerName', $event.target.value)"
        />
      </div>

      <div class="field">
        <label for="carrierContact">연락처</label>
        <input
          id="carrierContact"
          placeholder="010-0000-0000"
          :value="modelValue.carrierContact"
          @input="updateField('carrierContact', $event.target.value)"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.step-section {
  display: grid;
  gap: 22px;
}

.section-head {
  display: grid;
  gap: 6px;
}

.section-label {
  margin: 0;
  color: var(--blue-700);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.section-head h3 {
  margin: 0;
  color: var(--ink-900);
  font-size: 24px;
  font-weight: 800;
}

.section-head p {
  margin: 0;
  color: var(--ink-500);
  font-size: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.field {
  display: grid;
  gap: 7px;
}

.field label {
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 800;
}

.field input {
  height: 48px;
  padding: 0 14px;
  background: #ffffff;
  border: 1px solid #cbd7e3;
  border-radius: 8px;
}

.field input:focus {
  border-color: var(--blue-700);
  outline: none;
  box-shadow: 0 0 0 3px rgba(40, 103, 166, 0.12);
}
</style>
