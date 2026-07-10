# 통합 체크리스트

## 목적

김보현님 회원/권한 기능과 박준희님 gate_log/exception_log 기능을 받은 뒤, 기존 번호판 인식/조회/기사 화면과 충돌 없이 병합하기 위한 확인 목록이다.

## 김보현님 기능 수령 시 확인

| 구분 | 확인 항목 | 확인 결과 | 비고 |
|---|---|---|---|
| 로그인 | 로그인 응답에 userId가 포함되는가 |  | 현재 portGateUser에 userId 저장 중 |
| 로그인 | 로그인 응답에 roleCode가 포함되는가 |  | ADMIN / DRIVER / CARRIER |
| 기사 | driver 테이블에 user_id가 연결되는가 |  | 연결되면 기사 작업 조회 기준 변경 필요 |
| 기사 | DRIVER 계정 가입 후 driver 데이터가 생성되는가 |  | 승인 후 생성인지 즉시 생성인지 확인 |
| 기사 | driverId를 로그인 응답에서 받을 수 있는가 |  | 가능하면 프론트 조회 기준으로 사용 |
| 운송사 | carrier 테이블에 user_id가 연결되는가 |  | 운송사 화면 조회 기준 |
| 운송사 | CARRIER 계정 가입 후 carrier 데이터가 생성되는가 |  | 회사 등록 흐름 확인 |
| 권한 | 관리자 승인 전 로그인/접근 제한이 있는가 |  | status 값 기준 확인 |
| DB | users / driver / carrier 샘플 데이터가 맞는가 |  | 로그인 테스트 계정 필요 |

## 박준희님 기능 수령 시 확인

| 구분 | 확인 항목 | 확인 결과 | 비고 |
|---|---|---|---|
| 입차 | 입차 처리 API 주소가 확정되었는가 |  | 예: POST /api/gate-process/enter |
| 입차 | 요청 DTO에 tractorVehicleId가 포함되는가 |  | 트랙터 결과에서 전달 |
| 입차 | 요청 DTO에 trailerVehicleId가 포함되는가 |  | 트레일러 결과에서 전달 |
| 입차 | 요청 DTO에 workOrderId가 포함되는가 |  | 작업 상태 변경 기준 |
| 입차 | 통과 시 gate_log가 1건으로 저장되는가 |  | 트랙터/트레일러 병합 로그 |
| 예외 | 실패 시 exception_log가 저장되는가 |  | 실패 유형 코드 확인 |
| 작업 | 입차 성공 시 work_order 상태가 변경되는가 |  | 예: APPROVED -> GATE_IN |
| 출차 | 출차 처리 API가 있는가 |  | 예: POST /api/gate-process/exit |
| 출차 | 출차 성공 시 gate_log OUT이 저장되는가 |  | 작업 완료 후 출차 |
| 응답 | 성공/실패 응답 구조가 확정되었는가 |  | 프론트 메시지 표시 기준 |

## 이학섭 병합 시 확인

| 순서 | 작업 | 확인 |
|---|---|---|
| 1 | 최신 브랜치 pull 또는 merge 전 상태 확인 |  |
| 2 | DB 변경 SQL 확인 |  |
| 3 | 백엔드 컴파일 확인 |  |
| 4 | API 주소/DTO 이름 충돌 확인 |  |
| 5 | 프론트 api 파일 연결 |  |
| 6 | store action 연결 |  |
| 7 | view 표시값 연결 |  |
| 8 | 관리자 번호판 인식 정상 시나리오 테스트 |  |
| 9 | 기사 로그인 후 본인 작업정보 확인 |  |
| 10 | 실패 시나리오 메시지 확인 |  |

## 우선 수정 예상 지점

| 기능 수령 후 | 수정 예상 파일 | 수정 방향 |
|---|---|---|
| driver.user_id 확정 | DriverMapper.java | userName 기준 조회를 userId 또는 driverId 기준으로 변경 |
| 로그인 응답 driverId 포함 | LoginResponseDTO.java / LoginView.vue | portGateUser에 driverId 저장 |
| gate process API 확정 | plateRecognitionApi.js 또는 신규 API 파일 | 최종 출입 처리 API 연결 |
| gate process 응답 확정 | AdminPlateRecognitionView.vue | 프론트 자체 판단 대신 백엔드 결과 표시 |
| exception_log 응답 확정 | AdminPlateRecognitionView.vue | 실패 사유 메시지 표시 |
