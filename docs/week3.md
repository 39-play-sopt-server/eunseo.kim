# Week 3 키워드 과제

SOPT 39기 3차 서버 세미나 키워드 정리.

## 1. 페이지네이션 — `Page` vs `Slice`

| 구분 | `Page<T>` | `Slice<T>` |
|------|-----------|------------|
| count 쿼리 | 매 요청 **별도 실행** | 실행 안 함 |
| 전체 개수 | `getTotalElements()`, `getTotalPages()` 제공 | 제공 안 함 |
| 다음 페이지 존재 여부 | 전체 개수로 계산 | `size + 1`건을 조회해서 판단 |
| 비용 | 데이터 쿼리 + count 쿼리 = **2쿼리** | 데이터 쿼리 **1쿼리** |
| 어울리는 UI | "1 / 2 / 3 ... 10" 같은 **번호 네비게이션** | "더 보기" / 무한 스크롤 |

### 선택: `Page`
이번 과제의 목록 API는 `totalPages`, `totalElements`를 응답에 포함하도록 명세돼 있어 전체 개수가 필요하다. count 쿼리 비용을 감수하고 `Page`를 선택.

데이터가 수백만 건으로 커지면 count가 느려질 수 있고, 그 때는 "더보기" UI로 바꿔 `Slice`로 전환하는 게 자연스럽다.

---

## 2. DB 인덱스

### 왜 쓰는가
풀 스캔(모든 행을 하나씩 확인)을 피하기 위해 **B-tree** 등으로 정렬된 보조 자료구조를 둬 조회를 로그 시간에 끝내게 한다.

### 과제의 제목 검색이 인덱스를 못 쓰는 이유
```sql
SELECT * FROM post WHERE title LIKE '%키워드%';
```
B-tree 인덱스는 **접두사(prefix)** 기준으로 정렬되어 있어 `'키워드%'` (뒤쪽 와일드카드)에는 걸리지만, 앞뒤 와일드카드 `'%키워드%'`에는 걸리지 않아 결국 풀 스캔이 된다.

### 대안
- **FULLTEXT 인덱스**: MySQL이 지원. 토큰 단위 역색인을 만들어 `MATCH(title) AGAINST('키워드')`로 조회. 한글은 ngram 파서 설정 필요.
- **외부 검색 엔진**: Elasticsearch / OpenSearch. 색인/동기화 비용은 늘지만 유연한 쿼리와 랭킹이 가능.

### 인덱스의 비용
- INSERT/UPDATE/DELETE 시 **인덱스도 함께 갱신** → 쓰기 성능 저하.
- 디스크/메모리 공간을 추가로 소비.
- 카디널리티가 낮은 컬럼(예: boolean)은 효과가 거의 없음.

**결론**: 과제 수준에서는 FULLTEXT도 과하므로 평문 LIKE로 두고, 트래픽이 커지면 FULLTEXT → 검색 엔진 순으로 단계적 도입을 고려.

---

## 3. 회원 탈퇴 시 게시글 처리

### 선택지
| 전략 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **Hard Delete + CASCADE** | 회원 삭제 시 게시글도 모두 삭제 | 단순, 저장공간 회수 | 글/댓글 생태계 파괴, 복구 불가 |
| **Hard Delete + 작성자 익명화** | 회원 row 삭제, 게시글은 "탈퇴한 회원"으로 표기 (FK NULL 또는 "deleted" 유저로 재할당) | 글은 유지 | 로그/감사 추적 어려움 |
| **Soft Delete** | 회원 row는 유지, `deletedAt`만 세팅 | 데이터·관계 보존, 복구/감사 용이 | 조회 쿼리에 `deletedAt IS NULL` 조건 상시 추가, 저장공간 유지 |

### 선택: Soft Delete
블로그 성격상 **탈퇴해도 글은 남아야** 자연스럽다. 또한 Soft Delete는 실수로 탈퇴한 사용자 복구에 유리하고, 분쟁 발생 시 "누가 썼는지" 추적이 가능하다.

구현: `Member` 엔티티에 `LocalDateTime deletedAt` 필드를 두고 `markDeleted()` 메서드로 세팅. 조회 쿼리는 `deletedAt IS NULL`을 기본 조건으로 깐다 (필요 시 Hibernate `@SQLRestriction` / `@Where` 활용 가능).

---

## 4. DB `ON DELETE CASCADE` vs JPA `CascadeType`

둘 다 "연쇄 삭제/영속"을 표현하지만 **동작 레벨과 범위**가 완전히 다르다.

| 구분 | DB `ON DELETE CASCADE` | JPA `CascadeType.REMOVE` / `ALL` |
|------|------------------------|----------------------------------|
| 레벨 | **DB 제약 조건** (스키마) | **JPA/Hibernate 영속성 컨텍스트** (애플리케이션) |
| 트리거 시점 | SQL `DELETE` 실행 시 DB가 처리 | `EntityManager.remove()` 호출 시 JPA가 자식도 remove 호출 |
| 발행되는 SQL | 부모 DELETE 1건 → DB가 자식도 삭제 | 자식 각각에 대해 DELETE SQL 발행 (N+1 발생 가능) |
| 영속성 컨텍스트 반영 | **반영 안 됨** (JPA 모름) → 1차 캐시와 DB 불일치 가능 | 그대로 반영 |
| 라이프사이클 콜백 | 자식의 `@PreRemove` 등 **호출 안 됨** | 자식의 콜백도 정상 호출 |
| 다른 범위 | `CASCADE`만 존재 (삭제) | `PERSIST`, `MERGE`, `REMOVE`, `DETACH`, `REFRESH`, `ALL` |

### 어떻게 조합하나
- **DB CASCADE만**: 애플리케이션 어디서든 FK 참조만 끊어지면 자동 삭제. JPA 1차 캐시 불일치 주의.
- **JPA Cascade만**: SQL은 자식 각각 DELETE가 날아감 → 성능 저하. 대신 콜백/감사 로그가 정확히 동작.
- **둘 다**: 보통 **DB CASCADE로 데이터 무결성 보장 + JPA Cascade로 라이프사이클 관리**를 함께 설정. 다만 두 번 삭제되지 않도록 `orphanRemoval` 등과의 상호작용을 테스트해야 한다.

### 이번 과제는 어떻게?
Soft Delete를 택했기 때문에 **어느 쪽도 안 씀**. 회원이 "탈퇴"해도 게시글/좋아요는 그대로 둬야 하기 때문. 만약 Hard Delete였다면 "FK 무결성 측면에선 DB CASCADE가 안전, 비즈니스 로직 측면에선 JPA Cascade가 유연"이라는 트레이드오프를 저울질했을 것.
