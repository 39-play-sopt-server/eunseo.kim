# Architecture

SOPT 39기 2차/3차 서버 과제 — Spring Boot + JPA + MySQL 기반 블로그 API 아키텍처 문서.

## 1. 레이어 구조

```
Client
  │
  ▼
┌─────────────────────────────────────────────────────────────┐
│  Controller  (PostController, MemberController, LikeController) │  ← HTTP, @Valid, Swagger
└─────────────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────────────┐
│  Service     (PostService, MemberService, LikeService)      │  ← 트랜잭션, 비즈니스 로직
└─────────────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────────────┐
│  Repository  (JpaRepository + JpaSpecificationExecutor)     │  ← Spring Data JPA
└─────────────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────────────┐
│  Entity      (Post, Member, Like, Category, BaseEntity)     │  ← JPA 매핑된 도메인
└─────────────────────────────────────────────────────────────┘
  │
  ▼
MySQL
```

Service는 Controller를 모르고, Repository는 Service를 모른다. Entity는 바깥 레이어에 의존하지 않는다.

## 2. 패키지 구조

```
org.sopt.blog
├── Main.java
│
├── domain
│   ├── post
│   │   ├── controller
│   │   │   ├── PostController.java
│   │   │   ├── PostControllerDocs.java
│   │   │   └── dto
│   │   │       ├── request  (PostRequest, PostUpdateRequest)
│   │   │       └── response (PostResponse, PostSummaryResponse, PostPageResponse)
│   │   ├── service          (PostService)
│   │   ├── repository
│   │   │   ├── PostRepository.java          # JpaRepository + JpaSpecificationExecutor
│   │   │   └── PostSpecifications.java      # 동적 쿼리 Specification 모음
│   │   └── entity           (Post, Category)
│   │
│   ├── member
│   │   ├── controller / service / repository
│   │   └── entity           (Member)         # Soft Delete (deletedAt)
│   │
│   └── like
│       ├── controller / service / repository
│       └── entity           (Like)           # UNIQUE(post_id, member_id)
│
└── global
    └── common
        ├── entity           (BaseEntity: createdAt, updatedAt)
        ├── response         (ApiResponse)
        └── exception        (ErrorCode, CustomException, GlobalExceptionHandler)
```

- **domain**: 비즈니스 도메인별로 분리. 각 도메인은 controller/service/repository/entity를 내부에 둠.
- **global/common**: 공통 응답 포맷, 예외 처리, 공통 엔티티 상위 클래스.

## 3. 핵심 설계 결정

### 3.1 공통 응답 포맷 — `ApiResponse<T>`
모든 응답은 `{ code, message, data }` 구조로 통일. `code`는 HTTP 상태 코드가 아닌 **자체 에러 코드**(예: `1103` = 게시글 없음)로 세분화.

```json
{ "code": 200, "message": "게시글 목록 조회에 성공했습니다.", "data": { ... } }
{ "code": 1103, "message": "id=5 번 게시글이 없습니다.", "data": null }
```

### 3.2 전역 예외 처리 — `@RestControllerAdvice`
비즈니스 예외는 `CustomException(ErrorCode, 상세메시지)`로 던지고, `GlobalExceptionHandler`가 가로채 공통 포맷으로 변환.

| 예외 | 상황 | ErrorCode |
|------|------|-----------|
| `CustomException` | 비즈니스 예외 | 커스텀 |
| `MethodArgumentNotValidException` | `@Valid` (body) 실패 | `INVALID_INPUT_VALUE` |
| `ConstraintViolationException` | `@Validated` (query/path) 실패 | `INVALID_INPUT_VALUE` |
| `MethodArgumentTypeMismatchException` | query/path 타입 오류 | `INVALID_TYPE_VALUE` |
| `HttpMessageNotReadableException` | JSON 파싱/enum 변환 실패 | `INVALID_JSON_FORMAT` |
| `HttpRequestMethodNotSupportedException` | 잘못된 HTTP 메서드 | `METHOD_NOT_ALLOWED` |
| `Exception` | 그 외 | `INTERNAL_SERVER_ERROR` |

### 3.3 영속성 — Spring Data JPA + MySQL
- `JpaRepository`로 기본 CRUD 자동 생성.
- `@Transactional` 범위 내에서 Entity 필드를 변경하면 **dirty checking**으로 커밋 시점에 UPDATE가 자동 발행된다. 수정 API는 `setter`가 아닌 도메인 메서드(`update(...)`)를 호출해 명시적으로 상태를 바꾼다.
- `BaseEntity`에 `@CreatedDate`, `@LastModifiedDate`로 생성/수정 시각을 자동 관리 (`@EnableJpaAuditing`).

### 3.4 동적 검색 — `JpaSpecificationExecutor` + `Specification`
목록 조회는 `title`, `category`, `memberId` 등 선택적 필터가 조합된다. 매 조합마다 `findByXxxAnd...` 메서드를 만드는 대신 **Specification**으로 조건을 조립한다.

```java
Specification<Post> spec = Specification.allOf(PostSpecifications.fetchMember());
if (title != null)      spec = spec.and(PostSpecifications.titleContains(title));
if (category != null)   spec = spec.and(PostSpecifications.hasCategory(category));
if (memberId != null)   spec = spec.and(PostSpecifications.hasMember(memberId));
postRepository.findAll(spec, pageable);
```

