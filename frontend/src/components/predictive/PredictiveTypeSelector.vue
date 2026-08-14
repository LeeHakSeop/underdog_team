<script setup>
import { predictiveMaintenanceTypes } from '@/config/predictiveMaintenance'

defineProps({
  modelValue: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <section class="type-selector" aria-label="예지보전 설비 유형 선택">
    <div>
      <strong>예지보전 유형</strong>
      <span>확인할 설비 유형을 선택하세요.</span>
    </div>

    <div class="type-buttons">
      <button
        v-for="type in predictiveMaintenanceTypes"
        :key="type.code"
        type="button"
        class="type-button"
        :class="{ active: modelValue === type.code }"
        :disabled="!type.enabled"
        :title="type.description"
        @click="emit('update:modelValue', type.code)"
      >
        {{ type.label }}
        <small v-if="!type.enabled">준비 중</small>
      </button>
    </div>
  </section>
</template>

<style scoped>
.type-selector {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px;
  background: #ffffff;
  border: 1px solid var(--line);
}

.type-selector strong,
.type-selector span {
  display: block;
}

.type-selector strong {
  color: #20364f;
  font-size: 14px;
}

.type-selector span {
  margin-top: 2px;
  color: var(--ink-500);
  font-size: 11px;
}

.type-buttons {
  display: flex;
  gap: 6px;
}

.type-button {
  min-height: 34px;
  padding: 6px 12px;
  color: #29445f;
  background: #edf3f8;
  border: 1px solid #9fb0c0;
  font-weight: 700;
}

.type-button.active {
  color: #ffffff;
  background: var(--blue-700);
  border-color: #174c79;
}

.type-button:disabled {
  color: #7b8794;
  cursor: not-allowed;
  background: #f1f2f4;
  border-style: dashed;
}

.type-button small {
  display: inline;
  margin-left: 5px;
  color: inherit;
}

@media (max-width: 700px) {
  .type-selector {
    align-items: stretch;
    flex-direction: column;
  }

  .type-buttons,
  .type-button {
    width: 100%;
  }
}
</style>
