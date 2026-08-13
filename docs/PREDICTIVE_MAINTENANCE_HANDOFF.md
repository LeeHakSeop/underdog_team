# 예지보전 이관 및 실행 순서

## 최종 구성

예지보전 기능은 다음 세 테이블만 사용한다.

- `pm_equipment`: 안테나 기본정보
- `pm_sensor_data`: 센서 시계열과 같은 시각의 모델 판정값
- `pm_event`: 고장 예상, 실제 고장, 수리 완료 및 카카오 발송 결과

화면 데이터 흐름은 다음과 같다.

```text
Vue 화면 -> Spring REST API -> PostgreSQL
```

CSV는 화면이 직접 읽지 않는다. 초기 데이터 적재 또는 갱신할 때만 백엔드의 CSV 적재 API로 사용한다.

## 새 환경에서 실행하는 순서

1. PostgreSQL의 `port_db`에서 `DB/predictive_maintenance.sql`을 실행한다.
2. Spring 백엔드를 실행한다.
3. 프로젝트 루트의 PowerShell에서 다음 명령으로 데이터를 적재한다.

```powershell
curl.exe -X POST `
  -F "file=@DB\data\01_dashboard_timeseries.csv;type=text/csv" `
  http://localhost/api/predictive-maintenance/sensor-data/import
```

4. 다음 주소에서 적재 결과를 확인한다.

```text
http://localhost/api/predictive-maintenance/sensor-data/summary
```

정상 기준은 장비 24개, 센서 데이터 69,120행, 사건 39건이다.

5. Vue 프론트엔드를 실행하고 관리자 메뉴의 `예지보전`에 접속한다.

## 카카오 안전 기본값

실제 발송은 기본적으로 꺼져 있다.

```text
KAKAO_MESSAGE_ENABLED=false
KAKAO_MESSAGE_DRY_RUN=true
```

CSV에서 생성한 과거 사건은 `DEMO_NOT_SENT`로 저장하므로 카카오톡을 보내지 않는다.
실제 테스트 절차는 `docs/KAKAO_MESSAGE_SETUP.md`를 따른다.

## 최종 프로젝트에서 제외한 자료

- `antenna-maintenance-demo.json`: DB API 전환 전 사용하던 대용량 화면용 JSON
- `build-predictive-demo-data.mjs`: 위 JSON을 만들던 개발용 스크립트
- `PREDICTIVE_MAINTENANCE_PLAN.md`: 7개 테이블을 전제로 한 과거 설계 초안
- `pm_visualization_bundle_v3.zip`: 분석 결과 전달 원본
- `predictive_maintenance_timeseries_synthetic_v3.csv`: 모델 판정 열이 없는 원본 CSV
- `예지보전관련내용/`: 모델 분석·노트북·중간 산출물 전체

이 자료들은 분석 과정의 참고자료이며 최종 웹 프로젝트 실행에는 필요하지 않다.
