<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginForm from '@/components/auth/LoginForm.vue'
import SignupWizard from '@/components/auth/SignupWizard.vue'

const route = useRoute()
const router = useRouter()

const mode = ref(route.query.mode === 'signup' ? 'signup' : 'login')
const completeMessage = ref('')

const setMode = (nextMode) => {
  completeMessage.value = ''
  mode.value = nextMode

  router.replace({
    path: '/login',
    query: nextMode === 'signup' ? { mode: 'signup' } : {},
  })
}

const goLogin = () => {
  setMode('login')
}

const goSignup = () => {
  setMode('signup')
}

const handleSignupCompleted = () => {
  completeMessage.value = '회원가입 신청이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다.'
  setMode('login')
}
</script>

<template>
  <main class="auth-page" :class="{ 'signup-mode': mode === 'signup' }">
    <section class="brand-panel">
      <p class="eyebrow">PORT GATE MANAGEMENT SYSTEM</p>

      <h1>
        항만 게이트<br />
        차량 출입 관리
      </h1>

      <p class="brand-copy">
        운송사, 기사, 차량 승인과 컨테이너 상차 섹터 안내를 한 화면에서 관리합니다.
      </p>

      <div class="brand-features">
        <span>운송사 관리</span>
        <span>기사 등록</span>
        <span>차량 승인</span>
        <span>게이트 출입</span>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-card">
        <div class="auth-tabs">
          <button :class="{ active: mode === 'login' }" type="button" @click="goLogin">
            로그인
          </button>
          <button :class="{ active: mode === 'signup' }" type="button" @click="goSignup">
            회원가입
          </button>
        </div>

        <p v-if="completeMessage" class="complete-message">
          {{ completeMessage }}
        </p>

        <LoginForm v-if="mode === 'login'" />

        <SignupWizard v-else @completed="handleSignupCompleted" />
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(400px, 0.82fr) minmax(620px, 1fr);
  background: #dfe6ee;
}

.brand-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
  padding: clamp(34px, 5vw, 64px);
  color: #ffffff;
  background:
    linear-gradient(135deg, rgba(30, 58, 95, 0.96), rgba(22, 39, 58, 0.98)),
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.16), transparent 30%);
  border-right: 1px solid #172636;
}

.eyebrow {
  margin: 0;
  color: #c9d6e2;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.brand-panel h1 {
  max-width: 540px;
  margin: 0;
  font-size: clamp(38px, 3.6vw, 50px);
  font-weight: 800;
  line-height: 1.18;
}

.brand-copy {
  max-width: 500px;
  margin: 0;
  color: #dbe7f3;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.55;
}

.brand-features {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-width: 600px;
}

.brand-features span {
  padding: 7px 10px;
  color: #dbeafe;
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(219, 234, 254, 0.24);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.auth-panel {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  padding: 20px 26px;
}

.auth-card {
  width: min(100%, 920px);
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.auth-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  overflow: hidden;
  background: #e7edf3;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.auth-tabs button {
  min-height: 38px;
  color: var(--ink-500);
  background: transparent;
  border: 0;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

.auth-tabs button.active {
  color: #ffffff;
  background: var(--blue-700);
}

.complete-message {
  margin: 12px 0 0;
  padding: 10px 12px;
  color: #155e3b;
  background: #ecfdf3;
  border: 1px solid #b7ebc9;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 700;
}

@media (max-height: 760px) and (min-width: 1100px) {
  .auth-page {
    grid-template-columns: minmax(360px, 0.76fr) minmax(640px, 1fr);
  }

  .auth-page.signup-mode {
    grid-template-columns: 1fr;
  }

  .auth-page.signup-mode .brand-panel {
    display: none;
  }

  .brand-panel {
    gap: 14px;
    padding: 42px;
  }

  .brand-panel h1 {
    font-size: 42px;
  }

  .auth-page.signup-mode .auth-card {
    width: min(100%, 980px);
  }

  .auth-panel {
    padding: 16px 24px;
  }
}

@media (max-width: 1080px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: 280px;
  }

  .auth-panel {
    align-items: flex-start;
  }
}

@media (max-width: 620px) {
  .auth-panel,
  .brand-panel {
    padding: 22px;
  }
}
</style>
