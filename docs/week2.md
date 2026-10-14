# 2차 과제 키워드 정리

SOPT 39기 2차 서버 과제 키워드 학습 정리.

> 전체 아키텍처 설명은 [`architecture.md`](./architecture.md) 참고.

---

## 1. `record`

Java 14에서 preview, 16에서 정식 도입된 **불변 데이터 클래스**를 간결하게 선언하는 문법.

```java
public record PostRequest(String title, String content, Category category, String author) {}
```

### 자동 생성되는 것
- `private final` 필드
- 모든 필드를 받는 **canonical constructor**
- 각 필드에 대한 **getter**(`title()`, `content()` — `getXxx`가 아님!)
- `equals()`, `hashCode()`, `toString()`

### 특징
- 모든 필드가 **final** → 생성 후 상태 변경 불가
- 상속 불가 (`final` 클래스)
- 추가 메서드/정적 팩토리 메서드는 선언 가능

### 왜 쓰나
DTO/VO처럼 **데이터만 담는 객체**에 적합. Lombok `@Getter`/`@AllArgsConstructor` 없이도 보일러플레이트 제거. 불변성 보장으로 **멀티스레드 환경에서도 안전**.

### 이 프로젝트에서의 사용
- `PostRequest`, `PostResponse`, `PostSummaryResponse`, `PostPageResponse`
- `ApiResponse<T>` — 공통 응답 포맷

---

## 2. `@Controller` vs `@RestController`

| 구분 | `@Controller` | `@RestController` |
|------|--------------|-------------------|
| 반환값 | **뷰 이름**(View) | **데이터**(JSON/XML) |
| 내부 | `@Controller` | `@Controller` + `@ResponseBody` |
| 용도 | SSR (서버 사이드 렌더링) | REST API |
| 응답 변환 | ViewResolver가 템플릿 렌더링 | `HttpMessageConverter`가 객체 → JSON |

### `@Controller` 예시
```java
@Controller
public class PageController {
    @GetMapping("/home")
    public String home() {
        return "home";   // → templates/home.html 렌더링
    }
}
```

### `@RestController` 예시
```java
@RestController
public class PostController {
    @GetMapping("/posts/{id}")
    public PostResponse getPost(@PathVariable int id) {
        return postService.getPostById(id);   // → JSON으로 변환되어 응답 body에 담김
    }
}
```

### 메서드 단위 `@ResponseBody`
`@Controller`에서도 특정 메서드만 데이터를 반환하고 싶다면 `@ResponseBody`를 메서드에 붙이면 된다. `@RestController`는 이걸 **클래스 전체에 적용**한 것일 뿐.

### 이 프로젝트에서의 선택
REST API 서버이므로 `@RestController` 사용. 모든 메서드가 JSON을 반환한다.

---

## 3. DTO / VO / DAO

세 가지 모두 "데이터를 담거나 다루는 객체"지만 **역할이 다르다**.

### DTO (Data Transfer Object)
- **계층 간 데이터 전달**을 위한 객체
- 주로 Controller ↔ Service, 외부 API ↔ 서버 간 데이터 운반
- 가변/불변 둘 다 가능하지만 보통 **불변**(record로 선언)
- 비즈니스 로직 없음, 데이터만 보유

```java
public record PostRequest(String title, String content, Category category, String author) {}
```

### VO (Value Object)
- **값 그 자체**를 표현하는 객체 (돈, 좌표, 주소 등)
- **불변**이어야 하고, `equals()`/`hashCode()`로 **값 동등성** 비교
- 같은 값이면 같은 객체로 취급 (`Money(1000).equals(Money(1000))` → true)
- DDD(도메인 주도 설계)에서 중요한 개념

```java
public record Money(long amount, Currency currency) {}
```

### DAO (Data Access Object)
- **데이터베이스 접근**을 담당하는 객체
- SQL/JPA 쿼리를 캡슐화해 영속 계층과 분리
- Spring에서는 **Repository**가 유사한 역할

```java
@Repository
public class PostRepository {
    public Post findByIndex(int index) { ... }
    public void save(Post post) { ... }
}
```

### 핵심 비교

| 항목 | DTO | VO | DAO |
|------|-----|-----|-----|
| 목적 | 데이터 전달 | 값 표현 | 데이터 접근 |
| 상태 | 가변 or 불변 | **불변** | 보통 stateless |
| 비즈니스 로직 | X | 소량 가능 | X (쿼리만) |
| 동등성 | 보통 참조 비교 | **값 비교** | - |

### 이 프로젝트에서의 매핑
- **DTO**: `PostRequest`, `PostResponse`, `PostSummaryResponse`, `PostPageResponse`
- **VO**: 현재 없음 (`Category` enum이 가까운 역할)
- **DAO**: `PostRepository`

---

## 4. RESTful

**REST**(REpresentational State Transfer) 원칙을 지키는 API 설계 방식.

### 핵심 원칙 6가지

