# 테스트 데이터 계획

## 목적

트랙터/트레일러 번호판 인식부터 기사 화면 작업정보 확인까지 통합 테스트하기 위한 데이터 조합을 정리한다.

## 정상 테스트 데이터

| 케이스 | 트랙터 번호판 | 트레일러 번호판 | 기사 | 운송사 | 컨테이너 | 야드섹터 | 작업 상태 | 승인 | 예상 결과 |
|---|---|---|---|---|---|---|---|---|---|
| 정상 입차 1 |  |  |  |  |  |  | APPROVED | true | 통과 |
| 정상 입차 2 |  |  |  |  |  |  | APPROVED | true | 통과 |
| 기사 화면 조회 |  |  | 로그인 기사명 |  |  |  | APPROVED | true | 기사 화면 표시 |

## 실패 테스트 데이터

| 케이스 | 트랙터 번호판 | 트레일러 번호판 | 설정 조건 | 예상 결과 |
|---|---|---|---|---|
| 미등록 트랙터 | DB에 없음 | 정상 | 트랙터 vehicle 없음 | 불가 |
| 미등록 트레일러 | 정상 | DB에 없음 | 트레일러 vehicle 없음 | 불가 |
| 유형 불일치 1 | 트레일러 번호 | 정상 | 트랙터 영역에 TRAILER | 불가 |
| 유형 불일치 2 | 정상 | 트랙터 번호 | 트레일러 영역에 TRACTOR | 불가 |
| 기사 출입 불가 | 정상 | 정상 | driver.can_enter = false | 불가 |
| 운송사 상태 이상 | 정상 | 정상 | carrier_status != 정상 | 불가 |
| 작업정보 없음 | 정상 | 정상 | work_order 없음 | 불가 |
| 작업 미승인 | 정상 | 정상 | is_approved = false | 불가 |
| 컨테이너 없음 | 정상 | 정상 | container_id 없음 | 불가 |
| 야드섹터 없음 | 정상 | 정상 | sector_id 없음 | 불가 |

## DB 관계 확인표

| 테이블 | 필수 컬럼 | 확인 |
|---|---|---|
| users | user_id, login_id, user_name, role_code, status |  |
| driver | driver_id, driver_name, carrier_id, can_enter, user_id |  |
| carrier | carrier_id, carrier_name, carrier_status, user_id |  |
| vehicle | vehicle_id, plate_number, vehicle_type, carrier_id |  |
| work_order | work_order_id, driver_id, tractor_vehicle_id, trailer_vehicle_id, container_id |  |
| container | container_id, container_number, sector_id |  |
| yard_sector | sector_id, sector_name, guide_message |  |
| gate_log | gate_log_id, tractor_vehicle_id, trailer_vehicle_id, process_result |  |
| exception_log | exception_type, exception_message, resolved |  |

## 테스트 순서

| 순서 | 테스트 | 확인 |
|---|---|---|
| 1 | 관리자 로그인 |  |
| 2 | 트랙터 번호판 인식 |  |
| 3 | 기사/운송사 정보 표시 |  |
| 4 | 트레일러 번호판 인식 |  |
| 5 | 작업정보/야드섹터 표시 |  |
| 6 | 최종 입차 처리 |  |
| 7 | gate_log 저장 확인 |  |
| 8 | 기사 로그인 |  |
| 9 | 내 작업 안내 표시 |  |
| 10 | 실패 케이스 exception_log 확인 |  |

## 테스트 계정

| 역할 | 로그인 ID | 비밀번호 | 이름 | 연결 대상 |
|---|---|---|---|---|
| 관리자 |  |  |  | ADMIN |
| 기사 |  |  |  | driver |
| 운송사 |  |  |  | carrier |
