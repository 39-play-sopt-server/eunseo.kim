package org.sopt.blog.domain.member.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.sopt.blog.global.common.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(unique = true)
    private String email;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'ROLE_USER'")
    private MemberRole role;

    @Column(name = "deleted_at")   // nullable 기본값 (null = 활성 회원)
    private LocalDateTime deletedAt;

    public Member(String name, String email, String password) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = MemberRole.ROLE_USER;
    }

    public static Member createSocialMember(String name) {
        return new Member(name, null, null);
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    public void grantAdminRole() {
        this.role = MemberRole.ROLE_ADMIN;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
