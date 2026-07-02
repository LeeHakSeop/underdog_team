# 프로젝트 구조

이 저장소는 항만 물류센터 출입 관리 팀 프로젝트입니다. 
Spring Boot 백엔드, Vue/Vite 프론트엔드, 데이터베이스 SQL 스크립트로 이루어져있습니다.

## 주요 폴더

- `backend/portprj/` - Spring Boot 백엔드 프로젝트입니다.
- `frontend/` - Vue와 Vite 기반 프론트엔드 프로젝트입니다.
- `DB/` - 데이터베이스 테이블별 SQL 스크립트를 보관합니다.
- `tools/` - 프로젝트 보조 도구나 작업 스크립트를 보관하는 폴더입니다.
- `진행방향/` - 로컬 기획 자료와 진행 관련 문서를 보관하는 폴더입니다.

## DB 스크립트

현재 `DB/` 폴더에는 다음 SQL 파일이 있습니다.

- `carrier.sql` 운송사
- `container.sql` 컨테이너
- `driver.sql` 기사 
- `exception_log.sql` 예외 처리 로그
- `gate_log.sql` 게이트 출입로그
- `plate_recognition.sql` 인식 기록
- `vehicle.sql` 차량
- `work_order.sql` 작업 지시
- `work_status_history.sql` 작업 상태 진행 이력
- `yard_sector.sql` 작업 구역


## 프론트엔드 데이터 기준

프론트엔드 테이블 화면은 DB 다이어그램의 테이블 구조를 기준으로 맞춥니다.

- `carrier`
- `driver`
- `vehicle`
- `work_order`
- `container`
- `yard_sector`
- `gate_log`
- `plate_recognition`
- `work_status_history`
- `exception_log`


## 로컬 전용 폴더와 생성 파일

다음 항목은 의존성, 빌드 결과물, IDE 설정, 캐시 등 로컬에서 다시 생성할 수 있는 파일입니다.

- `.venv/`
- `.tools/`
- `.pytest_cache/`
- `.idea/`
- `backend/portprj/.gradle/`
- `backend/portprj/build/`
- `frontend/node_modules/`
- `frontend/dist/`

이 항목들은 Gitignore에 포함되어 있습니다.

## 실행 명령

백엔드 실행:

```powershell
cd backend/portprj
./gradlew bootRun
```

프론트엔드 개발 서버 실행:

```powershell
cd frontend
npm run dev
```

프론트엔드 빌드:

```powershell
cd frontend
npm run build
```

