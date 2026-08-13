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
  completeMessage.value = '회원가입 신청이 완료되었습니다. 승인 후 로그인할 수 있습니다.'
  setMode('login')
}
</script>

<template>
  <main class="auth-page">
    <section class="brand-panel">
      <div class="brand-message">
        <p class="eyebrow">PORT GATE</p>
        <h1>
          <span>항만 차량 출입과</span>
          <strong>컨테이너 작업 관리</strong>
        </h1>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-workspace">
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
  grid-template-columns: minmax(390px, 42fr) minmax(620px, 58fr);
  overflow: hidden;
  background: #f4f7fa;
}

.brand-panel {
  display: flex;
  position: relative;
  container-type: inline-size;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  padding: clamp(34px, 5vw, 64px);
  color: #ffffff;
  overflow: hidden;
  background: #1f4e7b;
}

.brand-panel::before {
  position: absolute;
  inset: 0;
  z-index: 0;
  content: '';
  background: linear-gradient(90deg, rgba(9, 26, 43, 0.28), rgba(9, 26, 43, 0.06)),
    url('/images/gamman-pier.jpg') center / cover no-repeat;
  transform: scaleX(-1);
}

.brand-message {
  position: relative;
  z-index: 1;
  width: min(100%, 480px);
  align-self: flex-start;
  padding: 0;
  text-shadow: 0 2px 18px rgba(0, 0, 0, 0.34);
}

.eyebrow {
  margin: 0 0 10px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.brand-panel h1 {
  width: 100%;
  margin: 0;
  line-height: 1.16;
}

.brand-panel h1 span,
.brand-panel h1 strong {
  display: block;
  word-break: keep-all;
}

.brand-panel h1 span {
  color: rgba(255, 255, 255, 0.88);
  font-size: clamp(19px, 2vw, 24px);
  font-weight: 700;
  letter-spacing: 0;
}

.brand-panel h1 strong {
  margin-top: 8px;
  color: #ffffff;
  font-size: clamp(30px, 3.7cqw, 42px);
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.18;
}

.auth-panel {
  display: flex;
  min-width: 0;
  min-height: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 44px 56px;
  background: #f4f7fa;
}

.auth-workspace {
  width: 100%;
  max-width: 760px;
  height: min(680px, calc(100dvh - 56px));
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 30px;
}

.auth-workspace > :deep(.login-form) {
  width: min(100%, 440px);
  justify-self: center;
  align-self: center;
}

.auth-tabs {
  display: grid;
  width: 100%;
  max-width: 360px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  justify-self: center;
  padding: 4px;
  background: #e8eef5;
  border-radius: 999px;
}

.auth-tabs button {
  min-height: 34px;
  color: var(--ink-500);
  background: transparent;
  border: 0;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.auth-tabs button.active {
  color: #ffffff;
  background: var(--blue-700);
  box-shadow: 0 6px 14px rgba(31, 92, 150, 0.22);
}

.complete-message {
  width: min(100%, 520px);
  justify-self: center;
  margin: -12px 0 0;
  padding: 10px 12px;
  color: #155e3b;
  background: #ecfdf3;
  border: 1px solid #b7ebc9;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
}

@media (max-height: 760px) and (min-width: 1100px) {
  .auth-page {
    grid-template-columns: minmax(340px, 40fr) minmax(640px, 60fr);
  }

  .brand-panel {
    padding: 28px;
  }

  .auth-panel {
    padding: 24px 36px;
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
    padding: 34px 24px;
  }

  .auth-workspace {
    height: auto;
  }
}

@media (max-width: 620px) {
  .auth-panel,
  .brand-panel {
    padding: 22px;
  }

  .brand-panel h1 strong {
    font-size: clamp(26px, 8vw, 34px);
  }
}
</style>
