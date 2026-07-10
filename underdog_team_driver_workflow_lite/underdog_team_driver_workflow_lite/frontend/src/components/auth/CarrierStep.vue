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
      <p>운송사 기본 정보를 입력하세요.</p>
    </div>

    <div class="form-grid">
      <div class="field">
        <label>운송사명</label>
        <input
          :value="modelValue.carrierName"
          @input="updateField('carrierName', $event.target.value)"
        />
      </div>

      <div class="field">
        <label>담당자명</label>
        <input
          :value="modelValue.managerName"
          @input="updateField('managerName', $event.target.value)"
        />
      </div>

      <div class="field field-full">
        <label>연락처</label>
        <input
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
  gap: 18px;
}

.section-head {
  border-bottom: 1px solid var(--line);
  padding-bottom: 12px;
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

.field label {
  font-weight: 700;
}

.field input {
  height: 40px;
  border: 1px solid var(--line);
  border-radius: 4px;
  padding: 0 12px;
}

.field-full {
  grid-column: 1 / -1;
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