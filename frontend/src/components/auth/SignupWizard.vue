<script setup>
import { computed, ref, watch } from 'vue'

import ProgressBar from './ProgressBar.vue'
import AccountStep from './AccountStep.vue'
import CarrierStep from './CarrierStep.vue'
import DriverStep from './DriverStep.vue'
import ConfirmStep from './ConfirmStep.vue'
import SummaryCard from './SummaryCard.vue'

import { registerAccount } from '@/stores/authStore'
import { checkLoginId } from '@/api/authApi'

const emit = defineEmits(['completed'])

const currentStep = ref(1)
const submitMessage = ref('')
const errorMessage = ref('')
const loading = ref(false)
const checkingLoginId = ref(false)
const signupRole = ref('CARRIER')

const accountForm = ref({
  username: '',
  password: '',
  passwordConfirm: '',
})

const loginIdCheck = ref({
  username: '',
  available: null,
  message: '',
})

const carrierForm = ref({
  carrierName: '',
  carrierContact: '',
  managerName: '',
})

const createDriverForm = () => ({
  driverName: '',
  driverContact: '',
  carrierId: '',
  plateNumber: '',
  vehicleType: 'TRACTOR',
  tonnage: '6x2',
  tractorNo: '',
  chassisNo: '',
})

const driverForm = ref(createDriverForm())

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

const driverVehicleForm = computed(() => ({
  plateNumber: driverForm.value.plateNumber,
  vehicleType: 'TRACTOR',
  tonnage: driverForm.value.tonnage,
  tractorNo: driverForm.value.tractorNo,
  chassisNo: driverForm.value.chassisNo,
}))

const clearMessage = () => {
  submitMessage.value = ''
  errorMessage.value = ''
}

const resetLoginIdCheck = () => {
  loginIdCheck.value = {
    username: '',
    available: null,
    message: '',
  }
}

watch(
  () => accountForm.value.username,
  () => {
    resetLoginIdCheck()
  },
)

watch(signupRole, () => {
  clearMessage()
  currentStep.value = 1
})

const resetForm = () => {
  currentStep.value = 1
  signupRole.value = 'CARRIER'
  accountForm.value = {
    username: '',
    password: '',
    passwordConfirm: '',
  }
  resetLoginIdCheck()
  carrierForm.value = {
    carrierName: '',
    carrierContact: '',
    managerName: '',
  }
  driverForm.value = createDriverForm()
}

const validateAccountFields = () => {
  if (!accountForm.value.username.trim()) throw new Error('아이디를 입력하세요.')
  if (!accountForm.value.password) throw new Error('비밀번호를 입력하세요.')
  if (accountForm.value.password.length < 4) throw new Error('비밀번호는 4자 이상 입력하세요.')
  if (accountForm.value.password !== accountForm.value.passwordConfirm) {
    throw new Error('비밀번호 확인이 일치하지 않습니다.')
  }
}

const checkUsernameAvailability = async () => {
  const username = accountForm.value.username.trim()
  if (!username) {
    resetLoginIdCheck()
    throw new Error('아이디를 입력하세요.')
  }

  checkingLoginId.value = true
  try {
    const result = await checkLoginId(username)
    loginIdCheck.value = {
      username,
      available: result.available,
      message: result.message || (result.available ? '사용 가능한 아이디입니다.' : '이미 사용 중인 아이디입니다.'),
    }
    if (!result.available) throw new Error(loginIdCheck.value.message)
    return result
  } finally {
    checkingLoginId.value = false
  }
}

const ensureLoginIdAvailable = async () => {
  const username = accountForm.value.username.trim()
  if (loginIdCheck.value.username === username && loginIdCheck.value.available === true) return
  await checkUsernameAvailability()
}

const validateAccount = async () => {
  validateAccountFields()
  await ensureLoginIdAvailable()
}

const validateCarrier = () => {
  if (!carrierForm.value.carrierName.trim()) throw new Error('운송사명을 입력하세요.')
  if (!carrierForm.value.managerName.trim()) throw new Error('담당자명을 입력하세요.')
  if (!carrierForm.value.carrierContact.trim()) throw new Error('운송사 연락처를 입력하세요.')
}

const validateDriver = () => {
  if (!driverForm.value.driverName.trim()) throw new Error('기사 이름을 입력하세요.')
  if (!driverForm.value.driverContact.trim()) throw new Error('기사 연락처를 입력하세요.')
  if (!driverForm.value.carrierId) throw new Error('소속 운송사를 선택하세요.')
  if (!driverForm.value.plateNumber.trim()) throw new Error('트랙터 차량번호를 입력하세요.')
  if (!driverForm.value.tonnage) throw new Error('축 형식을 선택하세요.')
}

const validateCurrentStep = async () => {
  if (currentStep.value === 1) {
    await validateAccount()
    return
  }
  if (currentStep.value === 2 && signupRole.value === 'CARRIER') {
    validateCarrier()
    return
  }
  if (currentStep.value === 2 && signupRole.value === 'DRIVER') validateDriver()
}

