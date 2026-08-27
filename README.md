# Port Gate 통합 운영 시스템

작성 기준일: 2026-08-27

항만 게이트의 차량 진입, 작업지시, 야드 운영, 컨테이너 반출입, AI 번호판 인식, 예지보전 관제를 통합한 웹 프로젝트입니다. Vue 프론트엔드, Spring Boot 백엔드, PostgreSQL 데이터베이스, FastAPI 기반 번호판 인식 서버로 구성됩니다.

## 주요 기능

- 역할별 화면: 관리자, 운송사, 기사 권한에 따라 전용 화면 제공
- 인증/회원 관리: 로그인, 회원가입, JWT 발급, 사용자 승인 상태 관리
- 운송사/기사/차량 관리: 운송사 등록, 기사 승인, 트랙터/트레일러 차량 관리
- 작업지시 관리: 배차 승인 대기부터 게이트 입차, 야드 작업, 작업 완료, 게이트 출차까지 상태 추적
- 게이트 처리: 입출차 처리, 게이트 로그 저장, 실패 흐름 예외 로그 연계
- 컨테이너/야드 관리: 컨테이너 위치, 야드 섹터, 섹터 수용량, 운영 맵, 혼잡도 조회
- AI 번호판 인식: 이미지 업로드 후 FastAPI OCR 서버에서 번호판 검출 및 인식
- 예지보전: 안테나 센서 시계열, 고장 예상/고장/정비 이벤트, 카카오 알림 데모 연계
- 날씨 위험도: OpenWeather API 기반 현재 날씨와 운영 위험도 계산

## 기술 스택

| 영역 | 기술 |
|---|---|
| Frontend | Vue 3, Vite, Pinia, Vue Router, Leaflet |
| Backend | Java 17, Spring Boot 3.5, Spring Security, JWT, MyBatis |
| Database | PostgreSQL |
| AI API | Python, FastAPI, Uvicorn, YOLO/CRNN/PaddleOCR 기반 번호판 인식 파이프라인 |
| Build/Tooling | Gradle, npm |

## 프로젝트 구조

```text
.
├─ frontend/                 # Vue 3 + Vite 프론트엔드
│  ├─ src/views/admin/        # 관리자 화면
│  ├─ src/views/carrier/      # 운송사 화면
│  ├─ src/views/driver/       # 기사 화면
│  ├─ src/api/                # REST API 클라이언트
│  ├─ src/stores/             # Pinia 상태 관리
│  └─ src/router/             # 권한별 라우팅
├─ backend/portprj/           # Spring Boot 백엔드
│  ├─ src/main/java/aaa/      # 기능별 controller/service/model 패키지
│  └─ src/main/resources/     # application.yaml 등 설정
├─ AI/                        # FastAPI 번호판 인식 서버
│  ├─ app/                    # FastAPI 엔트리포인트와 라우터
│  ├─ scripts/                # 번호판 검출/OCR/후처리 스크립트
│  └─ models/                 # 학습된 모델 파일
├─ DB/                        # DB 생성/패치/예지보전 적재 SQL 및 스크립트
├─ docs/                      # 기능별 설계 및 이관 문서
├─ scripts/                   # AI 스크립트 사본 및 유틸리티
└─ models/                    # AI 모델 사본
```

## 실행 전 준비

- Java 17
- Node.js 22.18 이상 또는 24.12 이상
- PostgreSQL
- Python 환경과 AI 의존 패키지
- 선택 사항: OpenWeather API 키, 카카오 메시지 테스트용 환경변수

DB 기본 설정은 `backend/portprj/src/main/resources/application.yaml` 기준입니다.

```yaml
spring.datasource.url: jdbc:postgresql://localhost:5432/port_db
spring.datasource.username: port_user
spring.datasource.password: 123456
server.port: 80
ai.plate.url: http://127.0.0.1:8000/api/plate-recognition
```

## 실행 순서

### 1. 데이터베이스 생성

PostgreSQL에 `port_db` 데이터베이스와 `port_user` 계정을 준비한 뒤, 프로젝트 루트에서 필요한 SQL을 적용합니다.

```powershell
psql -U port_user -d port_db -f DB\DB_Create.sql
```

예지보전 데모 데이터를 함께 적재하려면 백엔드를 먼저 실행한 뒤 아래 스크립트를 실행합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\DB\setup_predictive_maintenance.ps1
```

정상 적재 기준은 장비 24개, 센서 데이터 69,120행, 이벤트 39건입니다.

### 2. AI 번호판 인식 서버 실행

```powershell
cd AI
pip install -r dependencies.txt
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

상태 확인:

```text
http://127.0.0.1:8000/api/health
```

인식 API:

```text
POST http://127.0.0.1:8000/api/plate-recognition
Content-Type: multipart/form-data
file: 이미지 파일
```

자세한 내용은 `AI/README.md`를 참고합니다.

### 3. 백엔드 실행

```powershell
cd backend\portprj
.\gradlew.bat bootRun
```

기본 포트는 `80`입니다.

```text
http://localhost
```

날씨 API를 사용할 경우 환경변수 또는 루트의 `.weather-api-key` 파일에 OpenWeather API 키를 설정합니다.

```powershell
$env:WEATHER_API_KEY="your-openweather-api-key"
```

### 4. 프론트엔드 실행

```powershell
cd frontend
npm install
npm run dev
```

Vite 개발 서버 기본 주소:

```text
http://localhost:5173
```

## 화면 라우트

