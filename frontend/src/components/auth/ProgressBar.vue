<script setup>
defineProps({
  currentStep: {
    type: Number,
    required: true,
  },
  steps: {
    type: Array,
    required: true,
  },
})
</script>

<template>
  <div class="progress-wrap">
    <div
      v-for="(step, index) in steps"
      :key="step.key"
      class="progress-item"
      :class="{
        active: currentStep === index + 1,
        done: currentStep > index + 1,
      }"
    >
      <div class="circle">
        <span v-if="currentStep > index + 1">✓</span>
        <span v-else>{{ index + 1 }}</span>
      </div>

      <div class="label">
        {{ step.label }}
      </div>

      <div
        v-if="index < steps.length - 1"
        class="line"
        :class="{ done: currentStep > index + 1 }"
      />
    </div>
  </div>
</template>

<style scoped>
.progress-wrap {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  margin: 12px 0 18px;
}

.progress-item {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 6px;
  color: #8796a7;
  font-size: 12px;
  font-weight: 700;
}

.circle {
  z-index: 2;
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  color: #6b7c8f;
  background: #eef3f8;
  border: 1px solid #cbd6e2;
  border-radius: 50%;
}

.progress-item.active .circle {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
}

.progress-item.done .circle {
  color: #ffffff;
  background: #1f7a4d;
  border-color: #1f7a4d;
}

.label {
  text-align: center;
  white-space: nowrap;
}

.progress-item.active .label {
  color: var(--blue-700);
}

.progress-item.done .label {
  color: #1f7a4d;
}

.line {
  position: absolute;
  top: 14px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: #cbd6e2;
  transform: translateX(14px);
}

.line.done {
  background: #1f7a4d;
}

@media (max-width: 620px) {
  .label {
    font-size: 11px;
  }

  .circle {
    width: 24px;
    height: 24px;
  }

  .line {
    top: 12px;
  }
}
</style>