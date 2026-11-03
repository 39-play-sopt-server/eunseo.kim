# Week 4 키워드 과제

SOPT 39기 4차 서버 세미나 키워드 정리.

> 전체 아키텍처 설명은 [`architecture.md`](./architecture.md) 참고.

---

## 1. CI/CD

### CI (Continuous Integration, 지속적 통합)
여러 사람이 작성한 코드를 **자주 main에 합치고, 합칠 때마다 자동으로 빌드/테스트**해서 문제를 일찍 발견하는 방식.

- 통합을 미루면 브랜치 간 차이가 커져 머지 충돌과 버그가 한꺼번에 터진다.
- "내 컴퓨터에선 되는데"를 막기 위해 **깨끗한 환경(CI 서버)** 에서 매번 같은 방법으로 빌드한다.

### CD (Continuous Delivery / Deployment)
| 구분 | 의미 |
|------|------|
| Continuous **Delivery** | 언제든 배포 가능한 상태의 결과물을 자동으로 만들어 둠. 실제 운영 반영은 **사람이 버튼**을 눌러서 |
| Continuous **Deployment** | 검증을 통과하면 운영 서버까지 **자동 반영** |

### 왜 필요한가
- **수동 배포의 실수 제거**: SSH 접속 → pull → 빌드 → 기존 프로세스 종료 → 재실행을 매번 손으로 하면 순서를 빼먹거나 다른 버전을 띄우기 쉽다.
- **배포 시간 단축**: 작은 변경도 부담 없이 자주 배포 → 한 번에 바뀌는 양이 작아 문제 원인 추적이 쉽다.
- **재현성**: 배포 절차가 코드(워크플로 파일)로 남아 누가 실행해도 같은 결과.

### 이번 과제에 적용한 방식
GitHub Actions로 **Continuous Deployment** 구성 (`.github/workflows/deploy.yml`).

```
main에 push (PR 머지)
  → GitHub 러너: checkout → JDK 21 → ./gradlew build
  → scp로 jar를 EC2에 전송
  → ssh로 EC2 접속: git pull → docker compose up -d --build
```

- **빌드는 러너에서**: EC2(t3.micro, 메모리 1GB)는 Gradle 빌드가 느리고 메모리가 부족해서, 빌드는 GitHub 러너가 하고 EC2는 jar로 이미지만 다시 만든다.
- **이미지 레지스트리(Docker Hub 등) 미사용**: 서버가 1대라 jar를 직접 보내는 쪽이 단순하다. 서버가 늘어나면 레지스트리에 이미지를 올리고 각 서버가 pull하는 방식이 낫다.
- **비밀값 분리**: GitHub Secrets에는 SSH 접속 정보(`EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`)만 두고, DB 접속 정보는 서버의 `.env`(git 미포함)에서 compose `env_file`로 주입.
- 이번 구성에는 테스트 단계가 없어(`-x test`) 엄밀한 CI라기보다는 **CD 위주**. 테스트가 생기면 PR 단계에서 `./gradlew test`를 돌리는 워크플로를 따로 두는 게 다음 단계.

---

## 2. HTTPS를 사용해야 하는 이유

HTTP는 요청/응답을 **평문**으로 주고받는다. 중간 경로(공용 와이파이, 통신사, 프록시 등)에 있는 누구나 내용을 보고 바꿀 수 있다.

| 위협 | HTTP | HTTPS |
|------|------|-------|
| **도청** (내용 엿보기) | 비밀번호, 토큰, 개인정보가 그대로 노출 | 암호화되어 읽을 수 없음 |
| **변조** (중간에서 내용 바꾸기) | 응답에 광고·악성 스크립트 삽입 가능 | 무결성 검사로 변조 시 연결이 깨짐 |
| **위장** (가짜 서버) | 상대가 진짜 서버인지 확인할 방법 없음 | 인증서로 서버 신원 확인 |

### 그 밖의 현실적인 이유
- **브라우저 정책**: HTTP 사이트는 "주의 요함" 표시. Mixed Content(HTTPS 페이지에서 HTTP API 호출)는 브라우저가 차단 → 프론트엔드가 HTTPS면 API 서버도 HTTPS여야 한다.
- **최신 기능 제한**: Service Worker, Geolocation, HTTP/2 등은 HTTPS에서만 동작.
- **쿠키 보안**: `Secure` 속성 쿠키는 HTTPS로만 전송.
- **검색 엔진**: HTTPS를 순위 신호로 사용.

