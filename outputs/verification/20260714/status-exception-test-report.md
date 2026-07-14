# 상태·예외 코드 정합성 및 단위 테스트 결과

검증일: 2026-07-14

## 1. 적용 기준

작업 상태는 다음 코드만 사용한다.

`DISPATCH_WAITING → APPROVED → GATE_IN → IN_PROGRESS → COMPLETED → GATE_OUT`

승인 요청 반려는 `CANCELED`로 저장한다. 기존 작업 상태 별칭인 `REQUESTED`, `PENDING`, `REJECTED`는 작업 상태 처리에서 제거했다.

번호판 미탐지는 `PLATE_NOT_DETECTED`, 작업 미승인은 `WORK_ORDER_NOT_APPROVED`로 통일했다. 게이트와 AI가 같은 예외 코드를 반환하고, 예외 로그 SQL도 동일 코드를 저장한다.

## 2. 상태 이력 저장 흐름

| 동작 | 이전 상태 | 새 상태 | 저장 내용 |
|---|---|---|---|
| 관리자 승인 | DISPATCH_WAITING | APPROVED | 변경 시각, SYSTEM, 관리자 승인 |
| 관리자 반려 | DISPATCH_WAITING | CANCELED | 변경 시각, SYSTEM, 관리자 반려 |
| 입차 처리 | APPROVED | GATE_IN | 변경 시각, SYSTEM, 작업 상태 변경 |
| 작업 시작 | GATE_IN | IN_PROGRESS | 변경 시각, SYSTEM, 작업 상태 변경 |
| 작업 완료 | IN_PROGRESS | COMPLETED | 변경 시각, SYSTEM, 작업 상태 변경 |
| 출차 처리 | COMPLETED | GATE_OUT | 변경 시각, SYSTEM, 작업 상태 변경 |

작업 상태 변경과 `work_status_history` INSERT는 같은 트랜잭션으로 처리한다. 현재 인증 주체를 작업 서비스에 전달하는 구조가 없으므로 변경 주체는 `SYSTEM`으로 기록한다.

## 3. 단위 테스트 범위

- `WorkOrderServiceTest`: 승인, 반려, 작업 시작·완료, 입차 전 시작 거부, 작업 중이 아닌 완료 거부, 상태 이력 값 검증 — 6건
- `GateLogServiceTest`: 정상 입차·출차, 미승인 작업 거부, 완료 전 출차 거부, 반출 불가 컨테이너 거부, 예외 로그 코드 저장 — 5건
- 기존 애플리케이션 컨텍스트 테스트 — 1건

## 4. 실행 결과

```text
./gradlew.bat test
BUILD SUCCESSFUL
총 12건 / 실패 0건 / 오류 0건
```

```text
npm run build
Vite build successful
```

DB 구조 조회 결과 `work_status_history`는 `history_id`, `work_order_id`, `prev_status`, `new_status`, `changed_time`, `changed_by`, `reason`, `remark` 8개 컬럼으로 확인했다. 초기화 이후 상태 변경 요청을 실행하지 않았기 때문에 현재 이력 행 수는 0건이다.

## 5. 변경 파일

- `backend/portprj/src/main/java/aaa/work_order_p/model/WorkStatusHistoryDTO.java`
- `backend/portprj/src/main/java/aaa/work_order_p/model/WorkStatusHistoryMapper.java`
- `backend/portprj/src/main/java/aaa/work_order_p/service/WorkOrderService.java`
- `backend/portprj/src/main/java/aaa/gate_log_p/service/GateLogService.java`
- `backend/portprj/src/main/java/aaa/plate_recognition_p/service/PlateRecognitionService.java`
- `backend/portprj/src/test/java/aaa/work_order_p/service/WorkOrderServiceTest.java`
- `backend/portprj/src/test/java/aaa/gate_log_p/service/GateLogServiceTest.java`
- 관련 DB SQL 및 관리자 작업 화면 코드

이번 검증은 기존 DB 데이터를 변경하지 않기 위해 실제 승인·입차·출차 요청을 재실행하지 않고, Mockito 기반 단위 테스트와 빌드/DB 구조 조회로 확인했다.
