# 카카오 다중 계정 연결 설정

이 프로젝트는 액세스 토큰을 화면에 직접 입력하지 않습니다. 각 사용자가 카카오 로그인으로 한 번 연결하면 계정별 액세스 토큰과 리프레시 토큰을 PostgreSQL에 AES-GCM 암호화하여 저장하고, 백엔드가 만료 전에 자동 갱신합니다.

현재 발송 방식은 연결한 각 사용자의 **나와의 채팅방**입니다. 임의의 개인이나 특정 단체 채팅방으로 보내는 방식은 아닙니다.

## 가장 단순한 팀 사용 방식

발표 또는 공동 테스트에서는 백엔드와 DB를 한 컴퓨터에서만 실행하는 방식을 권장합니다.

1. 백엔드 실행 컴퓨터에만 아래 환경변수 설정을 합니다.
2. 각 팀원은 같은 예지보전 페이지에서 **다른 카카오 계정 추가**를 누릅니다.
3. 각자 자신의 카카오계정으로 로그인하고 `talk_message` 전송에 동의합니다.
4. 연결된 모든 계정의 나와의 채팅방으로 알림이 전송됩니다.

여러 컴퓨터에서 같은 DB를 바라보며 백엔드를 동시에 실행할 필요는 없습니다.

## 1. 카카오디벨로퍼스 확인

1. 해당 앱에서 **카카오 로그인**을 활성화합니다.
2. **카카오 로그인 > 동의항목**에서 `카카오톡 메시지 전송(talk_message)`을 설정합니다.
3. **카카오 로그인 > Redirect URI**에 아래 주소를 정확히 등록합니다.

   `http://localhost/api/predictive-maintenance/demo/notifications/kakao/oauth/callback`

4. 앱의 **REST API 키**와 REST API 키에 연결된 **Client secret**을 확인합니다.
5. 테스트 앱이라면 **앱 > 멤버**에 로그인할 팀원의 카카오계정을 추가하고, 팀원이 초대를 수락했는지 확인합니다. `KOE005`는 앱 멤버가 아니라는 뜻입니다.

Redirect URI는 프로토콜, 호스트, 포트, 경로와 마지막 `/`까지 등록값과 완전히 같아야 합니다. 다른 PC의 브라우저에서 서버에 접속한다면 `localhost`는 그 PC 자신을 뜻하므로, 실제 백엔드 주소를 Redirect URI와 환경변수 양쪽에 동일하게 사용해야 합니다.

## 2. 팀원 PC에서 최초 1회 설정

저장소의 `backend\portprj` 폴더에서 PowerShell을 열고 실행합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\setup-kakao-oauth.ps1
```

REST API 키와 Client secret을 입력합니다. 새 로컬 DB를 쓰는 컴퓨터는 암호화 키가 자동 생성됩니다. 설정값은 Git이 아닌 현재 Windows 사용자의 환경변수에 저장됩니다.

완료 후에는 **IntelliJ 창을 전부 종료하고 다시 실행**해야 합니다. 이후 별도 CMD 실행 없이 IntelliJ에서 `PortprjApplication`을 평소처럼 실행하면 됩니다.

## 3. DB 준비

`kakao_oauth_connection` 테이블은 Spring 백엔드 시작 시 자동 생성됩니다. 따라서 PostgreSQL의 `port_db` 연결이 정상이고 `port_user`에게 테이블 생성 권한이 있으면 별도 작업이 필요 없습니다.

서버 로그에 테이블 생성 권한 오류가 나오면 저장소 루트에서 DB 관리자 권한으로 다음 스크립트를 한 번 실행합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\DB\setup_kakao_oauth.ps1
```

DB 주소나 계정이 다르면 다음처럼 지정합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\DB\setup_kakao_oauth.ps1 `
  -DatabaseHost localhost -DatabasePort 5432 -DatabaseName port_db -DatabaseUser port_user
```

## 4. 다른 컴퓨터와 DB를 공유하는 경우

DB의 토큰은 `KAKAO_TOKEN_ENCRYPTION_KEY`로 암호화됩니다. 이미 연결정보가 저장된 DB를 다른 컴퓨터에서 그대로 사용하려면 **기존 백엔드와 정확히 같은 암호화 키**가 필요합니다.

기존 백엔드 컴퓨터의 Windows 사용자 환경변수 `KAKAO_TOKEN_ENCRYPTION_KEY`를 안전한 방법으로 전달한 뒤, 새 컴퓨터에서 다음 명령을 실행하고 프롬프트에 기존 키를 입력합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\setup-kakao-oauth.ps1 -UseExistingTokenDatabase
```

다른 암호화 키로 기존 DB를 열면 `저장된 카카오 토큰을 복호화할 수 없습니다`가 나타납니다. 이 경우 두 가지 중 하나를 선택합니다.

- 기존 암호화 키를 설정하고 IntelliJ를 다시 시작합니다.
- `kakao_oauth_connection`의 기존 연결정보를 삭제하고 각 계정을 다시 연결합니다.

암호화 키, REST API 키, Client secret과 토큰은 Git, 메신저 공개방 또는 화면 캡처에 올리지 않습니다.

## 5. 계정 연결

1. 백엔드와 프런트엔드를 실행합니다.
2. 관리자 예지보전 화면에서 **다른 카카오 계정 추가**를 누릅니다.
3. 연결할 사용자의 카카오계정으로 로그인합니다.
4. 화면에 해당 계정 번호가 추가되는지 확인합니다.

## 오류별 확인

- `카카오 OAuth 서버 설정이 필요합니다`: IntelliJ를 완전히 재시작했는지, Windows 사용자 환경변수가 등록됐는지 확인합니다.
- `KOE005`: 테스트 앱의 멤버로 등록되지 않은 계정입니다.
- `redirect_uri mismatch` 또는 로그인 후 실패: 카카오디벨로퍼스 등록 URI와 `KAKAO_REDIRECT_URI`가 정확히 같은지 확인합니다.
- `저장된 카카오 토큰을 복호화할 수 없습니다`: DB를 만든 컴퓨터와 암호화 키가 다릅니다.
- `relation kakao_oauth_connection does not exist` 또는 권한 오류: `DB\setup_kakao_oauth.ps1`을 실행합니다.
- 연결은 됐지만 메시지가 오지 않음: `talk_message` 동의 여부와 백엔드 로그의 카카오 API 응답을 확인합니다.

## Client secret 재발급 후 갱신

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\update-kakao-client-secret.ps1
```

갱신 후 IntelliJ를 완전히 종료하고 다시 실행합니다.

## 연결 유지 방식

- 액세스 토큰 만료 전에 백엔드가 리프레시 토큰으로 새 액세스 토큰을 발급합니다.
- 카카오가 새 리프레시 토큰을 반환하면 DB의 기존 토큰을 교체합니다.
- 리프레시 토큰 자체가 만료되거나 사용자가 연결을 해제하면 해당 계정만 다시 로그인해야 합니다.
- 암호화 키를 변경하면 기존 토큰을 복호화할 수 없습니다.
