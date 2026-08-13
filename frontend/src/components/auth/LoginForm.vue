<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginAccount, roleOptions } from '@/stores/authStore'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')

const loginForm = ref({
  username: 'admin',
  password: '1234',
  roleCode: 'ADMIN',
})

const submitLogin = async () => {
  errorMessage.value = ''
  loading.value = true

  try {
    const result = await loginAccount(loginForm.value)
    router.push(result.home)
  } catch (error) {
    errorMessage.value = error.message || '로그인에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <form class="login-form" @submit.prevent="submitLogin">
    <div class="form-head">
      <p class="section-label">LOGIN</p>
      <h2>로그인</h2>
      <p>역할을 선택하고 계정 정보를 입력하세요.</p>
    </div>

    <div class="field">
      <label for="loginRole">로그인 유형</label>
      <select id="loginRole" v-model="loginForm.roleCode">
        <option v-for="role in roleOptions" :key="role.code" :value="role.code">
          {{ role.label }}
        </option>
      </select>
    </div>

    <div class="field">
      <label for="loginUsername">아이디</label>
      <input
        id="loginUsername"
        v-model.trim="loginForm.username"
        autocomplete="username"
        required
      />
    </div>

    <div class="field">
      <label for="loginPassword">비밀번호</label>
      <input
        id="loginPassword"
        v-model="loginForm.password"
        autocomplete="current-password"
        required
        type="password"
      />
    </div>

    <div v-if="errorMessage" class="message error">
      {{ errorMessage }}
    </div>

    <button class="submit-button" type="submit" :disabled="loading">
      {{ loading ? '로그인 중...' : '로그인' }}
    </button>
  </form>
</template>

<style scoped>
.login-form {
  display: grid;
  gap: 18px;
  padding: 0;
  background: transparent;
}

.form-head {
  display: grid;
  gap: 6px;
  margin-bottom: 6px;
}

.section-label {
  margin: 0;
  color: var(--blue-700);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.form-head h2 {
  margin: 0;
  color: var(--ink-900);
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 0;
}

.form-head p {
  margin: 0;
  color: var(--ink-500);
  font-size: 14px;
  line-height: 1.45;
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

.field input,
.field select {
  width: 100%;
  height: 48px;
  padding: 0 14px;
  color: var(--ink-900);
  background: #ffffff;
  border: 1px solid #cbd7e3;
  border-radius: 8px;
  font-size: 14px;
}

.field input:focus,
.field select:focus {
  border-color: var(--blue-700);
  outline: none;
  box-shadow: 0 0 0 3px rgba(40, 103, 166, 0.12);
}

.message {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
}

.message.error {
  color: #8a1f1f;
  background: #fff1f1;
  border: 1px solid #f5b5b5;
}

.submit-button {
  min-height: 48px;
  margin-top: 4px;
  color: #ffffff;
  background: var(--blue-700);
  border: 1px solid var(--blue-700);
  border-radius: 8px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(31, 92, 150, 0.18);
}

.submit-button:disabled {
  cursor: wait;
  opacity: 0.65;
}
</style>
