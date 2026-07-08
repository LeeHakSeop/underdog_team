<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginApi, registerApi } from '@/api/authApi'

const router = useRouter()

const mode = ref('login')
const submitMessage = ref('')

const loginForm = ref({
  loginId: '',
  password: '',
})

const registerForm = ref({
  loginId: '',
  password: '',
  userName: '',
  roleCode: 'CARRIER',
})

const roleOptions = [
  { code: 'CARRIER', label: '운송사 담당자', home: '/carrier/dashboard' },
  { code: 'DRIVER', label: '화물 기사', home: '/driver/dashboard' },
  { code: 'ADMIN', label: '관리자', home: '/admin/main' },
]

const login = async () => {
  submitMessage.value = ''

  try {
    const user = await loginApi(loginForm.value)

    localStorage.setItem(
      'portGateUser',
      JSON.stringify({
        userId: user.userId,
        loginId: user.loginId,
        userName: user.userName,
        roleCode: user.roleCode,
      }),
    )

    if (user.token) {
      localStorage.setItem('token', user.token)
    }

    const role = roleOptions.find((item) => item.code === user.roleCode)

    if (role) {
      router.push(role.home)
    } else {
      router.push('/login')
    }
  } catch (error) {
    submitMessage.value = error.message || '로그인에 실패했습니다.'
  }
}

const register = async () => {
  submitMessage.value = ''

  try {
    await registerApi(registerForm.value)

    submitMessage.value = '회원가입이 완료되었습니다. 로그인해주세요.'

    loginForm.value.loginId = registerForm.value.loginId
    loginForm.value.password = registerForm.value.password

    registerForm.value = {
      loginId: '',
      password: '',
      userName: '',
      roleCode: 'CARRIER',
    }

    mode.value = 'login'
  } catch (error) {
    submitMessage.value = error.message || '회원가입에 실패했습니다.'
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="brand-panel">
      <p class="eyebrow">Port Gate System</p>
      <h1>
        항만 게이트 차량 출입 및<br />
        컨테이너 상차 섹터 안내 시스템
      </h1>
    </section>

    <section class="auth-panel">
      <div class="auth-card">
        <div class="auth-tabs">
          <button :class="{ active: mode === 'login' }" type="button" @click="mode = 'login'">
            로그인
          </button>
          <button :class="{ active: mode === 'signup' }" type="button" @click="mode = 'signup'">
            회원가입
          </button>
        </div>

        <form v-if="mode === 'login'" class="auth-form" @submit.prevent="login">
          <div class="form-head">
            <h2>로그인</h2>
            <p>계정 정보를 입력하면 해당 업무 화면으로 이동합니다.</p>
          </div>

          <div class="field">
            <label for="loginId">아이디</label>
            <input id="loginId" v-model="loginForm.loginId" autocomplete="username" required />
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

          <p v-if="submitMessage" class="form-message">{{ submitMessage }}</p>
          <button class="submit-button" type="submit">로그인</button>
        </form>

        <form v-else class="auth-form" @submit.prevent="register">
          <div class="form-head">
            <h2>회원가입</h2>
            <p>기본 계정을 생성합니다.</p>
          </div>

          <div class="field">
            <label for="registerLoginId">아이디</label>
            <input id="registerLoginId" v-model="registerForm.loginId" required />
          </div>

          <div class="field">
            <label for="registerPassword">비밀번호</label>
            <input id="registerPassword" v-model="registerForm.password" required type="password" />
          </div>

          <div class="field">
            <label for="registerUserName">이름</label>
            <input id="registerUserName" v-model="registerForm.userName" required />
          </div>

          <div class="field">
            <label for="registerRole">가입 유형</label>
            <select id="registerRole" v-model="registerForm.roleCode">
              <option value="CARRIER">운송사 담당자</option>
              <option value="DRIVER">화물 기사</option>
              <option value="ADMIN">관리자</option>
            </select>
          </div>

          <p v-if="submitMessage" class="form-message">{{ submitMessage }}</p>
          <button class="submit-button" type="submit">회원가입</button>
        </form>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.95fr);
  background: #dfe6ee;
}

.brand-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  padding: clamp(32px, 6vw, 70px);
  color: #ffffff;
  background: #26384d;
  border-right: 1px solid #172636;
}

.eyebrow {
  margin: 0;
  color: #c9d6e2;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0;
}

.brand-panel h1 {
  max-width: 680px;
  margin: 0;
  font-size: clamp(28px, 4vw, 46px);
  font-weight: 700;
  line-height: 1.22;
}

.auth-panel {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  padding: 26px;
}

.auth-card {
  width: min(100%, 540px);
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 2px;
  box-shadow: none;
}

.auth-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
  padding: 0;
  background: #e7edf3;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.auth-tabs button {
  min-height: 32px;
  color: var(--ink-500);
  background: transparent;
  border: 0;
  border-radius: 0;
  font-weight: 700;
}

.auth-tabs button.active {
  color: #ffffff;
  background: var(--blue-700);
}

.auth-form {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.form-head h2 {
  margin: 0 0 4px;
  font-size: 19px;
  font-weight: 700;
}

.form-head p {
  margin: 0;
  color: var(--ink-500);
  line-height: 1.5;
}

.form-message {
  margin: 0;
  padding: 8px 10px;
  color: #9f1d1d;
  background: #fff2f2;
  border: 1px solid #e6b8b8;
  font-size: 13px;
}

.submit-button {
  min-height: 34px;
  color: #ffffff;
  background: var(--blue-700);
  border: 1px solid var(--blue-700);
  border-radius: 2px;
  font-weight: 700;
}

.submit-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

@media (max-width: 980px) {
  .auth-page {
    grid-template-columns: 1fr;
    background: #eef4fb;
  }

  .brand-panel {
    min-height: 360px;
    padding-bottom: 24px;
    background: #26384d;
  }

  .auth-panel {
    padding-top: 20px;
  }
}

@media (max-width: 620px) {
  .auth-panel,
  .brand-panel {
    padding: 22px;
  }
}
</style>
