# 예지보전 REST API 계약 초안

Base URL은 `/api/predictive-maintenance`를 사용한다. 모든 목록 API는 `typeCode`를 받아 두 번째 설비 유형도 같은 화면과 호출 구조를 재사용한다.

## 1. 설비 유형과 대시보드

| Method | URL | 설명 |
|---|---|---|
| GET | `/types` | 설비 유형 목록과 활성 여부 조회 |
| GET | `/dashboard?typeCode=ANTENNA` | 위험 현황, 최근 경보, 위험 설비, 정비 일정 조회 |

대시보드의 초기 응답 구조:

```json
{
  "typeCode": "ANTENNA",
  "typeName": "안테나 예지보전",
  "dataAsOf": "2026-04-30T23:00:00",
  "summary": {
    "total": 24,
    "normal": 20,
    "caution": 3,
    "danger": 1,
    "openAlerts": 2
  },
  "riskDistribution": [],
  "riskTrend": [],
  "topRiskEquipment": [],
  "recentAlerts": [],
  "upcomingMaintenance": [],
  "activeModel": null
}
```

## 2. 설비 CRUD

| Method | URL | 설명 |
|---|---|---|
| GET | `/equipment` | 검색·필터·페이징 목록 |
| GET | `/equipment/{equipmentId}` | 설비 기본 정보와 최신 위험 조회 |
| POST | `/equipment` | 설비 등록 |
| PUT | `/equipment/{equipmentId}` | 설비 수정 |
| DELETE | `/equipment/{equipmentId}` | 설비 논리 삭제 |

목록 쿼리 예시:

```text
GET /api/predictive-maintenance/equipment
    ?typeCode=ANTENNA
    &keyword=ANT-001
    &locationCode=TG-01
    &riskLevel=DANGER
    &page=0
    &size=20
```

## 3. 센서 데이터 CRUD

| Method | URL | 설명 |
|---|---|---|
| GET | `/readings` | 설비·기간별 센서 데이터 조회 |
| GET | `/readings/{readingId}` | 센서 데이터 단건 조회 |
| POST | `/readings` | 센서 데이터 등록 |
| PUT | `/readings/{readingId}` | 센서 데이터 수정 |
| DELETE | `/readings/{readingId}` | 센서 데이터 삭제 |
| POST | `/readings/import` | CSV 가져오기(후속 단계) |

안테나 센서 데이터는 자주 사용하는 값을 명시적 필드로 보내고, 두 번째 유형의 미확정 값은 `extraMetrics`로 확장한다.

## 4. 예측 결과

| Method | URL | 설명 |
|---|---|---|
| GET | `/predictions` | 설비·기간·위험 등급별 예측 목록 |
| GET | `/predictions/latest/{equipmentId}` | 설비의 최신 예측과 근거 조회 |
| POST | `/predictions` | AI 서비스의 예측 결과 저장 |

예측 결과 예시:

```json
{
  "equipmentId": 1,
  "predictedAt": "2026-04-30T23:00:00",
  "targetName": "failure_within_7d",
  "predictionHorizonHours": 168,
  "failureProbability": 0.82,
  "riskLevel": "DANGER",
  "rulHours": 96,
  "reasons": [
    { "feature": "successRate", "message": "최근 정상 기준보다 낮습니다." },
    { "feature": "responseTimeMs", "message": "응답시간이 증가했습니다." }
  ]
}
```

## 5. 경보 CRUD와 상태 변경

| Method | URL | 설명 |
|---|---|---|
| GET | `/alerts` | 경보 목록 조회 |
| GET | `/alerts/{alertId}` | 경보 상세 조회 |
| POST | `/alerts` | 경보 등록 |
| PUT | `/alerts/{alertId}` | 담당자·메모 등 수정 |
| PATCH | `/alerts/{alertId}/status` | 경보 처리 상태 변경 |
| DELETE | `/alerts/{alertId}` | 잘못 등록한 수동 경보 삭제(관리자) |

## 6. 점검·정비 CRUD

| Method | URL | 설명 |
|---|---|---|
| GET | `/maintenance` | 점검·정비 목록 조회 |
| GET | `/maintenance/{maintenanceId}` | 상세 조회 |
| POST | `/maintenance` | 일정 또는 작업 등록 |
| PUT | `/maintenance/{maintenanceId}` | 내용 수정 |
| PATCH | `/maintenance/{maintenanceId}/status` | 진행 상태 변경 |
| DELETE | `/maintenance/{maintenanceId}` | 예정 작업 취소 또는 삭제 |

## 7. 모델 정보

| Method | URL | 설명 |
|---|---|---|
| GET | `/models?typeCode=ANTENNA` | 모델 버전과 평가 결과 목록 |
| GET | `/models/active?typeCode=ANTENNA` | 현재 적용 모델 조회 |
| POST | `/models` | 모델 버전 등록 |
| PATCH | `/models/{modelVersionId}/activate` | 적용 모델 변경 |

## 8. 공통 규칙

- 사용자 표시명은 `안테나 예지보전`, 내부 코드는 `ANTENNA`로 유지한다.
- 날짜와 시간은 ISO-8601 문자열로 전달한다.
- 위험 등급은 `NORMAL`, `CAUTION`, `DANGER`를 사용한다.
- 삭제 후 이력을 보존해야 하는 설비는 `isDeleted` 논리 삭제를 사용한다.
- 목록 응답은 `{ content, page, size, totalElements, totalPages }` 구조로 통일한다.
- 모델 결과가 확정되기 전까지 `failureProbability`, `reasons`, `activeModel`은 `null` 또는 빈 배열을 허용한다.