const nextStep = async () => {
  clearMessage()
  try {
    await validateCurrentStep()
    if (currentStep.value < maxStep.value) currentStep.value += 1
  } catch (error) {
    if (
      currentStep.value === 1 &&
      loginIdCheck.value.available === false &&
      error.message === loginIdCheck.value.message
    ) return
    errorMessage.value = error.message
  }
}

const prevStep = () => {
  clearMessage()
  if (currentStep.value > 1) currentStep.value -= 1
}

const buildPayload = () => {
  const base = {
    username: accountForm.value.username.trim(),
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
      carrierName: carrierForm.value.carrierName.trim(),
      carrierContact: carrierForm.value.carrierContact.trim(),
      managerName: carrierForm.value.managerName.trim(),
    }
  }

  return {
    ...base,
    driverName: driverForm.value.driverName.trim(),
    driverContact: driverForm.value.driverContact.trim(),
    carrierId: Number(driverForm.value.carrierId),
    plateNumber: driverForm.value.plateNumber.trim(),
    vehicleType: 'TRACTOR',
    tonnage: driverForm.value.tonnage,
    tractorNo: driverForm.value.tractorNo?.trim() || null,
    chassisNo: driverForm.value.chassisNo || null,
  }
}

const submitSignup = async () => {
  clearMessage()
  try {
    await validateAccount()
    if (signupRole.value === 'CARRIER') validateCarrier()
    if (signupRole.value === 'DRIVER') validateDriver()
    loading.value = true
    await registerAccount(buildPayload())
    submitMessage.value = '회원가입 신청이 완료되었습니다. 승인 후 로그인할 수 있습니다.'
    resetForm()
    emit('completed')
  } catch (error) {
    errorMessage.value = error.message || '회원가입에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

const handleCheckLoginId = async () => {
  clearMessage()
  try {
    await checkUsernameAvailability()
  } catch (error) {
    if (loginIdCheck.value.available === false && error.message === loginIdCheck.value.message) return
    errorMessage.value = error.message
  }
}
</script>

<template>
  <div class="signup-layout" :class="{ 'has-summary': currentStep === 3 }">
    <section class="wizard-panel">
      <ProgressBar :current-step="currentStep" :steps="steps" />

      <div class="wizard-body">
        <AccountStep
          v-if="currentStep === 1"
          v-model="accountForm"
          v-model:signupRole="signupRole"
          :login-id-check="loginIdCheck"
          :checking-login-id="checkingLoginId"
          @check-login-id="handleCheckLoginId"
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
          :vehicle-form="signupRole === 'DRIVER' ? driverVehicleForm : emptyVehicleForm"
        />
      </div>

      <div v-if="submitMessage" class="message success">{{ submitMessage }}</div>
      <div v-if="errorMessage" class="message error">{{ errorMessage }}</div>

      <div class="wizard-footer">
        <button
          v-if="currentStep > 1"
          type="button"
          class="secondary-button"
          @click="prevStep"
        >
          이전
        </button>

        <div class="spacer" />

        <button
          v-if="currentStep < maxStep"
          type="button"
          class="primary-button"
          :disabled="checkingLoginId"
          @click="nextStep"
        >
          {{ checkingLoginId ? '확인 중...' : '다음 단계' }}
        </button>

        <button
          v-else
          type="button"
          class="primary-button"
          :disabled="loading || checkingLoginId"
          @click="submitSignup"
        >
          {{ loading ? '가입 중...' : '회원가입 신청' }}
        </button>
      </div>
    </section>

    <SummaryCard
      v-if="currentStep === 3"
      :signup-role="signupRole"
      :account-form="accountForm"
      :carrier-form="carrierForm"
      :driver-form="driverForm"
      :vehicle-form="signupRole === 'DRIVER' ? driverVehicleForm : emptyVehicleForm"
      :current-step="currentStep"
    />
  </div>
</template>

<style scoped>
.signup-layout {
  display: grid;
  grid-template-columns: minmax(0, 720px);
  gap: 22px;
  align-items: start;
  justify-content: center;
}

.signup-layout.has-summary {
  grid-template-columns: minmax(0, 1fr) 220px;
}

.wizard-panel {
  display: grid;
  gap: 0;
  min-width: 0;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 0;
}

.wizard-body {
  min-height: 0;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 0;
}

.wizard-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 26px;
}

.spacer {
  flex: 1;
}

.primary-button,
.secondary-button {
  min-width: 124px;
  min-height: 46px;
  padding: 0 18px;
  border-radius: 8px;
  font-weight: 800;
  cursor: pointer;
}

.primary-button {
  color: #ffffff;
  background: var(--blue-700);
  border: 1px solid var(--blue-700);
  box-shadow: 0 10px 22px rgba(31, 92, 150, 0.16);
}

.secondary-button {
  color: var(--ink-700);
  background: transparent;
  border: 1px solid #cbd7e3;
}

.primary-button:disabled,
.secondary-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.message {
  margin-top: 18px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 800;
}

.message.success {
  color: #155e3b;
  background: #ecfdf3;
  border: 1px solid #b7ebc9;
}

.message.error {
  color: #991b1b;
  background: #fff1f1;
  border: 1px solid #fecaca;
}

@media (max-width: 900px) {
  .signup-layout,
  .signup-layout.has-summary {
    grid-template-columns: 1fr;
  }
}
</style>
