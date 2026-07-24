# 항만 게이트 Backend 구조 정리

작성일: 2026. 7. 20  
작성 기준: 현재 `src/main/java/aaa` 소스코드

---

## 1. 프로젝트 한 줄 요약 

이 백엔드는 항만 게이트에서 차량, 기사, 운송사, 컨테이너, 작업지시, 번호판 OCR, 게이트 입출차, 예외 처리, 대시보드 기능을 REST API로 제공하는 Spring Boot 서버이다.

전체 흐름은 다음과 같다.

```text
Vue.js 화면
  -> Spring Boot Controller
  -> Service 업무 로직
  -> MyBatis Mapper
  -> PostgreSQL
```

번호판 인식 기능은 중간에 FastAPI AI 서버가 추가된다.

```text
Vue.js 이미지 업로드
  -> PlateRecognitionController
  -> PlateRecognitionService
  -> FastAPI OCR 서버
  -> 차량/기사/운송사/작업지시 매칭
  -> 결과 반환
```

---

## 2. 기본 기술 스택

| 구분 | 내용 |
|---|---|
| Build | Gradle |
| Framework | Spring Boot |
| Security | Spring Security, JWT |
| DB | PostgreSQL |
| DB 접근 | MyBatis Mapper |
| 파일 업로드 | Spring Multipart |
| AI 연계 | FastAPI 번호판 OCR 서버 |
| 설정 파일 | `src/main/resources/application.yaml` |

주요 설정:

| 항목 | 값 |
|---|---|
| 서버 포트 | `80` |
| DB URL | `jdbc:postgresql://localhost:5432/port_db` |
| DB User | `port_user` |
| Multipart 최대 파일 | 10MB |
| Multipart 최대 요청 | 40MB |
| JWT 만료 | 1시간 |
| AI OCR URL | `http://127.0.0.1:8000/api/plate-recognition` |

---

## 3. 패키지 구조 이해하기

프로젝트는 기능별 패키지로 나뉘어 있다.

| 패키지 | 역할 |
|---|---|
| `auth_p` | 로그인, 회원가입, 사용자 상태 관리 |
| `user_p` | 사용자 계정 DTO/Mapper |
| `carrier_p` | 운송사 관리 |
| `driver_p` | 기사 관리, 기사별 작업 조회, 탈퇴/재활성화 |
| `vehicle_p` | 차량 관리, 트랙터 정보 조회 |
| `container_p` | 컨테이너 목록/등록/수정/삭제 |
| `yard_sector_p` | 야드 섹터 조회 |
| `work_order_p` | 작업지시 등록, 승인, 반려, 수정, 취소, 시작, 완료 |
| `gate_log_p` | 게이트 입출차 처리, 게이트 로그 |
| `plate_recognition_p` | 번호판 이미지 OCR 인식, 수동 보정 |
| `exception_log_p` | 예외 로그 조회 및 처리 |
| `dashboard_p` | 관리자 대시보드 통계 |
| `config_p` | 보안, CORS, Web 설정 |
| `filter` | JWT 생성/검증 필터 |

각 기능 패키지는 보통 아래 구조를 가진다.

```text
기능_p/
  controller/   API 입구
  service/      업무 로직
  model/        DTO, Mapper
```

---

## 4. 계층별 역할

### 4.1 Controller

Controller는 API의 입구이다.

주로 하는 일:

- URL 지정
- HTTP Method 지정
- Request Body, Path Variable, Query Parameter 받기
- Service 호출
- 응답 DTO 반환

예:

```text
POST /api/work-order
  -> WorkOrderController.create()
  -> WorkOrderService.insert()
```

### 4.2 Service

Service는 실제 업무 규칙을 처리한다.

주로 하는 일:

- 필수값 검증
- 상태값 검증
- 중복 검사
- 여러 Mapper 호출 묶기
- 트랜잭션 처리
- 예외 발생
- 상태 이력 저장

예:

```text
작업 시작 요청
  -> 작업지시 존재 여부 확인
  -> 승인 여부 확인
  -> 현재 상태가 GATE_IN인지 확인
  -> 컨테이너 반출 불가 처리
  -> 작업 상태 IN_PROGRESS 변경
```

### 4.3 Mapper

Mapper는 DB와 직접 대화한다.

주로 하는 일:

- SELECT
- INSERT
- UPDATE
- DELETE
- 테이블 컬럼과 DTO 필드 매핑

이 프로젝트는 XML Mapper보다는 Java Annotation 기반 MyBatis Mapper를 많이 사용한다.

### 4.4 DTO

DTO는 데이터를 담는 그릇이다.

예:

- `LoginDTO`: 로그인 요청값
- `VehicleDTO`: 차량 정보
- `WorkOrderDTO`: 작업지시 정보
- `GateProcessRequestDTO`: 게이트 처리 요청값
- `PlateRecognitionResultDTO`: OCR 인식 및 업무 매칭 결과

