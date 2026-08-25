# 카카오 다중 계정 연결 설정

이 프로젝트는 액세스 토큰을 화면에 직접 입력하지 않습니다. 각 팀원이 카카오 로그인으로 한 번씩 연결하면 계정별 액세스 토큰과 리프레시 토큰을 PostgreSQL에 AES-GCM 암호화하여 저장하고, 백엔드가 6시간마다 만료 여부를 확인해 자동 갱신합니다.

## 1. 카카오디벨로퍼스 설정

1. 카카오디벨로퍼스의 해당 앱에서 **카카오 로그인**을 활성화합니다.
2. **카카오 로그인 > 동의항목**에서 `카카오톡 메시지 전송(talk_message)`을 설정합니다.
3. **카카오 로그인 > Redirect URI**에 아래 주소를 정확히 등록합니다.

   `http://localhost/api/predictive-maintenance/demo/notifications/kakao/oauth/callback`

4. 앱의 **REST API 키**와 REST API 키에 연결된 **Client secret**을 확인합니다.

운영 주소나 백엔드 포트가 다르면 Redirect URI도 실제 외부 접근 주소로 변경해야 합니다. 카카오디벨로퍼스 등록값과 `KAKAO_REDIRECT_URI`는 완전히 같아야 합니다.

## 2. 서버 환경변수 최초 1회 등록

PowerShell에서 다음 스크립트를 실행합니다.

```powershell
cd C:\wjs\UndTeam2\backend\portprj
.\setup-kakao-oauth.ps1
```

REST API 키와 Client secret을 입력하면 암호화 키는 자동 생성됩니다. 값은 Git 저장소가 아닌 현재 Windows 사용자 환경변수에 저장됩니다. 완료 후 새 터미널을 열어야 합니다.

## 3. 실행 및 연결

```powershell
cd C:\wjs\UndTeam2\backend\portprj
.\run-with-kakao.cmd
```

프런트엔드를 실행하고 관리자 예지보전 화면에서 **카카오 계정 연결**을 누릅니다. 카카오 로그인과 메시지 전송 동의를 마치면 원래 화면으로 돌아옵니다. 다른 팀원은 같은 컴퓨터에서 **다른 카카오 계정 추가**를 누르고 자신의 계정으로 로그인하면 됩니다.

연결된 모든 계정은 목록에 표시되고 개별 해제할 수 있습니다. 고장 예상·실제 고장 알림은 목록에 있는 각 계정의 `나와의 채팅방`으로 한 번씩 전송됩니다.

## Client secret 재발급 후 갱신

카카오디벨로퍼스에서 Client secret을 재발급했다면 페이지 하단의 **저장**을 누른 뒤 다음 스크립트로 서버 설정만 갱신합니다.

```powershell
cd C:\wjs\UndTeam2\backend\portprj
.\update-kakao-client-secret.ps1
```

갱신 후 IntelliJ를 완전히 종료하고 다시 실행해야 새 환경변수가 적용됩니다.

## 연결 유지 방식

- 액세스 토큰 만료 전에 백엔드가 리프레시 토큰으로 새 액세스 토큰을 발급합니다.
- 카카오가 새 리프레시 토큰을 반환하면 DB의 기존 토큰을 즉시 교체합니다.
- 백엔드가 장기간 실행되지 않아 리프레시 토큰 자체가 만료되거나 사용자가 연결을 해제하면 해당 계정만 다시 로그인해야 합니다.
- `KAKAO_TOKEN_ENCRYPTION_KEY`를 변경하면 기존 토큰을 복호화할 수 없습니다. 이 경우 `kakao_oauth_connection`의 연결정보를 삭제하고 다시 연결합니다.

## 보안 주의사항

- REST API 키, Client secret, 암호화 키 또는 토큰을 프런트엔드 코드와 Git에 기록하지 않습니다.
- 운영 서버에서는 사용자 환경변수 대신 조직의 Secret Manager 또는 OS 보안 저장소 사용을 권장합니다.
- 현재 구현은 연결된 각 토큰 소유자의 `나와의 채팅방`으로 전송합니다. 연결하지 않은 사람에게 보내는 알림톡 또는 친구 메시지는 별도 발송 방식과 권한이 필요합니다.
