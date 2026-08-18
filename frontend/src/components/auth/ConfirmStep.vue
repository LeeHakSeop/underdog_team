<script setup>
import { vehicleTypeLabel } from '@/config/vehicleType'

defineProps({
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
})
</script>

<template>
  <section class="step-section">
    <div class="section-head">
      <p class="section-label">STEP 3</p>
      <h3>가입 정보 확인</h3>
      <p>신청 전에 입력한 정보를 확인하세요.</p>
    </div>

    <div class="summary-list">
      <div class="summary-group">
        <h4>계정</h4>
        <div class="summary-row">
          <span>가입 유형</span>
          <strong>{{ signupRole === 'CARRIER' ? '운송사 담당자' : '화물기사' }}</strong>
        </div>
        <div class="summary-row">
          <span>아이디</span>
          <strong>{{ accountForm.username }}</strong>
        </div>
      </div>

      <div v-if="signupRole === 'CARRIER'" class="summary-group">
        <h4>운송사</h4>
        <div class="summary-row">
          <span>운송사명</span>
          <strong>{{ carrierForm.carrierName }}</strong>
        </div>
        <div class="summary-row">
          <span>담당자</span>
          <strong>{{ carrierForm.managerName }}</strong>
        </div>
        <div class="summary-row">
          <span>연락처</span>
          <strong>{{ carrierForm.carrierContact }}</strong>
        </div>
      </div>

      <template v-else>
        <div class="summary-group">
          <h4>기사</h4>
          <div class="summary-row">
            <span>이름</span>
            <strong>{{ driverForm.driverName }}</strong>
          </div>
          <div class="summary-row">
            <span>연락처</span>
            <strong>{{ driverForm.driverContact }}</strong>
          </div>
          <div class="summary-row">
            <span>소속 운송사</span>
            <strong>{{ driverForm.carrierId }}</strong>
          </div>
        </div>

        <div class="summary-group">
          <h4>본인 트랙터</h4>
          <div class="summary-row">
            <span>차량번호</span>
            <strong>{{ vehicleForm.plateNumber }}</strong>
          </div>
          <div class="summary-row">
            <span>차량유형</span>
            <strong>{{ vehicleTypeLabel(vehicleForm.vehicleType) }}</strong>
          </div>
          <div class="summary-row">
            <span>축 형식</span>
            <strong>{{ vehicleForm.tonnage }}</strong>
          </div>
        </div>
      </template>
    </div>

    <div class="notice-box">
      <div class="notice-icon">!</div>
      <div>
        <strong>승인 후 로그인할 수 있습니다.</strong>
        <p v-if="signupRole === 'DRIVER'">
          운송사 승인과 관리자 최종 승인 후 터미널 출입 권한이 적용됩니다.
        </p>
        <p v-else>
          관리자 승인 후 운송사 계정으로 기사와 차량을 관리할 수 있습니다.
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.step-section {
  display: grid;
  gap: 22px;
}

.section-head {
  display: grid;
  gap: 6px;
}

.section-label {
  margin: 0;
  color: var(--blue-700);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.section-head h3,
.section-head p {
  margin: 0;
}

.section-head h3 {
  color: var(--ink-900);
  font-size: 24px;
  font-weight: 800;
}

.section-head p {
  color: var(--ink-500);
  font-size: 14px;
}

.summary-list {
  display: grid;
  gap: 18px;
}

.summary-group {
  display: grid;
  gap: 6px;
}

.summary-group h4 {
  margin: 0 0 2px;
  color: var(--blue-700);
  font-size: 14px;
  font-weight: 800;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 34px;
  padding: 6px 0;
  border-bottom: 1px solid #dfe7ef;
}

.summary-row span {
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}

.summary-row strong {
  color: var(--ink-900);
  text-align: right;
  word-break: keep-all;
}

.notice-box {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #eef6ff;
  border: 1px solid #c7dbff;
  border-radius: 8px;
}

.notice-icon {
  display: grid;
  width: 24px;
  height: 24px;
  flex: 0 0 auto;
  place-items: center;
  color: #ffffff;
  background: var(--blue-700);
  border-radius: 50%;
  font-weight: 900;
}

.notice-box strong {
  display: block;
  margin-bottom: 4px;
  color: #154284;
}

.notice-box p {
  margin: 0;
  color: #526579;
  font-size: 13px;
  line-height: 1.45;
}

@media (max-width: 620px) {
  .summary-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }

  .summary-row strong {
    text-align: left;
  }
}
</style>