| 역할 | 주요 경로 | 설명 |
|---|---|---|
| 공통 | `/login` | 로그인 |
| 관리자 | `/admin/main` | 상황 관제판 |
| 관리자 | `/admin/dashboard` | 운영 현황 요약 |
| 관리자 | `/admin/yard-map` | 운영 맵 |
| 관리자 | `/admin/plate-recognition` | AI 번호판 인식 |
| 관리자 | `/admin/predictive-maintenance` | 예지보전 |
| 관리자 | `/admin/members` | 회원 관리 |
| 관리자 | `/admin/containers` | 컨테이너 관리 |
| 관리자 | `/admin/work-orders` | 작업 관리 |
| 관리자 | `/admin/events` | 알림/이벤트 |
| 운송사 | `/carrier/dashboard` | 운송사 업무 |
| 운송사 | `/carrier/driver-approval` | 기사 승인/회원 관리 |
| 운송사 | `/carrier/input` | 배정/작업 입력 및 수정 |
| 운송사 | `/carrier/inquiry` | 배정/작업 조회 |
| 기사 | `/driver/dashboard` | 기사 작업 |
| 기사 | `/driver/work-status` | 작업 현황 |
| 기사 | `/driver/vehicles` | 차량 등록 |

## 주요 백엔드 API

| Base URL | 기능 |
|---|---|
| `/api/auth` | 로그인, 회원가입, 로그인 ID 중복 확인, 사용자 승인 상태 관리 |
| `/api/carrier` | 운송사 목록/상세/등록/수정/삭제 |
| `/api/driver` | 기사 목록/상세/승인/탈퇴/재활성화/기사별 작업 조회 |
| `/api/vehicle` | 차량 목록/상세/등록/수정/삭제/승인/차량별 정보 조회 |
| `/api/work-order` | 작업지시 조회/등록/수정/취소/승인/반려/시작/완료 |
| `/api/gate-log` | 게이트 로그 조회, 입출차 처리 |
| `/api/plate-recognition` | 번호판 이미지 인식, 수동 보정 |
| `/api/container` | 컨테이너 조회/등록/수정/삭제 |
| `/api/yard-sector` | 야드 섹터 조회, 수용량 수정 |
| `/api/yard-map` | 야드 운영 맵 스냅샷 |
| `/api/yard-congestion` | 야드 혼잡도 |
| `/api/exception-log` | 예외 로그 조회 및 처리 |
| `/api/dashboard` | 관리자 대시보드 통합 요약 |
| `/api/weather` | 현재 날씨 및 위험도 |
| `/api/predictive-maintenance` | 예지보전 장비, 센서 데이터, 이벤트 조회 |

## 핵심 업무 흐름

```text
회원가입/로그인
→ 운송사·기사·차량 등록
→ 관리자/운송사 승인
→ 작업지시 생성
→ 작업지시 승인
→ AI 번호판 인식
→ 차량·기사·작업지시 매칭
→ 게이트 입차
→ 야드 작업 시작
→ 작업 완료
→ 게이트 출차
→ 대시보드·예외 로그·예지보전 확인
```

## 예지보전 구성

예지보전은 다음 세 테이블을 중심으로 동작합니다.

| 테이블 | 역할 |
|---|---|
| `pm_equipment` | 안테나 기본 정보 |
| `pm_sensor_data` | 센서 시계열과 모델 판정값 |
| `pm_event` | 고장 예상, 실제 고장, 정비 완료, 회복 이벤트 |

CSV는 화면에서 직접 읽지 않고 백엔드 적재 API를 통해 PostgreSQL에 저장합니다.

```powershell
curl.exe -X POST `
  -F "file=@DB\data\01_dashboard_timeseries.csv;type=text/csv" `
  http://localhost/api/predictive-maintenance/sensor-data/import
```

적재 결과 확인:

```text
GET http://localhost/api/predictive-maintenance/sensor-data/summary
```

카카오 메시지 실제 발송은 기본적으로 비활성화되어 있습니다.

```text
KAKAO_MESSAGE_ENABLED=false
KAKAO_MESSAGE_DRY_RUN=true
```

## 참고 문서

- `backend/portprj/BACKEND_구조_정리.md`: 백엔드 패키지와 업무 흐름 설명
- `backend/portprj/API정의서_초안.md`: 백엔드 API 정의 초안
- `backend/portprj/AUTH_APPLY_README.md`: 인증/JWT 적용 내용
- `AI/README.md`: AI 번호판 인식 서버 실행과 응답 형식
- `docs/PREDICTIVE_MAINTENANCE_HANDOFF.md`: 예지보전 실행 및 이관 순서
- `docs/PREDICTIVE_MAINTENANCE_API.md`: 예지보전 DB/API 구성
- `docs/KAKAO_OAUTH_SETUP.md`: 카카오 OAuth 설정
- `docs/KAKAO_MESSAGE_SETUP.md`: 카카오 메시지 설정

## 개발 메모

- 루트의 `README.md`는 전체 프로젝트 안내 문서입니다.
- AI 서버 상세 문서는 `AI/README.md`에 유지합니다.
- `application.yaml`에는 로컬 개발 기본값이 포함되어 있으므로 운영 환경에서는 DB 비밀번호, JWT secret, API key, 카카오 secret을 환경변수 또는 별도 설정으로 분리해야 합니다.
- 현재 백엔드는 일부 인증 API를 제외한 `/api/**` 접근을 통합 개발 편의를 위해 넓게 허용하고 있습니다. 운영 배포 전에는 역할별 권한 정책을 강화해야 합니다.
