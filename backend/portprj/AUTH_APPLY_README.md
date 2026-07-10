# 인증 기능 적용 내용

이 압축본은 기존 `aaa` 패키지 구조를 유지하면서 로그인/회원가입 기능을 고도화한 버전입니다.

## 반영 내용

- 회원가입 시 BCrypt 비밀번호 해시 저장
- 로그인 시 `PasswordEncoder.matches()`로 검증
- JWT Access Token 발급
- `Authorization: Bearer <token>` 필터 추가
- 기사/운송사 계정은 기본 `PENDING`
- 관리자 계정은 기본 `ACTIVE`
- 관리자 회원목록/승인 API 추가
- 기존 API 영향 방지를 위해 `/api/**`는 일단 permitAll 유지

## 먼저 실행할 SQL

```sql
DB/auth_bcrypt_jwt_patch.sql
```

## 실행 전 확인

`build.gradle`에 JWT 의존성이 추가되어 있으므로 Gradle refresh가 필요합니다.

## API

- `POST /api/login` 또는 `POST /api/auth/login`
- `POST /api/register` 또는 `POST /api/auth/register`
- `GET /api/auth/users` ADMIN 권한 필요
- `PATCH /api/auth/users/{userId}/status` ADMIN 권한 필요

## 로그인 응답 예시

```json
{
  "userId": 1,
  "loginId": "admin",
  "userName": "관리자",
  "roleCode": "ADMIN",
  "status": "ACTIVE",
  "accessToken": "eyJ...",
  "tokenType": "Bearer"
}
```

## 주의

기존 평문 비밀번호 계정은 BCrypt 검증에 실패할 수 있습니다. 테스트 계정은 새로 회원가입해서 사용하세요.