- 각 `Specification`은 `(root, query, cb) -> Predicate` 람다로 **JPA Criteria** 트리의 일부를 반환.
- FK 필드는 **Java 필드명**으로 경로 탐색 (`root.get("member").get("id")`). 컬럼명 `member_id`가 아님.

### 3.5 N+1 회피 — `fetch join` in Specification
`Post` 목록을 뽑고 각 Post의 `member.name`을 쓰려 하면 Post 1건마다 Member SELECT가 발생(N+1). `fetchMember()`에서 `root.fetch("member")`로 **한 번의 join**으로 가져온다.

```java
public static Specification<Post> fetchMember() {
    return (root, query, cb) -> {
        if (Long.class != query.getResultType()) {   // count 쿼리에선 fetch 금지
            root.fetch("member");
        }
        return cb.conjunction();   // "항상 참" (조건 없이 join만)
    };
}
```

count 쿼리에 fetch가 섞이면 Hibernate가 예외를 던지므로 `query.getResultType()`로 분기.

### 3.6 좋아요 동시성 — UNIQUE 제약 + Pessimistic Lock
`Like` 엔티티에 `UNIQUE(post_id, member_id)`로 중복 좋아요를 DB 레벨에서 차단.

단, 동일 사용자가 동시에 여러 요청을 보내면 UNIQUE 인덱스의 **gap/next-key lock**끼리 교차하면서 MySQL이 데드락을 감지해 `LockAcquisitionException`을 던지는 케이스가 있다. 이를 피하기 위해 좋아요 로직은 먼저 **Post 행을 `SELECT ... FOR UPDATE`로 선점**해서 순차화한다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select p from Post p where p.id = :id")
Optional<Post> findByIdForUpdate(@Param("id") Long id);
```

이후 흐름:
1. Post 행 lock 획득 → 2. 좋아요 존재 여부 확인 → 3. Like INSERT + `likeCount++`.
동시에 들어온 N개의 요청 중 1개만 성공, 나머지는 `LIKE_ALREADY_EXISTS(409)`로 깔끔하게 떨어진다.

### 3.7 회원 Soft Delete
`Member`는 `deletedAt` 컬럼을 두고 `markDeleted()`로 논리 삭제. 게시글 FK는 유지되므로 "탈퇴한 회원이 쓴 글" 표시가 가능. 자세한 선택 근거는 `docs/week3.md` 참조.

### 3.8 Swagger 분리 — `*ControllerDocs` 인터페이스
`@Operation`, `@ApiResponses` 등 문서화 어노테이션을 별도 인터페이스에 두고 Controller는 `implements ...Docs`로 상속만. 본문 가독성 유지.

### 3.9 DTO = record
불변 요청/응답 DTO는 Java `record`. 필드/생성자/accessor/`equals`/`hashCode`/`toString` 자동 생성.

## 4. API 엔드포인트

### Post
| Method | Path | 설명 | 쿼리 파라미터 |
|--------|------|------|---------------|
| POST   | `/posts`           | 게시글 작성 | - |
| GET    | `/posts`           | 목록 (페이지네이션 + 필터) | `title`, `category`, `memberId`, `page`, `size` |
| GET    | `/posts/{postId}`  | 상세 조회 (조회수 +1) | - |
| PUT    | `/posts/{postId}`  | 수정 | - |
| DELETE | `/posts/{postId}`  | 삭제 | - |

### Member
| Method | Path | 설명 |
|--------|------|------|
| POST   | `/members`             | 회원 가입 |
| GET    | `/members/{memberId}`  | 단건 조회 |
| DELETE | `/members/{memberId}`  | 탈퇴 (Soft Delete) |

### Like
| Method | Path | 설명 |
|--------|------|------|
| POST   | `/posts/{postId}/likes`   | 좋아요 등록 |
| DELETE | `/posts/{postId}/likes`   | 좋아요 취소 |

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 5. 요청 흐름 예시 — 게시글 상세 조회

```
GET /posts/1
  │
  ▼
PostController#getPostById(1)
  │
  ▼
PostService#getPostById(1)   @Transactional
  ├─ postRepository.findById(1)      # Optional<Post>
  ├─ post.increaseViewCount()        # dirty checking → UPDATE
  └─ PostResponse.from(post)
  │
  ▼
ResponseEntity<ApiResponse<PostResponse>>
```

예외 경로:
```
존재하지 않는 id
  → CustomException(POST_NOT_FOUND)
  → GlobalExceptionHandler#handleCustomException
  → 404 { "code": 1103, "message": "id=5 번 게시글이 없습니다.", "data": null }
```

## 6. 향후 개선 포인트

- **검색 성능**: `title LIKE '%키워드%'`는 B-tree 인덱스가 안 먹음. FULLTEXT 인덱스 혹은 외부 검색 엔진 도입 검토.
- **인증/인가**: 현재 `memberId`를 요청 파라미터로 받음. 로그인 세션/JWT 기반으로 전환 필요.
- **테스트**: Controller / Service / Repository 단위 + 통합 테스트 보강 (특히 좋아요 동시성 재현 테스트).
- **캐싱**: 조회수 증가는 매 요청마다 UPDATE. 핫 포스트는 Redis + 배치 flush로 부하 분산 가능.
