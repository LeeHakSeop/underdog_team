# API 정의서

항만 게이트 차량 출입 및 컨테이너 상차 섹터 안내 시스템

작성일: 2026. 7. 13  
작성 기준: 현재 Spring Boot Backend Controller 및 DTO 기준

---

## - 목 차 -

1. 총칙
   - 1.1 목적
   - 1.2 적용 범위
   - 1.3 API 공통 규칙
2. API 구성
   - 2.1 API 분류
   - 2.2 공통 요청/응답 기준
3. API 목록
4. API 상세 정의
   - 4.1 인증 API
   - 4.2 운송사 API
   - 4.3 기사 API
   - 4.4 차량 API
   - 4.5 작업지시 API
   - 4.6 게이트 출입 API
   - 4.7 번호판 인식 API
   - 4.8 컨테이너 API
   - 4.9 야드 섹터 API
   - 4.10 예외 로그 API
   - 4.11 대시보드 API
   - 4.12 OCR 연계 API
5. 오류 처리 기준
6. 주요 DTO 정의

---

## 1. 총칙

### 1.1 목적

본 정의서는 항만 게이트 차량 출입 및 컨테이너 상차 섹터 안내 시스템에서 제공하는 Backend REST API의 요청 방식, URL, 요청 파라미터, 응답 데이터, 처리 기준을 정의한다.

본 시스템은 운송사, 기사, 차량, 컨테이너, 작업지시, 게이트 출입, 번호판 인식, 야드 섹터 안내, 예외 상황 관리 기능을 제공하며, Frontend 및 AI OCR 시스템과의 안정적인 연계를 목적으로 한다.

### 1.2 적용 범위

| 구분 | 내용 |
|---|---|
| Backend | Spring Boot |
| Frontend | Vue.js 또는 React.js 기반 화면 |
| DBMS | PostgreSQL |
| AI 연계 | 번호판 OCR 인식 시스템 |
| API 형식 | REST API |
| 데이터 형식 | JSON, Multipart Form Data |

### 1.3 API 공통 규칙

| 항목 | 기준 |
|---|---|
| Base URL | `/api` |
| 문자 인코딩 | UTF-8 |
| 기본 Content-Type | `application/json` |
| 파일 업로드 Content-Type | `multipart/form-data` |
| 인증 방식 | JWT Bearer Token 사용 |
| 인증 Header | `Authorization: Bearer {accessToken}` |
| 날짜/시간 형식 | ISO-8601 형식, 예: `2026-07-13T10:30:00` |

---

## 2. API 구성

### 2.1 API 분류

| 분류 | Base Path | 주요 기능 |
|---|---|---|
| 인증 | `/api/auth` | 로그인, 회원가입, 사용자 조회, 사용자 상태 변경 |
| 운송사 | `/api/carrier` | 운송사 등록, 조회, 수정, 삭제 |
| 기사 | `/api/driver` | 기사 등록, 조회, 수정, 삭제, 기사 작업지시 조회 |
| 차량 | `/api/vehicle` | 차량 등록, 조회, 승인, 수정, 삭제 |
| 작업지시 | `/api/work-order` | 작업지시 등록, 조회, 승인, 시작, 완료 |
| 게이트 출입 | `/api/gate-log` | 게이트 출입 이력 조회, 입출차 처리 |
| 번호판 인식 | `/api/plate-recognition` | OCR 인식, 수동 보정 |
| 컨테이너 | `/api/container` | 컨테이너 목록 조회 |
| 야드 섹터 | `/api/yard-sector` | 야드 섹터 목록 조회 |
| 예외 로그 | `/api/exception-log` | 예외 내역 조회, 처리 |
| 대시보드 | `/api/dashboard` | 관리자 대시보드 조회 |
| OCR 연계 | `/api/ocr` | 외부 OCR 결과 수신 |

### 2.2 공통 응답 기준

현재 구현 기준으로 API별 DTO 또는 기본 타입을 직접 반환한다. 향후 공통 응답 포맷을 적용할 경우 다음 형식을 사용할 수 있다.