### 이번 과제에 적용한 방식
- 도메인 `eunseo-blog.o-r.kr` + **Let's Encrypt** 무료 인증서.
- **Nginx가 HTTPS를 받아서 복호화(SSL Termination)** 한 뒤, 내부의 Spring(8080)에는 HTTP로 전달 (리버스 프록시).
- 80번으로 들어온 요청은 **301로 HTTPS 리다이렉트**.
- Spring은 자신이 HTTP로 요청을 받았다고 생각하므로, Nginx가 `X-Forwarded-Proto` 등을 넘기고 `server.forward-headers-strategy: framework`로 원래 요청이 HTTPS였음을 인식시킴 → Swagger 서버 URL이 `https://`로 생성됨.
- 인증서 갱신: Nginx가 컨테이너로 옮겨가면서 certbot을 **webroot 방식**으로 변경. certbot이 `/var/www/certbot`에 확인 파일을 두면 컨테이너 Nginx가 `/.well-known/acme-challenge/` 경로로 서빙하고, 갱신 후 훅으로 Nginx를 리로드.

---

## 3. SSL / TLS

### 관계
- **SSL** (Secure Sockets Layer): 넷스케이프가 만든 초기 프로토콜. SSL 2.0/3.0 모두 취약점으로 **폐기**.
- **TLS** (Transport Layer Security): SSL을 표준화·개선한 후속 프로토콜. 현재는 **TLS 1.2 / 1.3** 사용.
- 지금 "SSL 인증서", "SSL 설정"이라고 부르는 것도 실제로는 전부 TLS. 이름만 관습적으로 남아 있다. (Nginx 설정의 `ssl_certificate`도 마찬가지)

`HTTPS = HTTP + TLS` — HTTP 자체는 그대로이고, 그 아래에 TLS 암호화 계층을 깐 것.

### 사용하는 암호 기술
| 기술 | 역할 |
|------|------|
| **비대칭키** (공개키/개인키) | 연결 초기에 서버 인증 + 대칭키를 안전하게 합의. 느리다 |
| **대칭키** | 실제 데이터 암호화. 빠르다 |
| **해시(MAC/AEAD)** | 데이터가 중간에 바뀌지 않았는지 검증 |

비대칭키는 느리므로 **처음에만** 쓰고, 합의한 대칭키로 이후 통신을 암호화하는 **하이브리드 방식**.

### TLS 핸드셰이크 (TLS 1.2 기준 흐름)
1. **Client Hello**: 클라이언트가 지원하는 TLS 버전, 암호 스위트 목록, 랜덤값 전송
2. **Server Hello + Certificate**: 서버가 사용할 버전/암호 스위트를 고르고, **인증서**(서버 공개키 포함) 전송
3. **인증서 검증**: 클라이언트가 인증서의 서명을 따라 올라가 브라우저/OS에 내장된 **루트 CA**까지 신뢰 체인 확인, 도메인 일치와 만료일 확인
4. **키 교환**: (EC)DHE 등으로 양쪽이 같은 **세션 키(대칭키)** 를 만든다. 세션 키 자체는 네트워크에 흐르지 않음
5. **Finished**: 이후 모든 데이터는 세션 키로 암호화

TLS 1.3은 이 과정을 **1-RTT**로 줄이고(재접속 시 0-RTT 가능), 안전하지 않은 암호 스위트(RSA 키 교환, CBC 모드 등)를 제거했다.

### 인증서와 CA
- 인증서 = "이 공개키는 이 도메인의 것"이라는 사실을 **CA(인증기관)가 서명**해서 보증한 문서.
- 이번 과제 인증서: `CN=eunseo-blog.o-r.kr`, 발급자 Let's Encrypt `YE2`, 유효기간 90일(2026-11-03 ~ 2027-02-01).
- Let's Encrypt는 **ACME 프로토콜**로 도메인 소유를 자동 확인(HTTP-01 챌린지: 특정 경로에 확인 파일을 올려 증명)하고 무료로 발급. 유효기간이 짧은 대신 자동 갱신을 전제로 한다.
- `fullchain.pem`: 서버 인증서 + 중간 CA 인증서. 클라이언트가 루트까지 체인을 이을 수 있게 함께 보낸다.
- `privkey.pem`: 서버 개인키. 유출되면 누구나 이 서버로 위장할 수 있으므로 절대 공개 금지.
