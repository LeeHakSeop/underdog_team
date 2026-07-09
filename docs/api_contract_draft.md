# API 계약 초안

## 목적

김보현님, 박준희님 기능을 병합할 때 프론트와 백엔드가 맞춰야 할 요청/응답 구조 초안이다. 실제 구현된 API가 확정되면 이 문서를 기준으로 수정한다.

## 현재 구현된 기사 작업 조회 API

### GET /api/driver/my-work-orders

기사 로그인 후 본인 작업정보를 조회한다.

현재 임시 기준은 `userName`이다. 추후 `driver.user_id`가 확정되면 `userId` 또는 `driverId` 기준으로 변경한다.

Request:

```http
GET /api/driver/my-work-orders?userName=홍길동
```

Response:

```json
[
  {
    "workOrderId": 1,
    "workType": "상차",
    "reservedTime": "2026-07-09T09:00:00",
    "workStatus": "APPROVED",
    "isApproved": true,
    "driverId": 1,
    "driverName": "홍길동",
    "driverContact": "010-0000-0000",
    "canEnter": true,
    "carrierId": 1,
    "carrierName": "대한운송",
    "carrierContact": "051-000-0000",
    "vehicleId": 46,
    "plateNumber": "01나0666",
    "vehicleType": "TRACTOR",
    "vehicleStatus": "정상",
    "containerId": 10,
    "containerNumber": "MSCU0000010",
    "containerSize": "40FT",
    "containerLocation": "A-10",
    "block": "A",
    "bay": "10",
    "rowNo": "10",
    "sectorId": 10,
    "sectorName": "A-10",
    "sectorStatus": "사용가능",
    "guideMessage": "A-10 섹터로 이동하십시오.",
    "altWaitingArea": "A-WAIT"
  }
]
```

## 김보현님 기능 수령 후 확정 필요

### 로그인 응답

현재:

```json
{
  "userId": 1,
  "loginId": "driver01",
  "userName": "홍길동",
  "roleCode": "DRIVER",
  "token": null
}
```

추가되면 좋은 값:

```json
{
  "driverId": 1,
  "carrierId": 1
}
```

확정 후 프론트 저장값:

```js
localStorage.portGateUser = {
  userId,
  loginId,
  userName,
  roleCode,
  driverId,
  carrierId
}
```

## 박준희님 기능 수령 후 확정 필요

### POST /api/gate-process/enter

입차 최종 처리 API 초안이다.

Request:

```json
{
  "tractorVehicleId": 46,
  "trailerVehicleId": 52,
  "driverId": 3,
  "carrierId": 1,
  "workOrderId": 10,
  "containerId": 8,
  "sectorId": 2,
  "inOutType": "IN"
}
```

Success Response:

```json
{
  "success": true,
  "gateLogId": 15,
  "workOrderId": 10,
  "workStatus": "GATE_IN",
  "message": "입차 처리가 완료되었습니다."
}
```

Fail Response:

```json
{
  "success": false,
  "exceptionType": "WORK_ORDER_NOT_APPROVED",
  "message": "승인되지 않은 작업입니다."
}
```

### POST /api/gate-process/exit

출차 최종 처리 API 초안이다.

Request:

```json
{
  "tractorVehicleId": 46,
  "trailerVehicleId": 52,
  "workOrderId": 10,
  "inOutType": "OUT"
}
```

Success Response:

```json
{
  "success": true,
  "gateLogId": 16,
  "workOrderId": 10,
  "workStatus": "GATE_OUT",
  "message": "출차 처리가 완료되었습니다."
}
```

## 예외 코드 초안

| 코드 | 의미 |
|---|---|
| PLATE_NOT_DETECTED | 번호판 인식 실패 |
| VEHICLE_NOT_REGISTERED | 미등록 차량 |
| VEHICLE_TYPE_MISMATCH | 차량 유형 불일치 |
| DRIVER_NOT_FOUND | 기사 조회 실패 |
| DRIVER_CANNOT_ENTER | 기사 출입 불가 |
| CARRIER_STATUS_NOT_NORMAL | 운송사 상태 이상 |
| WORK_ORDER_NOT_FOUND | 작업정보 없음 |
| WORK_ORDER_NOT_APPROVED | 작업 미승인 |
| CONTAINER_NOT_FOUND | 컨테이너 정보 없음 |
| YARD_SECTOR_NOT_FOUND | 야드섹터 정보 없음 |
