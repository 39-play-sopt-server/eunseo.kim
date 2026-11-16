package org.sopt.blog.domain.member.entity;

import jakarta.persistence.*;
import org.sopt.blog.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "deleted_at")   // nullable 기본값 (null = 활성 회원)
    private LocalDateTime deletedAt;

    // JPA용 기본 생성자
    protected Member() {}

    public Member(String name, String email, String password) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
