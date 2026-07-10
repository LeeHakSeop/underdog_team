<script setup>
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
})
</script>

<template>
  <section class="step-section">

    <div class="section-head">

      <p class="section-label">
        STEP 4
      </p>

      <h3>
        가입 정보 확인
      </h3>

      <p>
        입력한 정보를 확인한 후 회원가입을 진행하세요.
      </p>

    </div>

    <!-- 계정 정보 -->
    <div class="summary-card">

      <h4>계정 정보</h4>

      <div class="summary-row">
        <span>가입유형</span>
        <strong>
          {{ signupRole === 'CARRIER' ? '운송사 담당자' : '화물 기사' }}
        </strong>
      </div>

      <div class="summary-row">
        <span>아이디</span>
        <strong>{{ accountForm.username }}</strong>
      </div>

    </div>

    <!-- 운송사 -->
    <div
      v-if="signupRole==='CARRIER'"
      class="summary-card"
    >

      <h4>운송사 정보</h4>

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

    <!-- 기사 -->
    <div
      v-else
      class="summary-card"
    >

      <h4>기사 정보</h4>

      <div class="summary-row">
        <span>기사명</span>
        <strong>{{ driverForm.driverName }}</strong>
      </div>

      <div class="summary-row">
        <span>연락처</span>
        <strong>{{ driverForm.driverContact }}</strong>
      </div>

      <div class="summary-row">
        <span>운송사</span>
        <strong>{{ driverForm.carrierId }}</strong>
      </div>

    </div>

    <!-- 차량 -->
    <div
      v-if="signupRole==='DRIVER'"
      class="summary-card"
    >

      <h4>차량 정보</h4>

      <div class="summary-row">
        <span>차량번호</span>
        <strong>{{ vehicleForm.plateNumber }}</strong>
      </div>

      <div class="summary-row">
        <span>차량종류</span>
        <strong>{{ vehicleForm.vehicleType }}</strong>
      </div>

      <div class="summary-row">
        <span>톤수</span>
        <strong>{{ vehicleForm.tonnage }}</strong>
      </div>

      <div class="summary-row">
        <span>트랙터번호</span>
        <strong>{{ vehicleForm.tractorNo }}</strong>
      </div>

      <div class="summary-row">
        <span>샤시번호</span>
        <strong>{{ vehicleForm.chassisNo }}</strong>
      </div>

    </div>

    <div class="notice-box">

      <div class="notice-icon">
        ℹ️
      </div>

      <div>

        <strong>
          관리자 승인 후 로그인할 수 있습니다.
        </strong>

        <p>
          회원가입 신청이 완료되면 관리자의 승인 후 시스템을 이용할 수 있습니다.
        </p>

      </div>

    </div>

  </section>
</template>

<style scoped>

.step-section{
    display:grid;
    gap:20px;
}

.section-head{
    padding-bottom:12px;
    border-bottom:1px solid var(--line);
}

.section-label{
    color:var(--blue-700);
    font-size:12px;
    font-weight:700;
}

.section-head h3{
    margin:4px 0;
}

.section-head p{
    color:var(--ink-500);
}

.summary-card{

    border:1px solid var(--line);

    background:#fff;

    border-radius:4px;

    padding:18px;

}

.summary-card h4{

    margin:0 0 14px;

    color:var(--blue-700);

}

.summary-row{

    display:flex;

    justify-content:space-between;

    align-items:center;

    padding:10px 0;

    border-bottom:1px solid #edf1f5;

}

.summary-row:last-child{

    border-bottom:none;

}

.summary-row span{

    color:var(--ink-500);

}

.summary-row strong{

    color:var(--ink-900);

}

.notice-box{

    display:flex;

    gap:14px;

    padding:18px;

    background:#eef6ff;

    border:1px solid #c7dbff;

    border-radius:4px;

}

.notice-icon{

    font-size:28px;

}

.notice-box strong{

    display:block;

    margin-bottom:6px;

    color:#154284;

}

.notice-box p{

    margin:0;

    color:#526579;

}

</style>