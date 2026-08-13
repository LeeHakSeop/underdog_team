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
  <div
    class="progress-wrap"
    :style="{ gridTemplateColumns: `repeat(${steps.length}, minmax(0, 1fr))` }"
  >
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
  gap: 0;
  width: min(100%, 520px);
  justify-self: center;
  margin: 2px auto 34px;
}

.progress-item {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 10px;
  color: #718396;
  font-size: 12px;
  font-weight: 800;
}

.circle {
  z-index: 2;
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  color: #5e7185;
  background: #f4f7fa;
  border: 1px solid #c7d3df;
  border-radius: 50%;
}

.progress-item.active .circle {
  color: #ffffff;
  background: var(--blue-700);
  border-color: var(--blue-700);
  box-shadow: 0 6px 14px rgba(31, 92, 150, 0.18);
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
  top: 15px;
  left: 50%;
  width: 100%;
  height: 1px;
  background: #c7d3df;
  transform: translateX(15px);
}

.line.done {
  background: #1f7a4d;
}

@media (max-width: 620px) {
  .progress-wrap {
    margin-bottom: 28px;
  }

  .label {
    font-size: 11px;
  }
}
</style>
