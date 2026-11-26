# Week 6 키워드 과제

SOPT 39기 6차 서버 세미나 키워드 학습 정리.

> 전체 레이어 구조는 [`architecture.md`](./architecture.md)를 참고한다. 이 문서는 세미나 키워드와 현재 프로젝트의 인증 구현을 함께 정리한다.

---

## 1. 세션 기반 인증에서 사용자 인증이 일어나는 과정

세션 기반 인증은 **인증 상태를 서버가 보관**하는 방식이다. 브라우저는 상태 자체가 아니라, 서버 저장소의 상태를 찾기 위한 무작위 세션 ID만 쿠키에 담는다.

### 로그인부터 이후 요청 검증까지

```text
Browser                    Application Server                 Session Store
  │ POST /login (id, pw)          │                                  │
  │──────────────────────────────>│ 사용자·비밀번호 검증              │
  │                               │─────────────────────────────────>│
  │                               │  sessionId → {memberId, role, exp}│
  │                               │<─────────────────────────────────│
  │ Set-Cookie: SESSION=sessionId │                                  │
  │<──────────────────────────────│                                  │
  │
  │ GET /posts (Cookie: SESSION)  │                                  │
  │──────────────────────────────>│ sessionId로 인증 상태 조회         │
  │                               │─────────────────────────────────>│
  │                               │<─────────────────────────────────│
  │                               │ SecurityContext에 사용자 설정      │
  │<──────────────────────────────│ 인가된 응답                        │
```

1. 클라이언트가 아이디·비밀번호로 로그인 요청을 보낸다. 전송 구간은 HTTPS여야 한다.
2. 서버는 비밀번호 해시를 비교하고 사용자의 활성 상태를 확인한다.
3. 성공하면 예측 불가능한 세션 ID를 만들고, 서버 측 저장소(DB·Redis·메모리 등)에 `memberId`, 권한, 만료 시각 같은 인증 상태를 저장한다.
4. 서버는 세션 ID를 `Set-Cookie`로 응답한다. 쿠키에는 적어도 `HttpOnly`, `Secure`, `SameSite`, 만료 범위를 설정한다.
5. 브라우저는 같은 사이트 요청에 세션 쿠키를 자동으로 보낸다.
6. 서버는 매 요청마다 세션 ID를 저장소에서 조회하고, 유효하면 인증 객체를 만들고 URL·권한 정책을 검사한다.
7. 로그아웃, 강제 로그아웃, 권한 변경 때 서버 저장소의 세션을 지우거나 갱신하면 다음 요청부터 즉시 반영된다.

### 장단점과 보안 포인트

| 항목 | 내용 |
|---|---|
| 장점 | 서버가 상태를 갖기 때문에 강제 로그아웃·권한 변경·세션 폐기를 즉시 처리하기 쉽다. |
| 단점 | 요청마다 저장소 조회가 필요하고, 서버가 늘어나면 세션 공유 저장소 또는 sticky session이 필요하다. |
| 세션 고정 공격 방어 | 로그인 성공 시 기존 세션 ID를 폐기하고 새 ID를 발급한다. |
| 탈취 방어 | HTTPS, `HttpOnly`, `Secure`, 짧은 만료, 서버 측 세션 폐기가 필요하다. |
| CSRF 방어 | 쿠키는 자동 전송되므로 상태 변경 요청에 CSRF 토큰 검증 또는 엄격한 `SameSite` 정책이 필요하다. |

이 프로젝트는 `SessionCreationPolicy.STATELESS`를 사용하므로 서버 세션을 만들지 않는다. 대신 다음 절의 Access/Refresh Token 방식으로 인증 상태를 구성한다.

---

## 2. 토큰 기반 인증에서 사용자 인증이 일어나는 과정

토큰 기반 인증에서는 서버가 매 요청마다 세션 ID로 인증 상태를 찾는 대신, 서명된 토큰의 claim을 검증해 사용자를 식별한다. 이 프로젝트는 짧게 사는 Access Token과 길게 사는 Refresh Token을 분리한다.

### 로그인과 토큰 발급

```text
Client                         API Server                         DB
  │ POST /auth/login (email, password) │                          │
  │───────────────────────────────────>│ 사용자·비밀번호 검증     │
  │                                    │─────────────────────────>│
  │                                    │<─────────────────────────│
  │                                    │ Access/Refresh JWT 생성   │
  │                                    │ Refresh 원문 해시 저장    │
  │                                    │─────────────────────────>│
  │ Authorization: Bearer <access>     │                          │
  │ Set-Cookie: refreshToken=...       │                          │
  │ Set-Cookie: XSRF-TOKEN=...         │                          │
  │<───────────────────────────────────│                          │
```

