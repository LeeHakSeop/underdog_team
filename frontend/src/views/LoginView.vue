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
      <div class="brand-message">
      <p class="eyebrow">PORT GATE MANAGEMENT SYSTEM</p>

      <h1>
        <span>항만 게이트 차량 출입</span>
        <strong>컨테이너 상차 섹터 안내 시스템</strong>
      </h1>
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
  height: 100dvh;
  grid-template-columns: 6fr 4fr;
  overflow: hidden;
  background: #dfe6ee;
}

.brand-panel {
  display: flex;
  position: relative;
  container-type: inline-size;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
  padding: clamp(34px, 5vw, 64px);
  color: #ffffff;
  overflow: hidden;
  background: #1f4e7b;
  border-right: 1px solid #172636;
}

.brand-panel::before {
  position: absolute;
  inset: 0;
  z-index: 0;
  content: '';
  background: url('/images/gamman-pier.jpg') center / cover no-repeat;
  transform: scaleX(-1);
}

.brand-message {
  position: relative;
  z-index: 1;
  width: max-content;
  max-width: none;
  align-self: flex-start;
  padding: 18px 30px;
  background: rgba(255, 255, 255, 0.72);
  border-radius: 8px;
  box-shadow: 0 18px 42px rgba(10, 25, 40, 0.24);
  backdrop-filter: blur(8px);
  transform: none;
}

.eyebrow {
  margin: 0;
  color: var(--blue-700);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.brand-panel h1 {
  width: max-content;
  max-width: none;
  margin: 0;
  line-height: 1.16;
}

.brand-panel h1 span,
.brand-panel h1 strong {
  display: block;
}

.brand-panel h1 span {
  color: var(--ink-700);
  font-size: clamp(19px, 2vw, 24px);
  font-weight: 700;
  letter-spacing: -0.03em;
}

.brand-panel h1 strong {
  margin-top: 8px;
  color: var(--ink-900);
  font-size: min(45px, 5cqw);
  font-weight: 800;
  letter-spacing: -0.045em;
  white-space: nowrap;
}

.auth-panel {
  display: flex;
  min-width: 0;
  min-height: 0;
  align-items: flex-start;
  justify-content: center;
  overflow-y: auto;
  padding: 24px 12px;
  background: linear-gradient(145deg, #e8eef4, #f7f9fb);
}

.auth-card {
  width: min(100%, 360px);
  margin: auto 0;
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 4px;
  box-shadow: 0 18px 44px rgba(25, 46, 67, 0.14);
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
    grid-template-columns: 6fr 4fr;
  }

  .auth-page.signup-mode {
    grid-template-columns: 1fr;
  }

  .auth-page.signup-mode .brand-panel {
    display: none;
  }

  .brand-panel {
    gap: 14px;
    padding: 28px;
  }

  .brand-panel h1 {
    font-size: 42px;
  }

  .auth-page.signup-mode .auth-card {
    width: min(100%, 360px);
  }

  .auth-panel {
    padding: 16px 24px;
  }
}

@media (max-width: 1080px) {
  .auth-page {
    height: auto;
    min-height: 100vh;
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .brand-panel {
    min-height: 280px;
  }

  .auth-panel {
    align-items: flex-start;
    overflow: visible;
  }
}

@media (max-width: 620px) {
  .auth-panel,
  .brand-panel {
    padding: 22px;
  }

  .brand-panel h1 strong {
    font-size: clamp(26px, 8vw, 34px);
    white-space: normal;
  }

  .brand-message {
    width: fit-content;
    max-width: 100%;
  }
}
</style>
