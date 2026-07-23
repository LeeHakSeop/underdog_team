<script setup>
const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const vehicleTypes = ['트랙터', '트레일러', '카고', '윙바디', '탱크로리', '냉동탑', '기타']
const tonnageOptions = ['1톤', '2.5톤', '5톤', '8.5톤', '11톤', '18톤', '25톤']

const updateField = (key, value) => {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value,
  })
}

const generateCode = (prefix) => {
  const number = Math.floor(Math.random() * 900000 + 100000)
  return `${prefix}-${number}`
}
</script>

<template>
  <section class="step-section">
    <div class="section-head">
      <p class="section-label">STEP 3</p>
      <h3>차량 정보</h3>
      <p>게이트 출입에 사용할 차량 정보를 입력하세요.</p>
    </div>

    <div class="form-grid">
      <div class="field">
        <label>차량번호</label>
        <input
          placeholder="예: 부산80바1234"
          :value="modelValue.plateNumber"
          @input="updateField('plateNumber', $event.target.value)"
        />
      </div>

      <div class="field">
        <label>차량종류</label>
        <select
          :value="modelValue.vehicleType"
          @change="updateField('vehicleType', $event.target.value)"
        >
          <option v-for="type in vehicleTypes" :key="type" :value="type">
            {{ type }}
          </option>
        </select>
      </div>

      <div class="field">
        <label>톤수</label>
        <select
          :value="modelValue.tonnage"
          @change="updateField('tonnage', $event.target.value)"
        >
          <option v-for="ton in tonnageOptions" :key="ton" :value="ton">
            {{ ton }}
          </option>
        </select>
      </div>

      <div class="field">
        <label>트랙터 번호</label>
        <div class="inline-field">
          <input
            placeholder="예: TR-202601"
            :value="modelValue.tractorNo"
            @input="updateField('tractorNo', $event.target.value)"
          />
          <button type="button" @click="updateField('tractorNo', generateCode('TR'))">
            자동
          </button>
        </div>
      </div>

    </div>

    <div class="notice-box">
      <strong>차량 등록 상태</strong>
      <span>회원가입 완료 후 관리자의 승인 전까지 차량 상태는 승인대기로 저장됩니다.</span>
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
  font-weight: 800;
}

.section-head h3 {
  margin: 4px 0;
  font-size: 18px;
  font-weight: 800;
}

.section-head p {
  margin: 0;
  color: var(--ink-500);
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.field-full {
  grid-column: 1 / -1;
}

.field label {
  color: var(--ink-700);
  font-weight: 700;
  font-size: 13px;
}

.field input,
.field select {
  width: 100%;
  min-width: 0;
  height: 40px;
  padding: 0 12px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 4px;
  box-sizing: border-box;
}

.inline-field {
  display: flex;
  gap: 8px;
  min-width: 0;
}

.inline-field input {
  flex: 1;
  min-width: 0;
}

.inline-field button {
  width: 60px;
  flex-shrink: 0;
  color: #ffffff;
  background: var(--blue-700);
  border: 1px solid var(--blue-700);
  border-radius: 4px;
  font-weight: 700;
}

.notice-box {
  display: grid;
  gap: 4px;
  padding: 12px;
  background: #fff8e7;
  border: 1px solid #ffe0a6;
  border-radius: 4px;
}

.notice-box strong {
  color: #b7791f;
}

.notice-box span {
  color: var(--ink-500);
  font-size: 13px;
}

@media (max-width: 620px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .field-full {
    grid-column: auto;
  }

  .inline-field {
    flex-direction: column;
  }

  .inline-field button {
    width: 100%;
    min-height: 36px;
  }
}
</style>
