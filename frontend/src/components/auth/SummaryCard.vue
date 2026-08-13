<script setup>
import { computed } from 'vue'
import { vehicleTypeLabel } from '@/config/vehicleType'

const props = defineProps({
  signupRole: {
    type: String,
    required: true,
  },
  accountForm: {
    type: Object,
    required: true,
  },
  carrierForm: {
    type: Object,
    required: true,
  },
  driverForm: {
    type: Object,
    required: true,
  },
  vehicleForm: {
    type: Object,
    required: true,
  },
  currentStep: {
    type: Number,
    default: 1,
  },
})

const statusText = computed(() => {
  switch (props.currentStep) {
    case 1:
      return '계정 입력'
    case 2:
      return props.signupRole === 'CARRIER' ? '운송사 입력' : '기사 정보 입력'
    case 3:
      return '최종 확인'
    default:
      return '가입 준비'
  }
})

const roleName = computed(() =>
  props.signupRole === 'CARRIER'
    ? '운송사 담당자'
    : '화물기사',
)
</script>

<template>
  <aside class="summary-card">
    <div class="summary-header">
      <h3>가입 현황</h3>
      <span class="badge">{{ statusText }}</span>
    </div>

    <div class="summary-item">
      <label>가입 유형</label>
      <strong>{{ roleName }}</strong>
    </div>

    <div v-if="accountForm.username" class="summary-item">
      <label>아이디</label>
      <strong>{{ accountForm.username }}</strong>
    </div>

    <template v-if="signupRole === 'CARRIER'">
      <div v-if="carrierForm.carrierName" class="summary-item">
        <label>운송사</label>
        <strong>{{ carrierForm.carrierName }}</strong>
      </div>

      <div v-if="carrierForm.managerName" class="summary-item">
        <label>담당자</label>
        <strong>{{ carrierForm.managerName }}</strong>
      </div>
    </template>

    <template v-else>
      <div v-if="driverForm.driverName" class="summary-item">
        <label>기사</label>
        <strong>{{ driverForm.driverName }}</strong>
      </div>

      <div v-if="vehicleForm.plateNumber" class="summary-item">
        <label>차량번호</label>
        <strong>{{ vehicleForm.plateNumber }}</strong>
      </div>

      <div class="summary-item">
        <label>차량유형</label>
        <strong>{{ vehicleTypeLabel(vehicleForm.vehicleType) }}</strong>
      </div>

      <div v-if="vehicleForm.tonnage" class="summary-item">
        <label>축 형식</label>
        <strong>{{ vehicleForm.tonnage }}</strong>
      </div>
    </template>

    <div v-if="currentStep === 3" class="status-box">
      <div class="status-icon">!</div>

      <div>
        <strong>승인 대기</strong>
        <p v-if="signupRole === 'DRIVER'">
          운송사 승인 후 관리자가 최종 출입 권한을 확정합니다.
        </p>
        <p v-else>
          관리자 승인 후 운송사 기능을 사용할 수 있습니다.
        </p>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.summary-card {
  position: sticky;
  top: 0;
  display: grid;
  gap: 7px;
  align-self: start;
  padding: 0 0 0 18px;
  background: transparent;
  border-left: 1px solid #dce6ef;
}

.summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 10px;
}

.summary-header h3 {
  margin: 0;
  color: var(--blue-700);
  font-size: 15px;
  font-weight: 800;
}

.badge {
  padding: 4px 8px;
  color: #1d4e89;
  white-space: nowrap;
  background: #eef6ff;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
}

.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 32px;
  border-bottom: 1px solid #e2eaf2;
}

.summary-item label {
  color: #596b7d;
  flex: 0 0 auto;
  font-size: 12px;
  font-weight: 700;
}

.summary-item strong {
  color: #1f3348;
  font-size: 13px;
  text-align: right;
  word-break: keep-all;
}

.status-box {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  padding: 10px;
  background: #ffffff;
  border: 1px solid #dce6ef;
  border-radius: 8px;
}

.status-icon {
  display: grid;
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  place-items: center;
  color: #ffffff;
  background: #718096;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 900;
}

.status-box strong {
  color: #344054;
}

.status-box p {
  margin: 4px 0 0;
  color: #596b7d;
  font-size: 11px;
  line-height: 1.45;
}

@media (max-width: 900px) {
  .summary-card {
    position: static;
    padding: 18px 0 0;
    border-top: 1px solid #dce6ef;
    border-left: 0;
  }
}
</style>
