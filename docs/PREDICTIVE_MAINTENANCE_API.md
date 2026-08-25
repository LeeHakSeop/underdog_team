# 예지보전 DB·API 최소 구성

현재 단계에서는 예지보전 전용 테이블을 세 개만 사용한다.

| 테이블 | 역할 |
|---|---|
| `pm_equipment` | 안테나 코드, 위치, 운영 여부 등 장비 기준정보 |
| `pm_sensor_data` | CSV 센서값과 같은 시점의 모델 판정값 |
| `pm_event` | 고장 예상, 실제 고장, 수리 완료 및 카카오 발송 결과 |

## 데이터 흐름

```text
CSV/기기 데이터
  -> pm_sensor_data 적재
  -> 예지보전 모델·운영정책 판정
  -> pm_event 사건 생성
  -> 카카오 알림 요청
  -> pm_event.notification_status 갱신
```

`pm_equipment`는 `pm_sensor_data`와 `pm_event`가 어느 안테나의 기록인지 연결한다.
점검·정비 화면은 별도 저장소가 아니라 `pm_event`를 시간순으로 조회해 표시한다.

## 1차 API 범위

Base URL은 `/api/predictive-maintenance`를 사용한다.

| Method | URL | 설명 |
|---|---|---|
| GET | `/equipment` | 안테나 목록 조회 |
| GET | `/equipment/{equipmentCode}` | 안테나 기본정보 조회 |
| GET | `/sensor-data?equipmentCode=&from=&to=` | 그래프용 센서 시계열 조회 |
| POST | `/sensor-data/import` | CSV 적재 요청 |
| GET | `/events?equipmentCode=&eventType=` | 전체 또는 안테나별 사건 이력 조회 |
| POST | `/events` | 모델·관리자 사건 등록 |
| PATCH | `/events/{eventId}/notification` | 카카오 발송 상태 갱신 |

## 사건과 카카오 알림

- `FAILURE_EXPECTED`: 고장 예상 진입 사건
- `FAILURE`: 실제 고장 확정 사건
- `MAINTENANCE_COMPLETED`: 수리 완료 사건
- `RECOVERY`: 고장 후 회복 판정 사건

현재 카카오 발송 대상은 `FAILURE_EXPECTED`, `FAILURE`로 제한한다.
발송 전에는 `PENDING`, 성공하면 `SENT`, 실패하면 `FAILED`로 같은 `pm_event` 행을 갱신한다.
동일 안테나·사건 유형·발생 시각은 유일 제약으로 중복 저장과 중복 알림을 막는다.

## CSV 필드 처리

V3 원본 센서값은 `pm_sensor_data`의 명시적인 열에 저장한다. 모델 결과인
`current_fault_probability`, `anomaly_count`, `abnormal_sensors`,
`operational_state`도 같은 시각의 행에 함께 저장해 별도 예측 테이블을 만들지 않는다.

원본 CSV의 `failure_within_7d`, `rul_hours`는 현재 운영 판정과 화면에서 사용하지 않으므로
DB 적재 대상에서 제외한다. 고장 예상·실제 고장·정비 시점은 운영정책 판정 후 `pm_event`에 저장한다.

## 직접 CSV 적재하기

백엔드가 실행 중인 상태에서 프로젝트 루트의 PowerShell에 다음 명령을 입력한다.

```powershell
curl.exe -X POST `
  -F "file=@DB\data\01_dashboard_timeseries.csv;type=text/csv" `
  http://localhost/api/predictive-maintenance/sensor-data/import
```

프로젝트에 포함된 `DB/data/01_dashboard_timeseries.csv`는 센서 원본값과 모델 판정 결과가
함께 들어 있다. 화면과 사건 기록을 동일하게 재현하려면 이 파일을 사용한다.

정상 응답 예시:

```json
{
  "sourceFile": "01_dashboard_timeseries.csv",
  "equipmentCount": 24,
  "sensorRowsUpserted": 69120,
  "eventRowsUpserted": 39,
  "firstCollectedAt": "2026-01-01T00:00:00",
  "lastCollectedAt": "2026-04-30T23:00:00"
}
```

적재 결과는 브라우저 또는 PowerShell에서 다음 주소로 확인한다.

```text
GET http://localhost/api/predictive-maintenance/sensor-data/summary
```

DB에서 직접 확인할 때는 다음 쿼리를 사용한다.

```sql
SELECT COUNT(*) FROM pm_equipment;   -- 24
SELECT COUNT(*) FROM pm_sensor_data; -- 69120
SELECT event_type, COUNT(*)
FROM pm_event
GROUP BY event_type
ORDER BY event_type;
```

가져오기는 `(equipment_id, collected_at)`을 기준으로 `ON CONFLICT ... DO UPDATE`를 사용한다.
따라서 수정된 CSV를 다시 올리면 중복 행을 추가하지 않고 기존 시각의 값을 갱신한다.
CSV에서 읽은 사건은 과거 기록이므로 `notification_status = 'DEMO_NOT_SENT'`로 저장하며
카카오톡을 발송하지 않는다.