1. **Client-Server 분리** — 서버는 자원, 클라이언트는 UI
2. **Stateless** — 각 요청은 독립적, 서버는 세션 상태 저장 X
3. **Cacheable** — 응답은 캐시 가능 여부를 명시
4. **Uniform Interface** — 통일된 자원 접근 방식 (가장 중요)
5. **Layered System** — 중간 계층(프록시, 로드밸런서) 허용
6. **Code on Demand** (선택) — 서버가 실행 코드 전송 가능

### URI 설계 규칙

| 좋은 예 | 나쁜 예 |
|---------|---------|
| `GET /posts` | `GET /getPosts` |
| `GET /posts/1` | `GET /post?id=1` |
| `POST /posts` | `POST /createPost` |
| `DELETE /posts/1` | `GET /deletePost?id=1` |

- **자원은 명사**로, **동작은 HTTP 메서드**로 표현
- 복수형 사용 (`/posts`)
- 계층 구조는 슬래시로 (`/posts/1/comments`)

### HTTP 메서드 의미

| 메서드 | 의미 | 멱등성 | 안전성 |
|--------|------|--------|--------|
| GET    | 조회 | O | O |
| POST   | 생성 | X | X |
| PUT    | 전체 수정 | O | X |
| PATCH  | 부분 수정 | X | X |
| DELETE | 삭제 | O | X |

- **멱등성**: 같은 요청을 여러 번 보내도 결과가 같음
- **안전성**: 서버 상태를 변경하지 않음

### HTTP 상태 코드

| 코드 | 의미 |
|------|------|
| 200 OK | 성공 |
| 201 Created | 자원 생성 성공 |
| 204 No Content | 성공, 응답 바디 없음 |
| 400 Bad Request | 잘못된 요청 |
| 401 Unauthorized | 인증 실패 |
| 403 Forbidden | 권한 없음 |
| 404 Not Found | 자원 없음 |
| 405 Method Not Allowed | 메서드 미지원 |
| 500 Internal Server Error | 서버 오류 |

### 이 프로젝트에서의 적용
```
POST   /posts          → 작성 (201 Created)
GET    /posts          → 전체 조회 (200)
GET    /posts/{index}  → 단건 조회 (200)
PUT    /posts/{index}  → 수정 (200)
DELETE /posts/{index}  → 삭제 (200)
```

---

## 5. Bean 생명주기

Spring IoC 컨테이너가 Bean을 **생성 → 초기화 → 사용 → 소멸**시키는 전체 과정.

### 생명주기 단계

```
1. Bean 정의 로드          (설정 파일/@Component 스캔)
          ▼
2. Bean 인스턴스 생성       (생성자 호출)
          ▼
3. 의존성 주입              (@Autowired, 생성자 주입)
          ▼
4. BeanNameAware 등 Aware 인터페이스 콜백
          ▼
5. BeanPostProcessor#postProcessBeforeInitialization
          ▼
6. @PostConstruct / InitializingBean#afterPropertiesSet / init-method
          ▼
7. BeanPostProcessor#postProcessAfterInitialization
          ▼
8. Bean 사용 (애플리케이션 실행 중)
          ▼
9. @PreDestroy / DisposableBean#destroy / destroy-method
          ▼
10. Bean 소멸
```

### 초기화 콜백 (3가지 방법)

```java
@Component
public class SomeBean implements InitializingBean, DisposableBean {

    // 1. @PostConstruct — 가장 권장 (JSR-250 표준)
    @PostConstruct
    public void init() { ... }

    // 2. InitializingBean 인터페이스 — Spring 종속적
    @Override
    public void afterPropertiesSet() { ... }

    // 3. @Bean(initMethod = "...") — 외부 라이브러리 설정에 유용
    // (설정 클래스에서 지정)
}
```

### 소멸 콜백

```java
@PreDestroy
public void cleanup() { ... }
```

### Bean Scope와 생명주기

| Scope | 생명주기 |
|-------|---------|
| **singleton** (기본) | 컨테이너 시작 ~ 종료까지 1개 |
| **prototype** | 요청마다 새로 생성, 소멸은 컨테이너가 관리 X |
| **request** / **session** | 웹 요청/세션 단위 |

### 의존성 주입 방식 3가지

1. **생성자 주입** (권장) — 불변성 보장, 테스트 용이, 순환 참조 컴파일 시점에 발견
2. **Setter 주입** — 선택적 의존성
3. **필드 주입** — 간결하지만 테스트 어렵고 불변성 깨짐 (비권장)

```java
@Service
public class PostService {
    private final PostRepository postRepository;

    // 생성자 주입 — Spring 4.3+에서는 @Autowired 생략 가능
    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }
}
```

### 이 프로젝트에서의 적용
- `@Service`로 등록된 `PostService`, `@Repository`로 등록된 `PostRepository`는 **싱글톤 Bean**
- 모두 **생성자 주입** 사용 — 불변성 보장
- Spring이 애플리케이션 시작 시 자동으로 생성 → 주입 → 소멸까지 관리