---

## 5. 주요 도메인 관계

시스템의 중심은 작업지시이다.

```text
Carrier(운송사)
  -> Driver(기사)
  -> Vehicle(차량)
  -> WorkOrder(작업지시)
  -> Container(컨테이너)
  -> YardSector(야드 위치)
```

게이트와 OCR은 이 작업지시를 실제 현장 처리 흐름으로 연결한다.
 
```text
PlateRecognition(번호판 인식)
  -> Vehicle 매칭
  -> Driver/Carrier 검증
  -> WorkOrder 검증
  -> GateLog 생성
  -> ExceptionLog 생성 가능
```

---

## 6. 인증/회원 흐름

### 6.1 회원가입

`POST /api/auth/register`

권한별 등록 흐름:

| roleCode | 처리 |
|---|---|
| `ADMIN` | 사용자 계정 생성, 상태 `ACTIVE` |
| `CARRIER` | 사용자 계정 + 운송사 정보 생성, 상태 `PENDING` |
| `DRIVER` | 사용자 계정 + 기사 정보 + 트랙터 차량 정보 생성, 상태 `PENDING` |

주요 규칙:

- `loginId`는 중복 불가
- 비밀번호는 4자 이상
- 권한은 `ADMIN`, `CARRIER`, `DRIVER` 중 하나
- 기사 가입 시 `carrierId`, `driverName`, `driverContact`, `plateNumber`, `tractorNo` 필요

### 6.2 로그인

`POST /api/auth/login`

로그인 성공 조건:

- ID/비밀번호 일치
- 사용자 상태가 `ACTIVE`
- 기사라면 `isRegistered=true`
- 기사라면 `canEnter=true`

성공 시 JWT 토큰을 반환한다.

### 6.3 승인 흐름

운송사:

```text
PENDING
  -> 관리자 승인
  -> ACTIVE
```

기사:

```text
PENDING
  -> 운송사 승인
  -> CARRIER_APPROVED
  -> 관리자 최종 승인
  -> ACTIVE
```

탈퇴:

```text
ACTIVE 또는 기타 상태
  -> /api/driver/{driverId}/withdraw
  -> WITHDRAWN
```

재활성화:

```text
WITHDRAWN
  -> /api/driver/{driverId}/reactivate
  -> PENDING 또는 CARRIER_APPROVED
```

---

## 7. 차량/기사/운송사 관리

### 7.1 운송사

Base URL: `/api/carrier`

기본 CRUD를 제공한다.

- 목록
- 상세
- 등록
- 수정
- 삭제

### 7.2 기사

Base URL: `/api/driver`

기본 관리 외에 다음 기능이 있다.

- 운송사 승인
- 탈퇴 처리
- 재활성화
- 기사별 작업지시 조회

주의:

- `userName` 기준 작업 조회는 동명이인 문제가 있을 수 있다.
- 실제 사용은 `userId` 기준 조회가 더 안전하다.

### 7.3 차량

Base URL: `/api/vehicle`

주요 기능:

- 차량 목록/상세
- 차량 등록/수정/삭제
- 차량 승인 상태 변경
- 번호판 기준 트랙터 정보 조회
- 운송사별 차량 조회
- 기사별 차량 조회

---

## 8. 컨테이너/야드 섹터

### 8.1 컨테이너

Base URL: `/api/container`

현재 기능:

- 목록 조회
- 등록
- 수정
- 삭제

주요 규칙:

- `containerNumber`는 필수
- `containerNumber`는 중복 불가
- `sectorId`가 있으면 실제 야드 섹터에 존재해야 함
- `canExit` 기본값은 `true`
- 연결된 작업지시가 있으면 컨테이너 삭제 불가

### 8.2 야드 섹터

Base URL: `/api/yard-sector`

현재는 목록 조회 중심이다.

야드 섹터는 컨테이너 위치 안내, 대기 차량 수, 대체 대기 구역 안내에 사용된다.

---

## 9. 작업지시 흐름

Base URL: `/api/work-order`

작업지시는 이 시스템의 핵심이다.

### 9.1 작업지시 상태

| 상태 | 의미 |
|---|---|
| `DISPATCH_WAITING` | 배차 승인 대기 |
| `APPROVED` | 관리자 승인 완료 |
| `GATE_IN` | 게이트 입차 완료 |
| `IN_PROGRESS` | 야드 작업 진행 중 |
| `COMPLETED` | 작업 완료 |
| `GATE_OUT` | 게이트 출차 완료 |
| `CANCELED` | 취소/반려 |

### 9.2 생성

`POST /api/work-order`

기본값:

