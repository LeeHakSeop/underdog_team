<script setup>
import { computed, ref } from 'vue'

import ProgressBar from './ProgressBar.vue'
import AccountStep from './AccountStep.vue'
import CarrierStep from './CarrierStep.vue'
import DriverStep from './DriverStep.vue'
import ConfirmStep from './ConfirmStep.vue'
import SummaryCard from './SummaryCard.vue'

import { registerAccount } from '@/stores/authStore'

const emit = defineEmits(['completed'])

const currentStep = ref(1)
const submitMessage = ref('')
const errorMessage = ref('')
const loading = ref(false)

const signupRole = ref('CARRIER')

const accountForm = ref({
  username: '',
  password: '',
  passwordConfirm: '',
})

const carrierForm = ref({
  carrierName: '',
  carrierContact: '',
  managerName: '',
})

const driverForm = ref({
  driverName: '',
  driverContact: '',
  carrierId: '',
})

const emptyVehicleForm = {
  plateNumber: '',
  vehicleType: '',
  tonnage: '',
  tractorNo: '',
  chassisNo: '',
}

const steps = computed(() => [
  { key: 'account', label: '계정' },
  {
    key: signupRole.value === 'CARRIER' ? 'carrier' : 'driver',
    label: signupRole.value === 'CARRIER' ? '운송사' : '기사',
  },
  { key: 'confirm', label: '확인' },
])

const maxStep = computed(() => steps.value.length)

const clearMessage = () => {
  submitMessage.value = ''
  errorMessage.value = ''
}

const resetForm = () => {
  currentStep.value = 1
  signupRole.value = 'CARRIER'

  accountForm.value = {
    username: '',
    password: '',
    passwordConfirm: '',
  }

  carrierForm.value = {
    carrierName: '',
    carrierContact: '',
    managerName: '',
  }

  driverForm.value = {
    driverName: '',
    driverContact: '',
    carrierId: '',
  }
}

const validateAccount = () => {
  if (!accountForm.value.username.trim()) {
    throw new Error('아이디를 입력하세요.')
  }

  if (!accountForm.value.password) {
    throw new Error('비밀번호를 입력하세요.')
  }

  if (accountForm.value.password.length < 4) {
    throw new Error('비밀번호는 4자 이상 입력하세요.')
  }

  if (accountForm.value.password !== accountForm.value.passwordConfirm) {
    throw new Error('비밀번호 확인이 일치하지 않습니다.')
  }
}

const validateCarrier = () => {
  if (!carrierForm.value.carrierName.trim()) {
    throw new Error('운송사명을 입력하세요.')
  }

  if (!carrierForm.value.managerName.trim()) {
    throw new Error('담당자명을 입력하세요.')
  }

  if (!carrierForm.value.carrierContact.trim()) {
    throw new Error('운송사 연락처를 입력하세요.')
  }
}

const validateDriver = () => {
  if (!driverForm.value.driverName.trim()) {
    throw new Error('기사명을 입력하세요.')
  }

  if (!driverForm.value.driverContact.trim()) {
    throw new Error('기사 연락처를 입력하세요.')
  }

  if (!driverForm.value.carrierId) {
    throw new Error('소속 운송사를 선택하세요.')
  }
}

const validateCurrentStep = () => {
  if (currentStep.value === 1) {
    validateAccount()
    return
  }

  if (currentStep.value === 2 && signupRole.value === 'CARRIER') {
    validateCarrier()
    return
  }

  if (currentStep.value === 2 && signupRole.value === 'DRIVER') {
    validateDriver()
  }
}

const nextStep = () => {
  clearMessage()

  try {
    validateCurrentStep()

    if (currentStep.value < maxStep.value) {
      currentStep.value += 1
    }
  } catch (error) {
    errorMessage.value = error.message
  }
}

const prevStep = () => {
  clearMessage()

  if (currentStep.value > 1) {
    currentStep.value -= 1
  }
}

