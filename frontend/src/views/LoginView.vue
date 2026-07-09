<script setup>
import { ref } from 'vue'
import LoginForm from '@/components/auth/LoginForm.vue'
import SignupWizard from '@/components/auth/SignupWizard.vue'

const mode = ref('login')
const completeMessage = ref('')

const goLogin = () => {
  mode.value = 'login'
}

const goSignup = () => {
  completeMessage.value = ''
  mode.value = 'signup'
}

const handleSignupCompleted = () => {
  completeMessage.value = '회원가입 신청이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다.'
  mode.value = 'login'
}
</script>

<template>
  <main class="auth-page">
    <section class="brand-panel">
      <p class="eyebrow">PORT GATE MANAGEMENT SYSTEM</p>

      <h1>
        항만 게이트 차량 출입 및<br />
        컨테이너 상차 섹터 안내 시스템
      </h1>

      <div class="brand-features">
        <span>운송사 관리</span>
        <span>기사 등록</span>
        <span>차량 승인</span>
        <span>게이트 출입 관리</span>
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
  grid-template-columns: minmax(0, 1.05fr) minmax(520px, 1fr);
  background: #dfe6ee;
}

.brand-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
  padding: clamp(32px, 6vw, 72px);
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
  max-width: 720px;
  margin: 0;
  font-size: clamp(30px, 4vw, 48px);
  font-weight: 800;
  line-height: 1.22;
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
  padding: 26px;
}

.auth-card {
  width: min(100%, 900px);
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.auth-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  background: #e7edf3;
  border: 1px solid var(--line);
  border-radius: 4px;
  overflow: hidden;
}

.auth-tabs button {
  min-height: 36px;
  color: var(--ink-500);
  background: transparent;
  border: 0;
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

@media (max-width: 1080px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: 300px;
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