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
  margin: 10px 0 16px;
}

.progress-item {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 6px;
  color: #6f8194;
  font-size: 13px;
  font-weight: 800;
}

.circle {
  z-index: 2;
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  color: #5e7185;
  background: #eef3f8;
  border: 1px solid #c4d1de;
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
  top: 15px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: #c4d1de;
  transform: translateX(15px);
}

.line.done {
  background: #1f7a4d;
}

@media (max-height: 760px) and (min-width: 1100px) {
  .progress-wrap {
    margin: 8px 0 12px;
  }
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
    transform: translateX(12px);
  }
}
</style>