| 필드 | 타입 | 설명 |
|---|---|---|
| success | Boolean | 요청 처리 성공 여부 |
| data | Object | 응답 데이터 |
| message | String | 처리 메시지 |
| errorCode | String | 오류 코드 |

---

## 3. API 목록

| No | 분류 | API명 | Method | URL |
|---:|---|---|---|---|
| 1 | 인증 | 로그인 | POST | `/api/auth/login` |
| 2 | 인증 | 회원가입 | POST | `/api/auth/register` |
| 3 | 인증 | 관리자 계정 생성 | POST | `/api/auth/admin-init` |
| 4 | 인증 | 사용자 목록 조회 | GET | `/api/auth/users` |
| 5 | 인증 | 사용자 상태 변경 | PATCH | `/api/auth/users/{userId}/status` |
| 6 | 운송사 | 운송사 목록 조회 | GET | `/api/carrier/list` |
| 7 | 운송사 | 운송사 상세 조회 | GET | `/api/carrier/detail/{carrierId}` |
| 8 | 운송사 | 운송사 등록 | POST | `/api/carrier/reg` |
| 9 | 운송사 | 운송사 수정 | PUT | `/api/carrier/modify/{carrierId}` |
| 10 | 운송사 | 운송사 삭제 | DELETE | `/api/carrier/delete/{carrierId}` |
| 11 | 기사 | 기사 목록 조회 | GET | `/api/driver/list` |
| 12 | 기사 | 기사 상세 조회 | GET | `/api/driver/detail/{driverId}` |
| 13 | 기사 | 기사 등록 | POST | `/api/driver/reg` |
| 14 | 기사 | 운송사 승인 처리 | PATCH | `/api/driver/{userId}/carrier-approve` |
| 15 | 기사 | 기사 수정 | PUT | `/api/driver/modify/{driverId}` |
| 16 | 기사 | 기사 삭제 | DELETE | `/api/driver/delete/{driverId}` |
| 17 | 기사 | 기사 작업지시 조회 | GET | `/api/driver/my-work-orders?userName={userName}` |
| 18 | 기사 | 사용자 ID 기준 작업지시 조회 | GET | `/api/driver/my-work-orders/user/{userId}` |
| 19 | 차량 | 차량 목록 조회 | GET | `/api/vehicle/list` |
| 20 | 차량 | 차량 상세 조회 | GET | `/api/vehicle/detail/{vehicleId}` |
| 21 | 차량 | 트랙터 정보 조회 | GET | `/api/vehicle/tractor-info/{plateNumber}` |
| 22 | 차량 | 운송사별 차량 조회 | GET | `/api/vehicle/carrier/{carrierId}` |
| 23 | 차량 | 기사별 차량 조회 | GET | `/api/vehicle/driver/{driverId}` |
| 24 | 차량 | 차량 등록 | POST | `/api/vehicle/reg` |
| 25 | 차량 | 차량 승인 상태 변경 | PATCH | `/api/vehicle/{vehicleId}/approval` |
| 26 | 차량 | 차량 수정 | PUT | `/api/vehicle/modify/{vehicleId}` |
| 27 | 차량 | 차량 삭제 | DELETE | `/api/vehicle/delete/{vehicleId}` |
| 28 | 작업지시 | 작업지시 목록 조회 | GET | `/api/work-order` |
| 29 | 작업지시 | 작업지시 상세 조회 | GET | `/api/work-order/{workOrderId}` |
| 30 | 작업지시 | 작업지시 등록 | POST | `/api/work-order` |
| 31 | 작업지시 | 트레일러 작업 정보 조회 | GET | `/api/work-order/trailer-info/{vehicleId}` |
| 32 | 작업지시 | 작업지시 승인 | PATCH | `/api/work-order/{workOrderId}/approve` |
| 33 | 작업지시 | 작업 시작 | PATCH | `/api/work-order/{workOrderId}/start` |
| 34 | 작업지시 | 작업 완료 | PATCH | `/api/work-order/{workOrderId}/complete` |
| 35 | 게이트 출입 | 게이트 로그 목록 조회 | GET | `/api/gate-log` |
| 36 | 게이트 출입 | 게이트 입출차 처리 | POST | `/api/gate-log/process` |
| 37 | 번호판 인식 | 번호판 OCR 인식 | POST | `/api/plate-recognition/recognize` |
| 38 | 번호판 인식 | 번호판 수동 보정 | PATCH | `/api/plate-recognition/{plateRecognitionId}/manual-correction` |
| 39 | 컨테이너 | 컨테이너 목록 조회 | GET | `/api/container` |
| 40 | 야드 섹터 | 야드 섹터 목록 조회 | GET | `/api/yard-sector` |
| 41 | 예외 로그 | 예외 로그 목록 조회 | GET | `/api/exception-log` |
| 42 | 예외 로그 | 예외 로그 처리 | PUT | `/api/exception-log/{exceptionLogId}/process` |
| 43 | 대시보드 | 관리자 대시보드 조회 | GET | `/api/dashboard/admin` |
| 44 | OCR 연계 | OCR 결과 수신 | POST | `/api/ocr` |

