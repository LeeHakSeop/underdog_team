# 항만 게이트 DB 전체 통합 수정본

이 패키지는 기존 DB에 다음 수정본을 순서대로 연결합니다.

1. users/carrier/driver/vehicle 관계 컬럼 및 FK
2. work_order와 gate_log의 트랙터·트레일러 분리 컬럼
3. exception_log 표준화 (`occurred_time`, `vehicle_id`)
4. 기존 exception 더미데이터 100건 안전 연결
5. train(4).txt 전체 8,972개 이미지-번호판 라벨 저장
6. 실제 번호판 기반 운영용 트레일러 200대 + 추가 100대
7. 컨테이너·작업오더·게이트·OCR·상태이력·예외 데이터
8. 저조도·역광·흐림 OCR 예외 시나리오
9. OCR 결과에 따른 exception_log 실시간 자동 생성 트리거
10. 작업 완료 시 트레일러 driver_id/user_id 초기화

## 실행 대상

이 파일은 **이미 기본 테이블이 생성되어 있는 기존 DB**에 적용하는 패치입니다.

필수 기본 테이블:

`users, carrier, driver, vehicle, yard_sector, container, work_order, gate_log, plate_recognition, work_status_history`

## Windows 실행

압축을 푼 폴더에서:

```bat
run_patch_windows.bat
```

또는 직접:

```bat
psql -U postgres -d 데이터베이스명 -f RUN_EXISTING_DB_FULL_PATCH.psql
```

## 실행 파일

실제로 실행해야 하는 것은 `RUN_EXISTING_DB_FULL_PATCH.psql` 하나입니다.
`reference_base_sql` 폴더는 기존 원본 확인용이며 한꺼번에 실행하지 마세요.

## 충돌 방지 처리

- `ADD COLUMN IF NOT EXISTS`
- FK 존재 여부 검사
- 실제 존재하지 않는 작업/게이트 ID는 예외 데이터에서 NULL 처리
- 번호판·컨테이너·작업오더 중복 방지
- `occured_time` 오타를 `occurred_time`으로 통일
- 기존 exception 복합 UNIQUE 제거
- 동일 OCR 예외 30초 중복 방지
- 모든 주요 PK 시퀀스 재정렬
- `korean_plate_character` 테이블 제거

## 적용 후 확인

```bat
psql -U postgres -d 데이터베이스명 -f VERIFY_AFTER_PATCH.sql
```

## 2026-07-15 OCR 예외 트리거 개선

`plate_recognition`에 OCR 결과가 최초 저장되면 다음 정보를 자동으로 조회하여 `exception_log`에 연결합니다.

- `gate_log_id`: OCR 결과의 게이트 출입 이력
- `vehicle_id`: 인식 번호판과 일치하는 차량
- `work_order_id`: 해당 트레일러의 최근 진행 중 작업오더

자동 예외 유형:

- `PLATE_RECOGNITION_FAILED`: OCR 인식 실패
- `LOW_CONFIDENCE`: 신뢰도 70 미만 또는 NULL
- `UNREGISTERED_VEHICLE`: DB에 등록되지 않은 번호판

동일 게이트에서 같은 예외가 30초 이내 반복되면 중복 저장하지 않습니다. 관리자 수동 수정으로 예외가 다시 생성되지 않도록 트리거는 `AFTER INSERT`에만 동작합니다.