1. `POST /auth/login`이 이메일·비밀번호 형식을 검증하고 BCrypt 해시로 비밀번호를 비교한다.
2. `JwtProvider`는 Access Token과 Refresh Token을 별도로 발급한다.
3. 공통 claim은 `sub`(회원 ID), `jti`(토큰 식별자), `iss`, `iat`, `exp`, `tokenType`이다. Access Token에만 `role` claim을 넣는다.
4. Refresh Token 원문은 DB에 저장하지 않는다. `TokenService`가 SHA-256 해시, `jti`, family, 상태, 만료 시각을 `RefreshToken`으로 보관한다.
5. Access Token은 `Authorization` 응답 헤더와 호환용 JSON body로 돌려주며, 클라이언트는 JavaScript 메모리에만 둔다.
6. Refresh Token은 `HttpOnly; Secure; SameSite=Strict` 쿠키로만 보관한다. JavaScript가 읽지 못하므로 XSS로 원문이 탈취되는 범위를 줄인다.

### 보호 요청의 검증

```text
Client  ── Authorization: Bearer <Access JWT> ──> JwtAuthenticationFilter
                                                      │
                                                      ├─ 서명·issuer·만료 검증
                                                      ├─ tokenType=ACCESS, role claim 검증
                                                      ├─ access jti blacklist 확인
                                                      ├─ 활성 회원 여부 확인
                                                      └─ SecurityContext에 memberId·ROLE_* 설정
                                                                  │
                                                           SecurityFilterChain 인가
```

`JwtAuthenticationFilter`는 토큰을 검증한 뒤에만 SecurityContext를 설정한다. 이후 `SecurityConfig`가 URL 권한을 판단한다. 예를 들어 `GET /members`는 `ROLE_ADMIN`만 통과하고, 게시글·좋아요 같은 상태 변경 API는 인증이 필요하다.

### 만료, 재발급, 재사용 탐지

```text
Client                                    API Server                         DB
  │ POST /auth/reissue                    │                                  │
  │ Cookie: refreshToken                  │                                  │
  │ X-XSRF-TOKEN: <cookie value>          │                                  │
  │──────────────────────────────────────>│ CSRF cookie/header 상수 시간 비교 │
  │                                       │ Refresh JWT·해시·상태 검증         │
  │                                       │ token family 활성 행 잠금           │
  │                                       │─────────────────────────────────>│
  │                                       │ 기존 token ROTATED + 새 token 저장 │
  │                                       │<─────────────────────────────────│
  │ 새 Access 헤더 + 새 Refresh Cookie    │                                  │
  │<──────────────────────────────────────│                                  │
```

1. Access Token이 만료되면 클라이언트는 `POST /auth/reissue`를 호출한다.
2. 브라우저는 Refresh Cookie를 자동으로 보내고, 클라이언트는 읽을 수 있는 `XSRF-TOKEN` 쿠키 값을 `X-XSRF-TOKEN` 헤더로 보낸다.
3. 서버는 double-submit CSRF 검증 뒤 Refresh JWT의 서명·만료·타입과 DB의 토큰 해시·상태·회원 일치를 확인한다.
4. 유효하면 기존 Refresh Token을 `ROTATED`로 바꾸고 같은 family의 새 Refresh Token과 새 Access Token을 발급한다.
5. 이미 회전·폐기된 토큰을 다시 쓰거나 해시가 다르면 탈취 가능성으로 간주하고, 해당 family의 활성 토큰을 모두 `REVOKED`로 바꾼다.
6. family의 활성 행을 비관적 잠금으로 잡으므로, 정상 재발급과 과거 토큰 재사용이 동시에 일어나도 후속 활성 토큰이 남지 않는다.

### 로그아웃과 탈퇴

- 로그아웃 시 Access Token의 `jti`를 만료 시각까지 blacklist에 저장하고 현재 Refresh family를 폐기한다.
- 탈퇴 시 회원을 soft delete한 뒤 Access Token blacklist 처리와 모든 활성 Refresh family 폐기를 수행한다.
- 만료된 Refresh Token과 blacklist 행은 scheduler가 정리한다.