---

## 4. API 상세 정의

### 4.1 인증 API

#### 4.1.1 로그인

| 항목 | 내용 |
|---|---|
| API명 | 로그인 |
| Method | POST |
| URL | `/api/auth/login` |
| 설명 | 사용자 ID, 비밀번호, 권한 정보를 이용하여 로그인 처리 후 JWT 토큰을 반환한다. |
| Request Body | `LoginDTO` |
| Response Body | `LoginResponseDTO` |

Request Body

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| loginId | String | Y | 로그인 ID |
| password | String | Y | 비밀번호 |
| roleCode | String | N | 사용자 권한 코드 |

Response Body

| 필드 | 타입 | 설명 |
|---|---|---|
| userId | Long | 사용자 고유 식별자 |
| loginId | String | 로그인 ID |
| userName | String | 사용자명 |
| roleCode | String | 권한 코드 |
| status | String | 사용자 상태 |
| token | String | JWT 토큰 |
| accessToken | String | JWT Access Token |
| tokenType | String | 토큰 타입, 기본값 `Bearer` |

#### 4.1.2 회원가입

| 항목 | 내용 |
|---|---|
| API명 | 회원가입 |
| Method | POST |
| URL | `/api/auth/register` |
| 설명 | 사용자, 운송사, 기사, 차량 등록 정보를 받아 회원가입을 처리한다. |
| Request Body | `RegisterDTO` |
| Response Body | `{ "message": "..." }` |

#### 4.1.3 사용자 상태 변경

| 항목 | 내용 |
|---|---|
| API명 | 사용자 상태 변경 |
| Method | PATCH |
| URL | `/api/auth/users/{userId}/status` |
| 설명 | 사용자 계정의 승인, 대기, 비활성 등의 상태값을 변경한다. |
| Path Variable | `userId` |
| Request Body | `{ "status": "APPROVED" }` |
| Response Body | `UserDTO` |

### 4.2 운송사 API

| API명 | Method | URL | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| 운송사 목록 조회 | GET | `/api/carrier/list` | - | `List<CarrierDTO>` | 등록된 운송사 목록을 조회한다. |
| 운송사 상세 조회 | GET | `/api/carrier/detail/{carrierId}` | `carrierId` | `CarrierDTO` | 특정 운송사의 상세 정보를 조회한다. |
| 운송사 등록 | POST | `/api/carrier/reg` | `CarrierDTO` | `CarrierDTO` | 운송사 정보를 신규 등록한다. |
| 운송사 수정 | PUT | `/api/carrier/modify/{carrierId}` | `carrierId`, `CarrierDTO` | `CarrierDTO` | 운송사 정보를 수정한다. |
| 운송사 삭제 | DELETE | `/api/carrier/delete/{carrierId}` | `carrierId` | `int` | 운송사 정보를 삭제한다. |

### 4.3 기사 API

