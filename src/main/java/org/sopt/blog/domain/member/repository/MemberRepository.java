package org.sopt.blog.domain.member.repository;

import org.sopt.blog.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdAndDeletedAtIsNull(Long memberId);

    List<Member> findAllByDeletedAtIsNullOrderByCreatedAtDesc();
}