### 세션과 토큰 방식의 차이

| 구분 | 세션 기반 | 이 프로젝트의 JWT 기반 |
|---|---|---|
| 인증 상태 | 서버 저장소의 세션 데이터 | Access JWT claim + Refresh Token DB 상태 |
| 요청 검증 | 세션 ID로 저장소 조회 | JWT 서명·claim 검증, 폐기 여부 조회 |
| 즉시 무효화 | 세션 삭제 | Access blacklist + Refresh family 폐기 |
| 브라우저 자동 전송 | 세션 쿠키 | Refresh Cookie만 자동 전송, Access는 헤더에 명시 |
| CSRF 대응 | 상태 변경 전반에 필요 | Cookie를 쓰는 재발급 API에 필요 |

---

## 3. OAuth 2.0 Authorization Code 인증 프로세스

OAuth는 사용자의 카카오 비밀번호를 이 서버가 직접 받지 않고, 카카오가 인증한 결과를 이용하는 위임 인증 방식이다. 이 프로젝트는 Authorization Code 방식을 사용한다.

### 각 주체의 역할

| 주체 | 역할 |
|---|---|
| 사용자 브라우저(Client) | 로그인 시작, 카카오 로그인·동의 화면 표시, 리다이렉트 수신 |
| 우리 서버(Client Application) | state 생성·검증, 인가 코드를 access token으로 교환, 사용자 정보 조회, 자체 JWT 발급 |
| 카카오 Authorization Server | 사용자 인증·동의 후 일회용 인가 코드 발급 |
| 카카오 Resource Server | 카카오 access token으로 사용자 정보 제공 |

### 현재 구현의 전체 흐름

```text
Browser                 Our Server                    Kakao
  │ GET /auth/kakao         │                           │
  │────────────────────────>│ state 생성, HttpOnly cookie│
  │ 302 Location: /oauth... │                           │
  │<────────────────────────│                           │
  │───────────────────────────────────────────────────>│ 로그인·동의
  │                                                     │
  │ GET /auth/kakao/callback?code=...&state=...         │
  │<───────────────────────────────────────────────────│
  │────────────────────────>│ state cookie와 비교        │
  │                         │ POST /oauth/token (code)  │
  │                         │──────────────────────────>│
  │                         │ 카카오 access token        │
  │                         │<──────────────────────────│
  │                         │ GET /v2/user/me           │
  │                         │──────────────────────────>│
  │                         │ providerUserId, nickname  │
  │                         │<──────────────────────────│
  │                         │ OAuthAccount 조회·생성     │
  │ Set-Cookie: Refresh/XSRF│ 자체 토큰 발급              │
  │ 302 frontend URL        │                           │
  │<────────────────────────│                           │
```

1. 브라우저가 `GET /auth/kakao`를 호출하면 서버는 고난도 난수 `state`를 생성해 `oauthState` HttpOnly·SameSite=Lax 쿠키에 저장하고 카카오 인가 URL로 302 리다이렉트한다.
2. 사용자는 카카오에서 로그인·동의를 수행한다. 카카오는 등록된 redirect URI에 짧은 수명의 일회용 authorization code와 state를 붙여 브라우저를 되돌린다.
3. 서버는 callback의 state와 Cookie의 state를 상수 시간 비교한다. 불일치하면 Cookie를 지우고 로그인 처리를 중단한다.
4. `KakaoOAuthClient`는 서버 간 통신으로 인가 코드를 카카오 access token으로 교환하고, 그 access token으로 카카오 사용자 ID·닉네임을 조회한다.
5. `OAuthLoginService`는 `(provider, providerUserId)`로 `OAuthAccount`를 조회한다. 최초 로그인이라면 비밀번호가 없는 소셜 전용 `Member`와 연결 계정을 만든다.
6. 서버는 이 서비스 전용 Access/Refresh Token을 발급하고 Refresh Cookie와 CSRF Cookie를 설정한 뒤 프런트엔드로 리다이렉트한다.
7. 프런트엔드는 필요할 때 `/auth/reissue`로 Access Token을 받는다. 카카오 access token을 브라우저나 우리 서비스의 인증 수단으로 재사용하지 않는다.

### OAuth에서 지켜야 할 경계

