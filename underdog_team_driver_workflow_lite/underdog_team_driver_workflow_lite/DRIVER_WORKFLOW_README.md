# 기사관리 + 작업오더 전달 기능

## 포함 기능
- 운송사 메뉴의 `기사 관리` 페이지
- 회원가입 당시 입력한 트랙터 차량번호, 트랙터번호, 샤시번호, 톤수, 상태 조회
- 기사/트랙터 정보 수정
- 기사 삭제
  - 승인 대기 또는 진행 중 작업이 있으면 삭제 차단
  - 완료 작업의 기사 참조는 NULL 처리
  - 기사 트랙터 삭제
  - 기사에게 배정된 트레일러는 기사/user 배정만 해제
- 운송사에서 기사 + 트레일러 + 작업유형 + 예약시간으로 작업오더 전달
- 생성 상태: `DISPATCH_WAITING`
- 관리자 승인 후: `APPROVED`
- 기사 페이지에서 작업 시작: `IN_PROGRESS`
- 기사 페이지에서 작업 완료: `COMPLETED`
- 완료 시 해당 트레일러의 `driver_id`, `user_id`를 NULL로 초기화

## DB 관련
이 버전은 기존 `work_order.vehicle_id`를 트레일러 ID로 사용합니다.
`tractor_vehicle_id`, `trailer_vehicle_id` 컬럼을 조회하지 않으므로 기존에 발생한
`wo.tractor_vehicle_id 칼럼 없음` 오류가 발생하지 않습니다.
컨테이너는 이번 범위에서 사용하지 않으며 `container_id`는 NULL로 저장됩니다.

## 실행
프론트엔드:
```bash
cd frontend
npm install
npm run dev
```

백엔드:
```bash
cd backend/portprj
gradlew.bat bootRun
```
