# Architecture

SOPT 39기 2차 서버 과제 — Spring Boot 기반 게시글(Post) CRUD API 아키텍처 문서.

## 1. 레이어 구조

```
Client
  │
  ▼
┌─────────────────────────────────────────────────────┐
│  Controller  (PostController + PostControllerDocs)  │  ← HTTP 요청/응답, @Valid, Swagger 문서
└─────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────┐
│  Service     (PostService)                          │  ← 비즈니스 로직, 예외 발생
└─────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────┐
│  Repository  (PostRepository)                       │  ← 데이터 접근 (In-memory)
└─────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────┐
│  Entity      (Post, Category)                       │  ← 도메인 모델
└─────────────────────────────────────────────────────┘
```

레이어 간 통신은 위에서 아래로만 흐른다. Service는 Controller를 모르고, Repository는 Service를 모른다.

## 2. 패키지 구조

```
org.sopt.blog
├── Main.java                                  # Spring Boot 진입점
│
├── domain
│   └── post
│       ├── controller
│       │   ├── PostController.java            # @RestController, 라우팅
│       │   ├── PostControllerDocs.java        # Swagger 어노테이션 분리용 인터페이스
│       │   └── dto
│       │       ├── request
│       │       │   └── PostRequest.java       # 요청 DTO (record)
│       │       └── response
│       │           ├── PostResponse.java      # 단건 조회 응답
│       │           ├── PostSummaryResponse.java # 목록용 요약 응답
│       │           └── PostPageResponse.java  # 페이지네이션 응답
│       │
│       ├── service
│       │   └── PostService.java               # 비즈니스 로직
│       │
│       ├── repository
│       │   └── PostRepository.java            # In-memory 저장소 (CopyOnWriteArrayList)
│       │
│       └── entity
│           ├── Post.java                      # 도메인 모델
│           └── Category.java                  # enum (NOTICE, QUESTION, COMMENT)
│
└── global
    └── common
        ├── response
        │   └── ApiResponse.java               # 공통 응답 포맷 (code, message, data)
        │
        └── exception
            ├── ErrorCode.java                 # 에러 코드 enum
            ├── CustomException.java           # 비즈니스 예외
            ├── ExceptionDto.java              # (예비) 예외 응답 DTO
            └── GlobalExceptionHandler.java    # @RestControllerAdvice, 전역 예외 처리
```

- **domain**: 비즈니스 도메인별로 묶는다. 지금은 `post` 하나뿐이지만 향후 `comment`, `user` 등을 같은 레벨에 추가.
- **global/common**: 모든 도메인이 공유하는 응답/예외 모듈.

## 3. 핵심 설계 결정

### 3.1 공통 응답 포맷 — `ApiResponse<T>`
모든 응답은 `{ code, message, data }` 구조로 통일한다. `code`는 HTTP 상태 코드가 아닌 **자체 에러 코드**(예: `1103` = 게시글 없음)를 사용해 세분화한다.

```json
{ "code": 200, "message": "게시글 목록 조회에 성공했습니다.", "data": { ... } }
{ "code": 1103, "message": "index=5 번 게시글이 존재하지 않습니다.", "data": null }
```

### 3.2 전역 예외 처리 — `@RestControllerAdvice`
비즈니스 예외는 `CustomException(ErrorCode, 상세메시지)`로 던지고, `GlobalExceptionHandler`가 아래를 모두 가로채 공통 포맷으로 변환한다.

| 예외 | 상황 | ErrorCode |
|------|------|-----------|
| `CustomException` | 비즈니스 예외 | 커스텀 |
| `MethodArgumentNotValidException` | `@Valid` 실패 | `INVALID_INPUT_VALUE` |
| `MethodArgumentTypeMismatchException` | path/query 타입 오류 | `INVALID_TYPE_VALUE` |
| `HttpMessageNotReadableException` | JSON 파싱/enum 변환 실패 | `INVALID_JSON_FORMAT` |
| `HttpRequestMethodNotSupportedException` | 잘못된 HTTP 메서드 | `METHOD_NOT_ALLOWED` |
| `IllegalArgumentException` | 엔티티 생성자 유효성 실패 | `INVALID_INPUT_VALUE` |
| `Exception` | 그 외 모든 예외 | `INTERNAL_SERVER_ERROR` |

### 3.3 조회수 동시성 — `AtomicInteger`
`Post.viewCount`는 `AtomicInteger`로 선언해 `incrementAndGet()`으로 원자적으로 증가시킨다. 여러 스레드가 동시에 상세 조회를 쳐도 카운트가 유실되지 않는다.

### 3.4 저장소 동시성 — `CopyOnWriteArrayList`
In-memory 저장소는 `CopyOnWriteArrayList`로 선언해 읽기/쓰기 동시 접근 시 `ConcurrentModificationException`을 방지한다.

### 3.5 Swagger 분리 — `PostControllerDocs` 인터페이스
Swagger `@Operation`, `@ApiResponses` 등 문서화 어노테이션을 별도 인터페이스로 빼내어 Controller 본문의 가독성을 유지한다. Controller는 `implements PostControllerDocs`로 상속만 받는다.

### 3.6 DTO = record
불변성이 기본 요구사항인 요청/응답 DTO는 Java `record`로 선언한다. 필드/생성자/getter/`equals`/`hashCode`/`toString`을 자동 생성해 보일러플레이트를 제거.

## 4. API 엔드포인트

| Method | Path             | 설명                        | 응답 코드 |
|--------|------------------|-----------------------------|-----------|
| POST   | `/posts`         | 게시글 작성                 | 201       |
| GET    | `/posts`         | 전체 목록 (페이지네이션)    | 200       |
| GET    | `/posts/{index}` | 단건 상세 조회 (조회수 +1)  | 200       |
| PUT    | `/posts/{index}` | 게시글 수정                 | 200       |
| DELETE | `/posts/{index}` | 게시글 삭제                 | 200       |

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 5. 요청 흐름 예시 — 게시글 상세 조회

```
GET /posts/0
  │
  ▼
PostController#getPostById(0)
  │
  ▼
PostService#getPostById(0)
  ├─ findPost(0)                     # index 범위 검증, 실패 시 CustomException(POST_NOT_FOUND)
  ├─ post.increaseViewCount()        # AtomicInteger 원자적 +1
  └─ PostResponse.from(post)         # 엔티티 → DTO 변환
  │
  ▼
PostRepository#findByIndex(0)
  │
  ▼
ResponseEntity<ApiResponse<PostResponse>>
```

예외 경로:
```
존재하지 않는 index
  → CustomException(POST_NOT_FOUND)
  → GlobalExceptionHandler#handleCustomException
  → 404 { "code": 1103, "message": "...", "data": null }
```

## 6. 향후 개선 포인트

- **영속 저장소**: In-memory → H2 / MySQL + JPA
- **삭제 전략**: `List.remove(index)`는 뒤 index가 당겨지는 부작용. 고유 ID 기반 삭제로 전환
- **인증/인가**: `author`는 현재 요청 본문에서 받지만, 향후 로그인 사용자 기반으로 변경
- **테스트**: Controller / Service 단위 테스트, 통합 테스트 보강