- `workStatus` 없으면 `DISPATCH_WAITING`
- `isApproved` 없으면 `false`

중복 배정 방지:

- 한 기사가 동시에 활성 작업 여러 개를 가질 수 없음
- 한 트레일러가 동시에 활성 작업 여러 개에 배정될 수 없음

### 9.3 수정

`PUT /api/work-order/{workOrderId}`

규칙:

- `DISPATCH_WAITING` 상태에서만 수정 가능
- 수정 후에도 상태는 `DISPATCH_WAITING`
- 승인 여부는 `false`

### 9.4 취소

`DELETE /api/work-order/{workOrderId}`

규칙:

- `DISPATCH_WAITING` 상태에서만 취소 가능
- 상태는 `CANCELED`
- 상태 이력 저장

### 9.5 승인/반려

승인:

```text
PATCH /api/work-order/{workOrderId}/approve
DISPATCH_WAITING -> APPROVED
```

반려:

```text
PATCH /api/work-order/{workOrderId}/reject
DISPATCH_WAITING -> CANCELED
```

### 9.6 작업 시작

`PATCH /api/work-order/{workOrderId}/start`

성공 조건:

- 작업지시 존재
- 승인 완료
- 현재 상태가 `GATE_IN`

성공 처리:

- 상태를 `IN_PROGRESS`로 변경
- 연결 컨테이너 `canExit=false`

### 9.7 작업 완료

`PATCH /api/work-order/{workOrderId}/complete`

성공 조건:

- 작업지시 존재
- 승인 완료
- 현재 상태가 `IN_PROGRESS`
- 연결 컨테이너 존재

성공 처리:

- 컨테이너 `canExit=true`
- 상태를 `COMPLETED`로 변경

---

## 10. 게이트 입출차 흐름

Base URL: `/api/gate-log`

주요 API:

- `GET /api/gate-log`
- `POST /api/gate-log/process`

게이트 처리 요청은 다음 정보를 받는다.

- 트랙터 차량 ID
- 트레일러 차량 ID
- 작업지시 ID
- 컨테이너 ID
- 섹터 ID
- 게이트 번호
- 입출차 구분 `IN` 또는 `OUT`

입차 흐름:

```text
작업지시 검증
  -> 승인 여부 확인
  -> 차량/컨테이너/섹터 정합성 확인
  -> GateLog 생성
  -> 작업 상태 GATE_IN 변경
```

출차 흐름:

```text
작업 완료 여부 확인
  -> 컨테이너 반출 가능 여부 확인
  -> GateLog 생성
  -> 작업 상태 GATE_OUT 변경
```

실패 시:

- `GATE_FAIL` 결과 반환
- 예외 로그 저장 가능

---

## 11. 번호판 OCR 흐름

Base URL: `/api/plate-recognition`

주요 API:

- `POST /api/plate-recognition/recognize`
- `PATCH /api/plate-recognition/{plateRecognitionId}/manual-correction`

OCR 인식 요청:

```text
multipart/form-data
file
ocrType: paddle 또는 crnn
plateType: TRACTOR 또는 TRAILER
gateNumber
gateName
inOutType
```

처리 흐름:

```text
이미지 업로드
  -> FastAPI OCR 서버 호출
  -> 번호판 인식 결과 저장
  -> 차량 매칭
  -> 기사/운송사 상태 확인
  -> 작업지시 조회
  -> 컨테이너/야드 섹터 조회
  -> matched, needReview, message 반환
```

수동 보정:

```text
OCR 오인식
  -> 관리자가 manualCorrection 입력
  -> PlateRecognitionDTO 업데이트
```

---

## 12. 예외 로그와 대시보드

### 12.1 예외 로그

Base URL: `/api/exception-log`

역할:

- OCR 실패
- 차량 미등록
- 기사 출입 불가
- 운송사 비활성
- 작업지시 없음
- 게이트 처리 실패

같은 업무 예외를 운영자가 확인하고 처리 상태를 남길 수 있다.

### 12.2 대시보드

Base URL: `/api/dashboard`

`GET /api/dashboard/admin`

관리자 화면에서 필요한 요약 정보를 통합 조회한다.

예:

- 차량/사용자/작업/게이트/OCR/예외  요약
- 작업 상태별 현황
- 최근 작업 목록
- 야드 섹터 현황

---

## 13. 보안 구조

관련 파일:

- `config_p/SecurityConfig.java`
- `filter/JwtUtil.java`
- `filter/JwtFilter.java`

현재 기준:

- 로그인, 회원가입, 로그인 ID 중복 확인, 관리자 초기 생성은 인증 없이 접근 가능
- `/api/auth/users/**`는 `ADMIN` 권한 필요
- 나머지 `/api/**`는 현재 기능 통합을 위해 `permitAll`
- JWT 필터는 등록되어 있음
- CORS는 `localhost:5173`, `127.0.0.1:5173`, `200.200.200.66:5173` 허용

