<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
<<<<<<< HEAD
import { loginApi, registerApi } from '@/api/authApi'
=======
import { useCarrierStore } from '@/stores/carrierStore'
>>>>>>> origin/pjh

const router = useRouter()
const carrierStore = useCarrierStore()

const mode = ref('login')
const submitMessage = ref('')

const loginForm = ref({
  loginId: '',
  password: '',
})

const signupForm = ref({
  username: '',
  password: '',
  name: '',
  email: '',
  phone: '',
  roleCode: 'CARRIER',
  carrierName: '',
  businessNo: '',
  managerName: '',
  driverName: '',
  driverCarrierName: '',
  driverCanEnter: 'Y',
  adminArea: '전체 운영',
})

const roleOptions = [
  { code: 'CARRIER', label: '운송사 담당자', home: '/carrier/dashboard' },
  { code: 'DRIVER', label: '화물 기사', home: '/driver/dashboard' },
  { code: 'ADMIN', label: '관리자', home: '/admin/main' },
]

const selectedRole = computed(() =>
  roleOptions.find((role) => role.code === signupForm.value.roleCode)
)

const login = () => {
  const role =
    roleOptions.find((item) => item.code === loginForm.value.roleCode) ||
    roleOptions[2]

  localStorage.setItem(
    'portGateUser',
    JSON.stringify({
      username: loginForm.value.username,
      roleCode: role.code,
      roleName: role.label,
    }),
  )

  router.push(role.home)
}

const saveLoginUser = (role) => {
  localStorage.setItem(
    'portGateUser',
    JSON.stringify({
      username: signupForm.value.username,
      name: signupForm.value.name,
      roleCode: role.code,
      roleName: role.label,
    }),
  )
}

<<<<<<< HEAD
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
=======
const signup = async () => {
  const role =
    roleOptions.find((item) => item.code === signupForm.value.roleCode) ||
    roleOptions[0]
>>>>>>> origin/pjh

  submitMessage.value = ''

  try {
    if (role.code === 'CARRIER') {
      await carrierStore.addCarrier({
        carrierName: signupForm.value.carrierName || signupForm.value.name,
        carrierContact: signupForm.value.phone,
        managerName: signupForm.value.managerName || signupForm.value.name,
        carrierStatus: 'ACTIVE',
      })
    }

    saveLoginUser(role)
    router.push(role.home)
  } catch (error) {
    submitMessage.value =
      carrierStore.error || '회원가입 처리 중 오류가 발생했습니다.'
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
            <p>역할을 선택하면 해당 업무 화면으로 이동합니다.</p>
          </div>

        
          <div class="field">
<<<<<<< HEAD
            <label for="loginUsername">아이디</label>
            <input id="loginUsername" v-model="loginForm.loginId" autocomplete="username" />
            <p v-if="submitMessage" class="form-message">{{ submitMessage }}</p>
=======
            <label for="loginUsername">사용자 ID</label>
            <input id="loginUsername" v-model="loginForm.username" autocomplete="username" />
>>>>>>> origin/pjh
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
            <p>역할과 DB 기준 정보를 입력합니다.</p>
          </div>

          <div class="form-grid">
            <div class="field">
              <label for="signupRole">역할</label>
              <select id="signupRole" v-model="signupForm.roleCode">
                <option v-for="role in roleOptions" :key="role.code" :value="role.code">
                  {{ role.label }}
                </option>
              </select>
            </div>
            <div class="field">
              <label for="signupUsername">사용자 ID</label>
              <input id="signupUsername" v-model="signupForm.username" required />
            </div>
            <div class="field">
              <label for="signupPassword">비밀번호</label>
              <input id="signupPassword" v-model="signupForm.password" required type="password" />
            </div>
            <div class="field">
              <label for="signupName">이름</label>
              <input id="signupName" v-model="signupForm.name" required />
            </div>
            <div class="field">
              <label for="signupEmail">이메일</label>
              <input id="signupEmail" v-model="signupForm.email" type="email" />
            </div>
            <div class="field">
              <label for="signupPhone">연락처</label>
              <input id="signupPhone" v-model="signupForm.phone" />
            </div>
          </div>

          <div class="role-fields">
            <h3>{{ selectedRole?.label }} 추가 정보</h3>

            <div v-if="signupForm.roleCode === 'CARRIER'" class="form-grid">
              <div class="field">
                <label for="carrierName">운송사명</label>
                <input id="carrierName" v-model="signupForm.carrierName" required />
              </div>
              <div class="field">
                <label for="businessNo">사업자번호</label>
                <input id="businessNo" v-model="signupForm.businessNo" />
              </div>
              <div class="field">
                <label for="managerName">담당자명</label>
                <input id="managerName" v-model="signupForm.managerName" />
              </div>
            </div>

            <div v-else-if="signupForm.roleCode === 'DRIVER'" class="form-grid">
              <div class="field">
                <label for="driverName">기사명</label>
                <input id="driverName" v-model="signupForm.driverName" />
              </div>
              <div class="field">
                <label for="driverCarrierName">소속 운송사</label>
                <input id="driverCarrierName" v-model="signupForm.driverCarrierName" />
              </div>
              <div class="field">
                <label for="driverCanEnter">출입 가능 여부</label>
                <select id="driverCanEnter" v-model="signupForm.driverCanEnter">
                  <option value="Y">Y - 가능</option>
                  <option value="N">N - 제한</option>
                </select>
              </div>
            </div>

            <div v-else class="form-grid">
              <div class="field">
                <label for="adminArea">관리 영역</label>
                <input id="adminArea" v-model="signupForm.adminArea" />
              </div>
            </div>
          </div>

          <p v-if="submitMessage" class="form-message">{{ submitMessage }}</p>
<<<<<<< HEAD
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
=======
          <button class="submit-button" :disabled="carrierStore.loading" type="submit">
            {{ carrierStore.loading ? '처리 중' : '회원가입 후 시작' }}
          </button>
>>>>>>> origin/pjh
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

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>