const buildPayload = () => {
  const base = {
    username: accountForm.value.username,
    password: accountForm.value.password,
    roleCode: signupRole.value,
    displayName:
      signupRole.value === 'CARRIER'
        ? carrierForm.value.managerName || carrierForm.value.carrierName
        : driverForm.value.driverName,
  }

  if (signupRole.value === 'CARRIER') {
    return {
      ...base,
      carrierName: carrierForm.value.carrierName,
      carrierContact: carrierForm.value.carrierContact,
      managerName: carrierForm.value.managerName,
    }
  }

  return {
    ...base,
    driverName: driverForm.value.driverName,
    driverContact: driverForm.value.driverContact,
    carrierId: Number(driverForm.value.carrierId),
  }
}

const submitSignup = async () => {
  clearMessage()

  try {
    validateAccount()

    if (signupRole.value === 'CARRIER') {
      validateCarrier()
    }

    if (signupRole.value === 'DRIVER') {
      validateDriver()
    }

    loading.value = true

    await registerAccount(buildPayload())

    submitMessage.value = '회원가입 신청이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다.'

    resetForm()
    emit('completed')
  } catch (error) {
    errorMessage.value = error.message || '회원가입에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="signup-layout">
    <section class="wizard-panel">
      <ProgressBar
        :current-step="currentStep"
        :steps="steps"
      />

      <div class="wizard-body">
        <AccountStep
          v-if="currentStep === 1"
          v-model="accountForm"
          v-model:signupRole="signupRole"
        />

        <CarrierStep
          v-else-if="signupRole === 'CARRIER' && currentStep === 2"
          v-model="carrierForm"
        />

        <DriverStep
          v-else-if="signupRole === 'DRIVER' && currentStep === 2"
          v-model="driverForm"
        />

        <ConfirmStep
          v-else-if="currentStep === 3"
          :signup-role="signupRole"
          :account-form="accountForm"
          :carrier-form="carrierForm"
          :driver-form="driverForm"
          :vehicle-form="emptyVehicleForm"
        />
      </div>

      <div
        v-if="submitMessage"
        class="message success"
      >
        {{ submitMessage }}
      </div>

      <div
        v-if="errorMessage"
        class="message error"
      >
        {{ errorMessage }}
      </div>

      <div class="wizard-footer">
        <button
          v-if="currentStep > 1"
          type="button"
          class="secondary-button"
          @click="prevStep"
        >
          ← 이전
        </button>

        <div class="spacer" />

        <button
          v-if="currentStep < maxStep"
          type="button"
          class="primary-button"
          @click="nextStep"
        >
          다음 →
        </button>

        <button
          v-else
          type="button"
          class="primary-button"
          :disabled="loading"
          @click="submitSignup"
        >
          {{ loading ? '가입 중...' : '회원가입' }}
        </button>
      </div>
    </section>

    <SummaryCard
      :signup-role="signupRole"
      :account-form="accountForm"
      :carrier-form="carrierForm"
      :driver-form="driverForm"
      :vehicle-form="emptyVehicleForm"
      :current-step="currentStep"
    />
  </div>
</template>

<style scoped>
.signup-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 16px;
}

.wizard-panel {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 16px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.wizard-body {
  min-height: 320px;
  padding: 14px;
  background: #f8fbfe;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.message {
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 700;
}

.message.success {
  color: #155e3b;
  background: #ecfdf3;
  border: 1px solid #b7ebc9;
}

.message.error {
  color: #8a1f1f;
  background: #fff1f1;
  border: 1px solid #f5b5b5;
}

.wizard-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 4px;
}

.spacer {
  flex: 1;
}

.primary-button,
.secondary-button {
  min-width: 96px;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 4px;
  font-weight: 800;
  cursor: pointer;
}

.primary-button {
  color: #ffffff;
  background: var(--blue-700);
  border: 1px solid var(--blue-700);
}

.primary-button:hover {
  background: #1d4e89;
}

.primary-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.secondary-button {
  color: #28445f;
  background: #ffffff;
  border: 1px solid #c8d4e0;
}

.secondary-button:hover {
  background: #f1f5f9;
}

@media (max-width: 960px) {
  .signup-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .wizard-panel {
    padding: 12px;
  }

  .wizard-body {
    padding: 12px;
  }

  .wizard-footer {
    display: grid;
    grid-template-columns: 1fr;
  }

  .primary-button,
  .secondary-button {
    width: 100%;
  }

  .spacer {
    display: none;
  }
}
</style>