| API명 | Method | URL | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| 기사 목록 조회 | GET | `/api/driver/list` | - | `List<DriverDTO>` | 등록된 기사 목록을 조회한다. |
| 기사 상세 조회 | GET | `/api/driver/detail/{driverId}` | `driverId` | `DriverDTO` | 특정 기사 상세 정보를 조회한다. |
| 기사 등록 | POST | `/api/driver/reg` | `DriverDTO` | `DriverDTO` | 기사 정보를 신규 등록한다. |
| 운송사 승인 처리 | PATCH | `/api/driver/{userId}/carrier-approve` | `userId` | - | 기사 사용자를 운송사 기준으로 승인 처리한다. |
| 기사 수정 | PUT | `/api/driver/modify/{driverId}` | `driverId`, `DriverDTO` | `DriverDTO` | 기사 정보를 수정한다. |
| 기사 삭제 | DELETE | `/api/driver/delete/{driverId}` | `driverId` | `int` | 기사 정보를 삭제한다. |
| 기사 작업지시 조회 | GET | `/api/driver/my-work-orders` | Query: `userName` | `List<DriverWorkOrderDTO>` | 기사 이름 기준으로 배정된 작업지시를 조회한다. |
| 사용자 ID 기준 작업지시 조회 | GET | `/api/driver/my-work-orders/user/{userId}` | `userId` | `List<DriverWorkOrderDTO>` | 사용자 ID 기준으로 배정된 작업지시를 조회한다. |

### 4.4 차량 API

| API명 | Method | URL | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| 차량 목록 조회 | GET | `/api/vehicle/list` | - | `List<VehicleDTO>` | 등록된 차량 목록을 조회한다. |
| 차량 상세 조회 | GET | `/api/vehicle/detail/{vehicleId}` | `vehicleId` | `VehicleDTO` | 특정 차량 상세 정보를 조회한다. |
| 트랙터 정보 조회 | GET | `/api/vehicle/tractor-info/{plateNumber}` | `plateNumber` | `TractorVehicleInfoDTO` | 차량번호 기준 트랙터 및 기사/운송사 정보를 조회한다. |
| 운송사별 차량 조회 | GET | `/api/vehicle/carrier/{carrierId}` | `carrierId` | `List<VehicleDTO>` | 운송사 ID 기준 차량 목록을 조회한다. |
| 기사별 차량 조회 | GET | `/api/vehicle/driver/{driverId}` | `driverId` | `VehicleDTO` | 기사 ID 기준 차량 정보를 조회한다. |
| 차량 등록 | POST | `/api/vehicle/reg` | `VehicleDTO` | `VehicleDTO` | 차량 정보를 신규 등록한다. |
| 차량 승인 상태 변경 | PATCH | `/api/vehicle/{vehicleId}/approval` | `vehicleId`, `VehicleDTO` | `VehicleDTO` | 차량 등록 승인 또는 상태를 변경한다. |
| 차량 수정 | PUT | `/api/vehicle/modify/{vehicleId}` | `vehicleId`, `VehicleDTO` | `VehicleDTO` | 차량 정보를 수정한다. |
| 차량 삭제 | DELETE | `/api/vehicle/delete/{vehicleId}` | `vehicleId` | `int` | 차량 정보를 삭제한다. |

### 4.5 작업지시 API

| API명 | Method | URL | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| 작업지시 목록 조회 | GET | `/api/work-order` | - | `List<WorkOrderDTO>` | 전체 작업지시 목록을 조회한다. |
| 작업지시 상세 조회 | GET | `/api/work-order/{workOrderId}` | `workOrderId` | `WorkOrderDTO` | 특정 작업지시 상세 정보를 조회한다. |
| 작업지시 등록 | POST | `/api/work-order` | `WorkOrderDTO` | `WorkOrderDTO` | 신규 작업지시를 등록한다. |
| 트레일러 작업 정보 조회 | GET | `/api/work-order/trailer-info/{vehicleId}` | `vehicleId` | `TrailerWorkInfoDTO` | 트레일러 차량 기준 작업 및 야드 정보를 조회한다. |
| 작업지시 승인 | PATCH | `/api/work-order/{workOrderId}/approve` | `workOrderId` | `WorkOrderDTO` | 작업지시를 승인 처리한다. |
| 작업 시작 | PATCH | `/api/work-order/{workOrderId}/start` | `workOrderId` | `WorkOrderProcessResultDTO` | 작업지시 상태를 작업 시작으로 변경한다. |
| 작업 완료 | PATCH | `/api/work-order/{workOrderId}/complete` | `workOrderId` | `WorkOrderProcessResultDTO` | 작업지시 상태를 작업 완료로 변경한다. |

