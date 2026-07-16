# vehicle(2).csv 기반 전체 테스트 데이터 실행 가이드

## 생성 파일
- `20260716_all_cases_test_data_vehicle_based.sql`: 실제 vehicle ID·번호판을 참조하는 PostgreSQL 테스트 데이터
- `ALL_CASES_TEST_MATRIX_vehicle_based.csv`: 41개 주요 업무 분기 테스트 매트릭스
- `VEHICLE_TEST_REFERENCE.csv`: 이번 테스트에서 참조하는 실제 차량 목록

## 핵심 변경점
이전 버전처럼 910000대의 가상 차량을 새로 만들지 않습니다. 업로드된 `vehicle(2).csv`에서 실제 존재하는 차량 ID와 번호판을 사용합니다. 예: 정상 트랙터 307(서울84사2453), 정상 트레일러 1(01가7431), 점검중 트레일러 6(01고6421), 미등록 트레일러 16(01고8819), 작업중 트레일러 604(171머3292), 정상 카고 2(부산80바1002).

## 실행 전 조건
1. `vehicle(2).csv`의 내용이 vehicle 테이블에 반영되어 있어야 합니다.
2. driver 307·309와 container 1~12가 존재해야 합니다.
3. `20260709_add_tractor_trailer_columns.sql`, `20260710_add_vehicle_driver_user_columns.sql`, `exception_log(fix).sql`이 적용되어 있어야 합니다.
4. 운영 DB가 아니라 테스트 DB 또는 별도 스키마에서 실행하세요.

## 실행
```sql
\i 20260716_all_cases_test_data_vehicle_based.sql
```

## 범위
테스트 데이터 PK는 920000~920999를 사용합니다. vehicle 테이블은 수정하지 않으며, work_order, gate_log, plate_recognition, work_status_history, exception_log의 테스트 행만 생성합니다.

## 포함 분기
작업 상태 전 단계, 동일 트레일러 중복 배정, 점검중·미등록·작업중 차량 배정, 정상 입출차, 미승인 입차, 중복 입차, 입차 없는 출차, 차량 조합 불일치, 게이트 장비 오류, OCR 고·경계·저신뢰도, 미검출, 오인식과 수동 수정, 미등록 번호판, 트랙터·트레일러·UNKNOWN 유형, 동일 게이트 재인식, 컨테이너 출차 보류, 예외 처리 대기·처리중·완료, 정상 및 수동 상태 이력을 포함합니다.

## 주의
`work_order`에 비정상 배차 행도 의도적으로 삽입합니다. DB 제약이나 트리거가 이미 해당 입력을 차단하도록 구현되어 있으면 전체 트랜잭션이 실패할 수 있습니다. 이 경우 SQL의 비정상 배차 행(920011~920014)을 주석 처리하고 API/서비스 계층 테스트로 검증하세요.
