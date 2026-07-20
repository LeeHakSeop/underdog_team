<script setup>
const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  signupRole: {
    type: String,
    required: true,
  },
  loginIdCheck: {
    type: Object,
    required: true,
  },
  checkingLoginId: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:modelValue',
  'update:signupRole',
  'check-login-id',
])

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
      <p class="section-label">STEP 1</p>
      <h3>계정 정보</h3>
      <p>로그인에 사용할 계정과 가입 유형을 입력하세요.</p>
    </div>

    <div class="role-choice">
      <button
        type="button"
        :class="{ active: signupRole === 'CARRIER' }"
        @click="emit('update:signupRole', 'CARRIER')"
      >
        <strong>운송사</strong>
        <span>운송사 담당자 계정</span>
      </button>

      <button
        type="button"
        :class="{ active: signupRole === 'DRIVER' }"
        @click="emit('update:signupRole', 'DRIVER')"
      >
        <strong>화물 기사</strong>
        <span>기사 및 차량 등록</span>
      </button>
    </div>

    <div class="form-grid">
      <div class="field username-field">
        <label for="signupUsername">아이디</label>
        <div
          class="username-row"
          :class="{
            checked: loginIdCheck.available === true,
            invalid: loginIdCheck.available === false,
          }"
        >
          <input
            id="signupUsername"
            :value="modelValue.username"
            autocomplete="username"
            required
            @input="updateField('username', $event.target.value)"
          />
          <button
            type="button"
            class="check-button"
            :disabled="checkingLoginId"
            @click="emit('check-login-id')"
          >
            {{ checkingLoginId ? '확인 중' : '확인' }}
          </button>
        </div>
        <p
          v-if="loginIdCheck.message"
          class="check-message"
          :class="{ available: loginIdCheck.available, unavailable: loginIdCheck.available === false }"
        >
          {{ loginIdCheck.message }}
        </p>
      </div>

      <div class="field">
        <label for="signupPassword">비밀번호</label>
        <input
          id="signupPassword"
          :value="modelValue.password"
          autocomplete="new-password"
          required
          type="password"
          @input="updateField('password', $event.target.value)"
        />
      </div>

      <div class="field">
        <label for="signupPasswordConfirm">비밀번호 확인</label>
        <input
          id="signupPasswordConfirm"
          :value="modelValue.passwordConfirm"
          autocomplete="new-password"
          required
          type="password"
          @input="updateField('passwordConfirm', $event.target.value)"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.step-section {
  display: grid;
  gap: 14px;
}

.section-head {
  display: grid;
  gap: 4px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line);
}

.section-label {
  margin: 0;
  color: var(--blue-700);
  font-size: 12px;
  font-weight: 800;
}

.section-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.section-head p {
  margin: 0;
  color: var(--ink-500);
  font-size: 13px;
}

.role-choice {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.role-choice button {
  display: grid;
  gap: 4px;
  min-height: 70px;
  padding: 12px;
  text-align: left;
  background: #f7fafe;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.role-choice button.active {
  background: #eef6ff;
  border-color: var(--blue-700);
  box-shadow: inset 0 0 0 1px var(--blue-700);
}

.role-choice strong {
  color: var(--ink-900);
  font-size: 14px;
}

.role-choice span {
  color: var(--ink-500);
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.field {
  display: grid;
  gap: 5px;
}

.username-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 72px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 3px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.username-row:focus-within {
  border-color: var(--blue-700);
  box-shadow: 0 0 0 2px rgba(40, 103, 166, 0.12);
}

.username-row.checked {
  border-color: #2f9e62;
}

.username-row.invalid {
  border-color: #d94a4a;
}

.field label {
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 700;
}

.field input {
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.username-row input {
  border: 0;
  border-radius: 0;
}

.username-row input:focus {
  outline: none;
}

.check-button {
  min-width: 0;
  min-height: 34px;
  padding: 0 10px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  background: var(--blue-700);
  border: 0;
  border-left: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 0;
  cursor: pointer;
}

.check-button:hover {
  background: #1d4e89;
}

.check-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.check-message {
  min-height: 16px;
  margin: 1px 0 0;
  font-size: 12px;
  font-weight: 700;
}

.check-message.available {
  color: #155e3b;
}

.check-message.unavailable {
  color: #8a1f1f;
}

@media (max-width: 620px) {
  .role-choice,
  .form-grid,
  .username-row {
    grid-template-columns: 1fr;
  }

  .check-button {
    width: 100%;
  }
}
</style>