### 4.6 게이트 출입 API

#### 4.6.1 게이트 로그 목록 조회

| 항목 | 내용 |
|---|---|
| API명 | 게이트 로그 목록 조회 |
| Method | GET |
| URL | `/api/gate-log` |
| 설명 | 차량 입출차 이력 목록을 조회한다. |
| Response Body | `List<GateLogDTO>` |

#### 4.6.2 게이트 입출차 처리

| 항목 | 내용 |
|---|---|
| API명 | 게이트 입출차 처리 |
| Method | POST |
| URL | `/api/gate-log/process` |
| 설명 | 트랙터, 트레일러, 작업지시, 컨테이너, 야드 정보를 기준으로 게이트 입출차를 처리한다. |
| Request Body | `GateProcessRequestDTO` |
| Response Body | `GateProcessResultDTO` |

Request Body

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| tractorVehicleId | Long | N | 트랙터 차량 ID |
| trailerVehicleId | Long | N | 트레일러 차량 ID |
| workOrderId | Long | N | 작업지시 ID |
| containerId | Long | N | 컨테이너 ID |
| sectorId | Long | N | 야드 섹터 ID |
| gateNumber | String | Y | 게이트 번호 |
| gateName | String | N | 게이트명 |
| inOutType | String | Y | 입출차 구분, 예: `IN`, `OUT` |

Response Body

| 필드 | 타입 | 설명 |
|---|---|---|
| success | Boolean | 처리 성공 여부 |
| gateLogId | Long | 생성 또는 처리된 게이트 로그 ID |
| workOrderId | Long | 작업지시 ID |
| workStatus | String | 작업 상태 |
| processResult | String | 처리 결과 |
| exceptionType | String | 예외 유형 |
| message | String | 처리 메시지 |

### 4.7 번호판 인식 API

#### 4.7.1 번호판 OCR 인식

| 항목 | 내용 |
|---|---|
| API명 | 번호판 OCR 인식 |
| Method | POST |
| URL | `/api/plate-recognition/recognize` |
| Content-Type | `multipart/form-data` |
| 설명 | 차량 이미지를 업로드하여 AI 번호판 인식을 수행하고, 차량/기사/운송사/작업지시 매칭 결과를 반환한다. |
| Response Body | `PlateRecognitionResultDTO` |

Request Parameter

| 필드 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| file | MultipartFile | Y | - | 차량 또는 번호판 이미지 파일 |
| ocrType | String | N | `paddle` | OCR 엔진 유형 |
| plateType | String | N | `TRAILER` | 번호판 유형 |
| gateNumber | String | N | `G01` | 게이트 번호 |
| gateName | String | N | `AI_GATE` | 게이트명 |
| inOutType | String | N | `IN` | 입출차 구분 |

#### 4.7.2 번호판 수동 보정

| 항목 | 내용 |
|---|---|
| API명 | 번호판 수동 보정 |
| Method | PATCH |
| URL | `/api/plate-recognition/{plateRecognitionId}/manual-correction` |
| 설명 | OCR 인식 실패 또는 오인식 건에 대해 관리자가 번호판을 수동 보정한다. |
| Path Variable | `plateRecognitionId` |
| Request Body | `ManualCorrectionRequestDTO` |
| Response Body | `PlateRecognitionDTO` |

Request Body

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| manualCorrection | String | Y | 관리자가 보정한 번호판 값 |

### 4.8 컨테이너 API

| API명 | Method | URL | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| 컨테이너 목록 조회 | GET | `/api/container` | - | `List<ContainerDTO>` | 등록된 컨테이너 목록을 조회한다. |

### 4.9 야드 섹터 API

| API명 | Method | URL | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| 야드 섹터 목록 조회 | GET | `/api/yard-sector` | - | `List<YardSectorDTO>` | 야드 섹터 목록 및 안내 정보를 조회한다. |