- **인가 코드와 카카오 client secret은 서버에만 둔다.** 브라우저에 client secret을 보내면 안 된다.
- **redirect URI는 카카오 개발자 콘솔에 등록한 정확한 주소와 일치**해야 한다.
- **state 검증은 필수**다. 없으면 공격자가 자신의 카카오 계정 로그인 결과를 피해자 브라우저에 연결하는 로그인 CSRF·계정 혼동 공격이 가능하다.
- 카카오의 식별자는 이메일이 아니라 `(provider, providerUserId)` 조합으로 관리한다. 이메일만으로 기존 비밀번호 계정과 자동 병합하면 계정 탈취 경로가 될 수 있다.
- provider 확장은 `OAuthProvider` enum과 `OAuthClient` 구현체를 추가하는 방식이다. 공통 callback·계정 연결·자체 토큰 발급 로직은 바꾸지 않는다.

---

## 4. 현재 프로젝트의 보안 계약

### 인증 API와 토큰 전달 방식

| API | Access Token | Refresh Token | 추가 검증 |
|---|---|---|---|
| `POST /auth/login` | 응답 `Authorization` 헤더와 JSON body | `refreshToken` HttpOnly Cookie | 요청 DTO 형식·비밀번호 검증 |
| `POST /auth/reissue` | 새 응답 `Authorization` 헤더와 JSON body | 요청 Cookie → 새 Cookie로 교체 | `XSRF-TOKEN` Cookie와 `X-XSRF-TOKEN` Header 비교 |
| `POST /auth/logout` | 요청 `Authorization: Bearer` | 요청 Cookie의 family 폐기 및 Cookie 삭제 | Access `jti` blacklist 등록 |
| `DELETE /auth/withdraw` | 요청 `Authorization: Bearer` | 모든 활성 family 폐기 및 Cookie 삭제 | 현재 회원 soft delete |
| `GET /auth/{provider}/callback` | 프런트 리다이렉트 뒤 재발급으로 수령 | Refresh·CSRF Cookie 설정 | OAuth state Cookie와 query state 비교 |

- Access Token은 JavaScript 메모리에만 보관한다. `localStorage`·`sessionStorage`는 XSS 발생 시 탈취하기 쉬우므로 사용하지 않는다.
- Refresh Token은 JavaScript가 읽을 수 없는 `HttpOnly; Secure; SameSite=Strict` Cookie로 보관한다.
- `XSRF-TOKEN`은 재발급 때만 JavaScript가 읽어 헤더로 복사하는 Cookie다. 원본 Refresh Token이 아니다.
- 응답 DTO에서 게시글 제목·내용·작성자명과 회원 이름·이메일을 HTML escape한다. 프런트도 사용자 입력을 `innerHTML`이 아닌 텍스트 렌더링으로 표시해야 한다.

### 코드 구조와 책임

| 구성 요소 | 책임 |
|---|---|
| `JwtProvider` | Access/Refresh JWT 발급과 서명·issuer·claim 검증 |
| `TokenService` | Refresh 해시 저장, rotation, 재사용 탐지, Access blacklist, 로그아웃·탈퇴 폐기 |
| `JwtAuthenticationFilter` | Access JWT 검증 후 `memberId`, `ROLE_*`를 SecurityContext에 설정 |
| `SecurityConfig` | 공개 경로, 인증 필요 경로, `ROLE_ADMIN` 인가와 stateless filter-chain 구성 |
| `OAuthLoginService` / `OAuthClient` | provider별 API 호출과 공통 계정 연결·자체 토큰 발급 분리 |

`GET /members`는 `ROLE_ADMIN`만 접근할 수 있다. 게시글 수정·삭제와 좋아요 요청은 인증된 `memberId`를 사용하므로, 요청 body로 다른 회원 ID를 넘겨 권한을 우회할 수 없다.

---

## 5. 제출 전 확인할 흐름

- 이메일 회원가입 → 로그인 → Access Header로 보호 API 요청 → Refresh 재발급 → 로그아웃/탈퇴 흐름
- 일반 사용자와 관리자 계정의 `GET /members` 인가 차이
- 카카오 로그인 시작 → state 검증 → Redirect → Refresh Cookie 발급 흐름
- XSS 입력이 게시글·회원 응답에서 HTML escape되는지, Refresh Token이 JavaScript에 노출되지 않는지

이 문서는 세미나 키워드의 개념과 실제 구현을 연결하기 위한 정리다. 전체 패키지 구조와 API 목록은 [`architecture.md`](./architecture.md)를 기준으로 한다.
