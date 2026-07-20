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
      return '계정 정보 입력 중'
    case 2:
      return props.signupRole === 'CARRIER'
        ? '운송사 정보 입력 중'
        : '기사 정보 입력 중'
    case 3:
      return '가입 정보 확인'
    default:
      return '가입 준비 완료'
  }
})

const roleName = computed(() =>
  props.signupRole === 'CARRIER'
    ? '운송사 담당자'
    : '화물 기사'
)
</script>

<template>
  <aside class="summary-card">
    <div class="summary-header">
      <h3>가입 현황</h3>
      <span class="badge">
        {{ statusText }}
      </span>
    </div>

    <div class="summary-item">
      <label>가입유형</label>
      <strong>{{ roleName }}</strong>
    </div>

    <div class="summary-item">
      <label>아이디</label>
      <strong>{{ accountForm.username || '-' }}</strong>
    </div>

    <div
      v-if="signupRole === 'CARRIER'"
      class="summary-item"
    >
      <label>운송사</label>
      <strong>{{ carrierForm.carrierName || '-' }}</strong>
    </div>

    <div
      v-if="signupRole === 'CARRIER'"
      class="summary-item"
    >
      <label>담당자</label>
      <strong>{{ carrierForm.managerName || '-' }}</strong>
    </div>

    <div
      v-if="signupRole === 'DRIVER'"
      class="summary-item"
    >
      <label>기사명</label>
      <strong>{{ driverForm.driverName || '-' }}</strong>
    </div>

    <div
      v-if="signupRole === 'DRIVER'"
      class="summary-item"
    >
      <label>차량번호</label>
      <strong>{{ vehicleForm.plateNumber || '-' }}</strong>
    </div>

    <div
      v-if="signupRole === 'DRIVER'"
      class="summary-item"
    >
      <label>차량종류</label>
      <strong>{{ vehicleTypeLabel(vehicleForm.vehicleType) }}</strong>
    </div>

    <div class="status-box">
      <div class="status-icon">
        !
      </div>

      <div>
        <strong>승인대기</strong>

        <p>
          관리자 승인 후 로그인 가능합니다.
        </p>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.summary-card {
  position: sticky;
  top: 20px;
  display: grid;
  gap: 12px;
  align-self: start;
  padding: 18px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 4px;
}

.summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
}

.summary-header h3 {
  margin: 0;
  color: var(--blue-700);
  font-size: 19px;
  font-weight: 800;
}

.badge {
  padding: 5px 10px;
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
  min-height: 46px;
  border-bottom: 1px solid #edf2f6;
}

.summary-item:last-of-type {
  border-bottom: 0;
}

.summary-item label {
  color: #596b7d;
  font-size: 14px;
  font-weight: 700;
}

.summary-item strong {
  color: #1f3348;
  text-align: right;
}

.status-box {
  display: flex;
  gap: 10px;
  padding: 13px;
  background: #fff8e7;
  border: 1px solid #f3cf82;
  border-radius: 4px;
}

.status-icon {
  display: grid;
  width: 26px;
  height: 26px;
  flex: 0 0 auto;
  place-items: center;
  color: #ffffff;
  background: #d99a24;
  border-radius: 50%;
  font-size: 16px;
  font-weight: 900;
}

.status-box strong {
  color: #9a640b;
}

.status-box p {
  margin: 4px 0 0;
  color: #596b7d;
  font-size: 12px;
  line-height: 1.45;
}

@media (max-height: 760px) and (min-width: 1100px) {
  .summary-card {
    gap: 10px;
    padding: 16px;
  }

  .summary-item {
    min-height: 42px;
  }
}

@media (max-width: 960px) {
  .summary-card {
    position: static;
  }
}
</style>