### 4.10 예외 로그 API

| API명 | Method | URL | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| 예외 로그 목록 조회 | GET | `/api/exception-log` | - | `List<ExceptionLogDTO>` | 시스템 예외 및 OCR/게이트 처리 예외 목록을 조회한다. |
| 예외 로그 처리 | PUT | `/api/exception-log/{exceptionLogId}/process` | `exceptionLogId`, `ExceptionLogDTO` | `int` | 예외 로그의 처리 상태와 관리자 조치 내용을 저장한다. |

### 4.11 대시보드 API

| API명 | Method | URL | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| 관리자 대시보드 조회 | GET | `/api/dashboard/admin` | - | `DashboardDTO` | 관리자 화면에 필요한 요약, 작업 현황, 최근 작업, 섹터 정보를 조회한다. |

### 4.12 OCR 연계 API

| 항목 | 내용 |
|---|---|
| API명 | OCR 결과 수신 |
| Method | POST |
| URL | `/api/ocr` |
| 설명 | 외부 OCR 시스템에서 인식한 번호판 결과를 수신한다. |
| Request Body | `OcrDTO` |
| Response Body | `{ "msg": "success" }` |

Request Body

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| plateNumber | String | Y | OCR로 인식된 번호판 |

---

## 5. 오류 처리 기준

| HTTP Status | 오류 유형 | 설명 | 처리 기준 |
|---|---|---|---|
| 400 | Bad Request | 필수 요청값 누락 또는 형식 오류 | 요청 파라미터 및 Body 값을 확인한다. |
| 401 | Unauthorized | 인증 정보 없음 또는 토큰 오류 | 로그인 후 발급받은 JWT 토큰을 Header에 포함한다. |
| 403 | Forbidden | 권한 부족 | 사용자 권한 또는 승인 상태를 확인한다. |
| 404 | Not Found | 대상 데이터 없음 | 요청 ID에 해당하는 데이터 존재 여부를 확인한다. |
| 409 | Conflict | 중복 또는 상태 충돌 | 차량번호, 로그인 ID 등 고유값 중복 여부를 확인한다. |
| 500 | Internal Server Error | 서버 내부 오류 | 서버 로그 및 예외 로그를 확인한다. |

---

## 6. 주요 DTO 정의

### 6.1 CarrierDTO

| 필드 | 타입 | 설명 |
|---|---|---|
| carrierId | Long | 운송사 고유 식별자 |
| carrierName | String | 운송사명 |
| carrierContact | String | 운송사 연락처 |
| managerName | String | 운송사 담당자명 |
| carrierStatus | String | 운송사 상태 |
| userId | Long | 사용자 ID |

### 6.2 DriverDTO

| 필드 | 타입 | 설명 |
|---|---|---|
| driverId | Long | 기사 고유 식별자 |
| driverName | String | 기사명 |
| driverContact | String | 기사 연락처 |
| isRegistered | Boolean | 등록 여부 |
| carrierId | Long | 소속 운송사 ID |
| canEnter | Boolean | 게이트 출입 가능 여부 |
| userId | Long | 사용자 ID |

### 6.3 VehicleDTO

| 필드 | 타입 | 설명 |
|---|---|---|
| vehicleId | Long | 차량 고유 식별자 |
| plateNumber | String | 차량 번호판 |
| vehicleType | String | 차량 유형 |
| tonnage | String | 적재 중량 |
| isRegistered | Boolean | 차량 등록 여부 |
| vehicleStatus | String | 차량 상태 |
| tractorNo | String | 트랙터 번호 |
| chassisNo | String | 섀시 번호 |
| driverId | Long | 기사 ID |
| carrierId | Long | 운송사 ID |
| userId | Long | 사용자 ID |

### 6.4 WorkOrderDTO

| 필드 | 타입 | 설명 |
|---|---|---|
| workOrderId | Long | 작업지시 고유 식별자 |
| workType | String | 작업 유형 |
| vehicleId | Long | 차량 ID |
| tractorVehicleId | Long | 트랙터 차량 ID |
| trailerVehicleId | Long | 트레일러 차량 ID |
| driverId | Long | 기사 ID |
| containerId | Long | 컨테이너 ID |
| reservedTime | LocalDateTime | 예약 시간 |
| workStatus | String | 작업 상태 |
| isApproved | Boolean | 승인 여부 |

