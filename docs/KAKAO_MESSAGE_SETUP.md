# 카카오톡 시연 알림 설정

현재 구현은 시연용 안테나가 `고장 예상` 또는 `실제 고장` 시점을 통과할 때,
카카오톡의 **나에게 보내기** API를 호출한다.

## 1. 카카오 개발자 설정

1. 카카오 디벨로퍼스에서 애플리케이션을 생성한다.
2. 카카오 로그인을 활성화하고 Redirect URI를 등록한다.
3. 동의 항목에서 `카카오톡 메시지 전송(talk_message)`을 사용하도록 설정한다.
4. 본인 카카오 계정으로 로그인해 액세스 토큰을 발급받는다.

액세스 토큰은 비밀번호처럼 취급하며 Git에 올리지 않는다.

## 2. 백엔드 실행 설정

PowerShell에서 백엔드를 실행하기 전에 다음 환경 변수를 지정한다.

```powershell
$env:KAKAO_MESSAGE_ENABLED="true"
$env:KAKAO_MESSAGE_DRY_RUN="true"
$env:KAKAO_ACCESS_TOKEN="발급받은_액세스_토큰"
$env:KAKAO_ALLOWED_USER_ID="확인한_본인_카카오_사용자_ID"
$env:KAKAO_DASHBOARD_URL="http://localhost:5173"
```

기본값은 `KAKAO_MESSAGE_ENABLED=false`, `KAKAO_MESSAGE_DRY_RUN=true`이므로 실제 메시지를 발송하지 않는다.
실제 발송 전에 카카오 토큰의 사용자 ID와 `KAKAO_ALLOWED_USER_ID`를 비교하며, 다르면 `BLOCKED` 처리한다.

안전한 시험 순서는 다음과 같다.

1. `KAKAO_MESSAGE_DRY_RUN=true` 상태에서 시연 요청이 `DRY_RUN`으로 처리되는지 확인한다.
2. 카카오의 액세스 토큰 정보 조회 API로 본인 사용자 ID를 확인한다. 이 조회는 메시지를 보내지 않는다.
3. 사용자 ID가 본인 계정인지 확인한 뒤 `KAKAO_ALLOWED_USER_ID`에 설정한다.
4. 실제 한 건 발송 직전에만 `KAKAO_MESSAGE_DRY_RUN=false`로 변경한다.
5. 수신을 확인한 즉시 다시 `KAKAO_MESSAGE_DRY_RUN=true` 또는 `KAKAO_MESSAGE_ENABLED=false`로 되돌린다.

## 3. 시연 흐름

1. 예지보전 화면에서 `시연용 안테나`를 선택한다.
2. 재생을 시작한다.
3. 고장 예상 시점에 첫 번째 카카오톡을 발송한다.
4. 실제 고장 시점에 두 번째 카카오톡을 발송한다.

## 운영 전환 시 주의사항

- 나에게 보내기는 로그인한 본인의 카카오톡 `나와의 채팅`으로만 발송한다.
- REST API 액세스 토큰은 만료되므로 장기 운영 시 Refresh Token 갱신 처리가 필요하다.
- 여러 관리자에게 자동 발송하려면 카카오톡 친구 메시지 권한 또는 비즈메시지 알림톡을 사용해야 한다.
- 운영 환경에서는 알림 발송 결과와 중복 방지 키를 DB에 저장해야 한다.
