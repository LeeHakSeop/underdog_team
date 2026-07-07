<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginApi, registerApi } from '@/api/authApi'

const router = useRouter()

const mode = ref('login')
const submitMessage = ref('')

const loginForm = ref({
  loginId: '',
  password: '',
})

const signupRole = ref('CARRIER')
const carrierForm = ref({
  carrier_name: '',
  carrier_contact: '',
  manager_name: '',
  carrier_status: 'PENDING',
})

const driverForm = ref({
  driver_name: '',
  driver_contact: '',
  is_registered: false,
  carrier_id: 1,
  can_enter: false,
})

const roleOptions = [
  { code: 'CARRIER', label: '운송사 담당자', home: '/carrier/dashboard' },
  { code: 'DRIVER', label: '화물 기사', home: '/driver/dashboard' },
  { code: 'ADMIN', label: '관리자', home: '/admin/main' },
]

const selectedRole = computed(() => roleOptions.find((role) => role.code === signupRole.value))

const readRows = (key) => JSON.parse(localStorage.getItem(key) || '[]')
const writeRows = (key, rows) => localStorage.setItem(key, JSON.stringify(rows))

const nextId = (rows, idKey) => {
  return rows.reduce((max, row) => Math.max(max, Number(row[idKey]) || 0), 0) + 1
}

const saveLoginUser = (role, username) => {
  localStorage.setItem(
    'portGateUser',
    JSON.stringify({
      username,
      roleCode: role.code,
      roleName: role.label,
    }),
  )
}

const registerForm = ref({
  loginId: '',
  password: '',
  userName: '',
  roleCode: 'CARRIER',
})

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
  } catch (error) {
    submitMessage.value = error.message || '회원가입에 실패했습니다.'
  }
}



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

const signup = () => {
  const role = selectedRole.value || roleOptions[0]
  submitMessage.value = ''

  if (role.code === 'CARRIER') {
    const rows = readRows('portGateCarrierSignups')
    rows.push({
      carrier_id: nextId(rows, 'carrier_id'),
      carrier_name: carrierForm.value.carrier_name,
      carrier_contact: carrierForm.value.carrier_contact,
      manager_name: carrierForm.value.manager_name,
      carrier_status: 'PENDING',
    })
    writeRows('portGateCarrierSignups', rows)
    submitMessage.value = '운송사 가입 정보가 저장되었습니다. 관리자 승인이 필요합니다.'
    return
  }

  if (role.code === 'DRIVER') {
    const rows = readRows('portGateDriverSignups')
    rows.push({
      driver_id: nextId(rows, 'driver_id'),
      driver_name: driverForm.value.driver_name,
      driver_contact: driverForm.value.driver_contact,
      is_registered: false,
      carrier_id: Number(driverForm.value.carrier_id),
      can_enter: false,
    })
    writeRows('portGateDriverSignups', rows)
    submitMessage.value = '기사 가입 정보가 저장되었습니다. 관리자 승인이 필요합니다.'
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="brand-panel">
      <p class="eyebrow">항만 게이트 시스템</p>
      <h1>차량 출입 및 컨테이너 상차 섹터 안내</h1>
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
            <p>역할을 선택하면 해당 업무 화면으로 이동합니다.</p>
          </div>

        
          <div class="field">
            <label for="loginUsername">아이디</label>
            <input id="loginUsername" v-model="loginForm.loginId" autocomplete="username" />
            <p v-if="submitMessage" class="form-message">{{ submitMessage }}</p>
          </div>
          <div class="field">
            <label for="loginPassword">비밀번호</label>
            <input
              id="loginPassword"
              v-model="loginForm.password"
              autocomplete="current-password"
              type="password"
            />
          </div>

          <button class="submit-button" type="submit">로그인</button>
        </form>

        <form v-else class="auth-form" @submit.prevent="register">
          <!-- <div class="form-head">
            <h2>회원가입</h2>
            <p>가입 정보는 DB 구조에 맞춰 저장되고, 승인과 권한은 관리자가 결정합니다.</p>
          </div>

          <div class="field">
            <label for="signupRole">가입 유형</label>
            <select id="signupRole" v-model="signupRole">
              <option value="CARRIER">운송사</option>
              <option value="DRIVER">화물 기사</option>
            </select>
          </div>

          <div v-if="signupRole === 'CARRIER'" class="role-fields">
            <h3>운송사 정보</h3>
            <div class="form-grid">
              <div class="field">
                <label for="carrierName">운송사명</label>
                <input id="carrierName" v-model="carrierForm.carrier_name" required />
              </div>
              <div class="field">
                <label for="carrierContact">연락처</label>
                <input id="carrierContact" v-model="carrierForm.carrier_contact" />
              </div>
              <div class="field">
                <label for="managerName">담당자명</label>
                <input id="managerName" v-model="carrierForm.manager_name" />
              </div>
              <div class="field">
                <label for="carrierStatus">가입 상태</label>
                <input id="carrierStatus" v-model="carrierForm.carrier_status" disabled />
              </div>
            </div>
          </div>

          <div v-else class="role-fields">
            <h3>기사 정보</h3>
            <div class="form-grid">
              <div class="field">
                <label for="driverName">기사명</label>
                <input id="driverName" v-model="driverForm.driver_name" required />
              </div>
              <div class="field">
                <label for="driverContact">연락처</label>
                <input id="driverContact" v-model="driverForm.driver_contact" />
              </div>
              <div class="field">
                <label for="carrierId">소속 운송사 ID</label>
                <input id="carrierId" v-model.number="driverForm.carrier_id" min="1" type="number" />
              </div>
              <div class="field">
                <label for="isRegistered">등록 승인 여부</label>
                <input id="isRegistered" :value="driverForm.is_registered" disabled />
              </div>
              <div class="field">
                <label for="canEnter">출입 가능 여부</label>
                <input id="canEnter" :value="driverForm.can_enter" disabled />
              </div>
            </div>
          </div>

          <p v-if="submitMessage" class="form-message">{{ submitMessage }}</p>
          <button class="submit-button" type="submit">가입 정보 저장</button> -->
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
            <input id="registerPassword" v-model="registerForm.password" type="password" required />
          </div>

          <div class="field">
            <label for="registerUserName">이름</label>
            <input id="registerUserName" v-model="registerForm.userName" required />
          </div>

          <div class="field">
            <label for="registerRole">가입 유형</label>
            <select id="registerRole" v-model="registerForm.roleCode">
              <option value="CARRIER">운송사</option>
              <option value="DRIVER">화물 기사</option>
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
}

.auth-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  background: #e7edf3;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.auth-tabs button {
  min-height: 32px;
  color: var(--ink-500);
  background: transparent;
  border: 0;
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

.role-fields {
  display: grid;
  gap: 10px;
  padding: 10px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.role-fields h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
}

.form-message {
  margin: 0;
  padding: 8px 10px;
  color: #173b60;
  background: #eef7ff;
  border: 1px solid #bdd2ed;
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

@media (max-width: 980px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: 300px;
  }
}

@media (max-width: 620px) {
  .auth-panel,
  .brand-panel {
    padding: 22px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