### 6.5 GateLogDTO

| 필드 | 타입 | 설명 |
|---|---|---|
| gateLogId | Long | 게이트 로그 고유 식별자 |
| vehicleId | Long | 차량 ID |
| tractorVehicleId | Long | 트랙터 차량 ID |
| trailerVehicleId | Long | 트레일러 차량 ID |
| gateNumber | String | 게이트 번호 |
| gateName | String | 게이트명 |
| entryTime | LocalDateTime | 입차 시간 |
| exitTime | LocalDateTime | 출차 시간 |
| inOutType | String | 입출차 구분 |
| processResult | String | 처리 결과 |
| managerCheck | Boolean | 관리자 확인 여부 |

### 6.6 PlateRecognitionDTO

| 필드 | 타입 | 설명 |
|---|---|---|
| plateRecognitionId | Long | 번호판 인식 고유 식별자 |
| gateLogId | Long | 게이트 로그 ID |
| vehicleImage | String | 차량 이미지 경로 또는 파일명 |
| recognizedPlate | String | 인식된 번호판 |
| plateType | String | 번호판 유형 |
| isSuccess | Boolean | 인식 성공 여부 |
| confidence | BigDecimal | 인식 신뢰도 |
| manualCorrection | String | 수동 보정 번호판 |
| errorMessage | String | 오류 메시지 |
| recognitionTime | LocalDateTime | 인식 시간 |

### 6.7 ContainerDTO

| 필드 | 타입 | 설명 |
|---|---|---|
| containerId | Long | 컨테이너 고유 식별자 |
| containerNumber | String | 컨테이너 번호 |
| containerSize | String | 컨테이너 크기 |
| containerLocation | String | 컨테이너 위치 |
| sectorId | Long | 야드 섹터 ID |
| block | String | 블록 |
| bay | String | 베이 |
| rowNo | String | 로우 번호 |
| canExit | Boolean | 반출 가능 여부 |
| sealNumber | String | 씰 번호 |
| shippingLine | String | 선사 |

### 6.8 YardSectorDTO

| 필드 | 타입 | 설명 |
|---|---|---|
| sectorId | Long | 섹터 고유 식별자 |
| sectorName | String | 섹터명 |
| blockName | String | 블록명 |
| sectorStatus | String | 섹터 상태 |
| waitingVehicleCount | Integer | 대기 차량 수 |
| guideMessage | String | 안내 메시지 |
| altWaitingArea | String | 대체 대기 구역 |

### 6.9 ExceptionLogDTO

| 필드 | 타입 | 설명 |
|---|---|---|
| exceptionLogId | Long | 예외 로그 고유 식별자 |
| gateLogId | Long | 게이트 로그 ID |
| vehicleId | Long | 차량 ID |
| plateNumber | String | 차량 번호판 |
| exceptionType | String | 예외 유형 |
| exceptionMessage | String | 예외 메시지 |
| occurredTime | LocalDateTime | 발생 시간 |
| processStatus | String | 처리 상태 |
| managerAction | String | 관리자 조치 내용 |
| processedTime | LocalDateTime | 처리 시간 |

---

## 작성 시 보완 필요 사항

| 항목 | 보완 내용 |
|---|---|
| 권한 기준 | ADMIN, CARRIER, DRIVER 등 역할별 API 접근 가능 여부 정리 필요 |
| 상태 코드 | 실제 서비스/예외 처리 로직 기준 응답 코드 확정 필요 |
| Enum 값 | `workStatus`, `vehicleStatus`, `inOutType`, `carrierStatus` 등의 허용값 정리 필요 |
| 예시 데이터 | 화면 또는 테스트 데이터 기준 Request/Response 예시 추가 필요 |
| 공통 응답 포맷 | 현재는 DTO 직접 반환 방식이므로, 공통 응답 래퍼 적용 여부 결정 필요 |