운영 수준으로 가려면:

- API별 권한 정책을 더 구체화해야 함
- `admin-init`는 운영 환경에서 제거하거나 제한해야 함
- 공통 예외 응답 구조가 있으면 프론트 연동이 쉬워짐

---

## 14. 혼자 백엔드 코드를 읽는 순서

새 기능을 이해할 때는 이 순서로 보면 된다.

### 1단계: Controller에서 URL 찾기

예:

```text
WorkOrderController
```

여기서 어떤 API가 있는지 본다.

```text
GET /api/work-order
POST /api/work-order
PATCH /api/work-order/{workOrderId}/approve
```

### 2단계: Service에서 업무 규칙 찾기

예:

```text
WorkOrderService.approve()
```

여기서 상태 검증, 예외, DB 변경 순서를 본다.

### 3단계: Mapper에서 SQL 확인하기

예:

```text
WorkOrderMapper.approve()
```

실제로 어떤 컬럼이 어떻게 바뀌는지 확인한다.

### 4단계: DTO에서 요청/응답 필드 확인하기

예:

```text
WorkOrderDTO
```

프론트에서 어떤 값을 보내야 하고, 어떤 값을 받을 수 있는지 확인한다.

### 5단계: 상태값을 따로 메모하기

작업지시, 사용자, 차량, 예외 상태값은 기능 이해의 핵심이다.

---

## 15. 새 API를 직접 만들 때 순서

예: 컨테이너 상세 조회 API를 추가한다고 가정한다.

### 1단계: URL 정하기

```text
GET /api/container/{containerId}
```

### 2단계: Controller 메서드 추가

```java
@GetMapping("/{containerId}")
public ContainerDTO detail(@PathVariable Long containerId) {
    return service.detail(containerId);
}
```

### 3단계: Service 메서드 확인 또는 추가

```java
public ContainerDTO detail(Long containerId) {
    return mapper.detail(containerId);
}
```

필요하면 없는 데이터일 때 `404` 예외를 던진다.

### 4단계: Mapper SQL 작성

```java
@Select("""
    SELECT ...
    FROM container
    WHERE container_id = #{containerId}
""")
ContainerDTO detail(Long containerId);
```

### 5단계: DTO 필드 확인

DB 컬럼과 DTO 필드명이 잘 매핑되는지 확인한다.

### 6단계: 테스트

Postman, 브라우저, 프론트 화면, 또는 테스트 코드로 확인한다.

---

## 16. 문서화할 때 보는 기준

API 정의서를 쓸 때는 Controller와 DTO만 보면 기본 문서는 작성할 수 있다.

하지만 좋은 정의서를 만들려면 Service까지 봐야 한다.

| 볼 파일 | 알 수 있는 것 |
|---|---|
| Controller | URL, Method, 요청 위치 |
| DTO | Request/Response 필드 |
| Service | 필수값, 상태 검증, 예외, 업무 규칙 |
| Mapper | 실제 DB 컬럼, 정렬 기준, 조인 관계 |
| SecurityConfig | 인증/권한 |

API 한 개를 문서화할 때 최소 항목:

- API명
- Method
- URL
- 설명
- 인증/권한
- Request Path/Query/Body
- Response Body
- 상태 변경
- 실패 조건

---

## 17. 현재 백엔드에서 특히 중요한 포인트

1. 작업지시가 시스템의 중심이다.
2. 기사, 차량, 컨테이너, 야드 섹터는 작업지시를 처리하기 위한 기준 데이터이다.
3. OCR은 번호판을 인식하는 것에서 끝나지 않고, 차량/기사/운송사/작업지시를 매칭한다.
4. 게이트 처리는 작업 상태를 `GATE_IN`, `GATE_OUT`으로 바꾸는 현장 처리 기능이다.
5. 작업 시작/완료는 컨테이너 반출 가능 여부와 연결된다.
6. 예외 로그는 실패한 업무 흐름을 운영자가 확인하기 위한 장치이다.
7. 현재 보안은 일부만 제한되어 있고, 대부분 API는 통합 작업을 위해 열려 있다.

---

## 18. 머릿속에 넣는 가장 쉬운 그림

```text
회원가입/로그인
  -> 운송사/기사/차량 등록
  -> 관리자 승인
  -> 작업지시 생성
  -> 작업지시 승인
  -> OCR 번호판 인식
  -> 차량/기사/작업 매칭
  -> 게이트 입차
  -> 야드 작업 시작
  -> 작업 완료
  -> 게이트 출차
  -> 대시보드/예외 로그 확인
```

이 흐름만 잡히면 나머지 API들은 대부분 이 흐름을 보조하는 기능으로 이해하면 된다.
